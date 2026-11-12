/*
 * Copyright 2026 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.dapr.it.testcontainers.support;

import com.google.protobuf.Empty;
import io.dapr.grpc.GrpcHealthCheckService;
import io.dapr.testcontainers.DaprProtocol;
import io.dapr.v1.AppCallbackHealthCheckGrpc;
import io.dapr.v1.DaprAppCallbackProtos.HealthCheckResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrpcCallbackWaitStrategyTest {
  @Test
  void protocolsAndIpv6Address() {
    assertEquals(DaprProtocol.HTTP, new DaprTestEnvironment("http", getClass()).appProtocol());
    assertEquals(DaprProtocol.GRPC, new DaprTestEnvironment("grpc", getClass(), DaprProtocol.GRPC).appProtocol());
    assertThrows(IllegalArgumentException.class, () -> new DaprTestEnvironment("bad", getClass(), null));
    assertEquals("[::1]:8080", Endpoints.http("::1", 8080).getRawAuthority());
  }

  @Test
  void successfulHealthClosesProbeChannel() throws Exception {
    Server server = ServerBuilder.forPort(0).addService(new GrpcHealthCheckService()).build().start();
    ManagedChannel channel = channel(server);
    try {
      GrpcCallbackWaitStrategy.awaitCallback(channel, Duration.ofSeconds(5));
      assertTrue(channel.isTerminated());
    } finally {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void retriesUnavailableCallbackUntilItSucceeds() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    Server server = ServerBuilder.forPort(0).addService(
        new AppCallbackHealthCheckGrpc.AppCallbackHealthCheckImplBase() {
          @Override
          public void healthCheck(Empty request, StreamObserver<HealthCheckResponse> response) {
            if (attempts.incrementAndGet() == 1) {
              response.onError(Status.UNAVAILABLE.asRuntimeException());
            } else {
              response.onNext(HealthCheckResponse.getDefaultInstance());
              response.onCompleted();
            }
          }
        }).build().start();
    ManagedChannel channel = channel(server);
    try {
      GrpcCallbackWaitStrategy.awaitCallback(channel, Duration.ofSeconds(5));
      assertTrue(attempts.get() >= 2);
      assertTrue(channel.isTerminated());
    } finally {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void missingCallbackFailsAndClosesProbeChannel() throws Exception {
    Server server = ServerBuilder.forPort(0).build().start();
    ManagedChannel channel = channel(server);
    try {
      assertThrows(StatusRuntimeException.class,
          () -> GrpcCallbackWaitStrategy.awaitCallback(channel, Duration.ofSeconds(5)));
      assertTrue(channel.isTerminated());
    } finally {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void stalledCallbackTimesOutAndClosesProbeChannel() throws Exception {
    Server server = ServerBuilder.forPort(0).addService(
        new AppCallbackHealthCheckGrpc.AppCallbackHealthCheckImplBase() {
          @Override
          public void healthCheck(Empty request, StreamObserver<HealthCheckResponse> response) { }
        }).build().start();
    ManagedChannel channel = channel(server);
    try {
      long start = System.nanoTime();
      assertThrows(IllegalStateException.class,
          () -> GrpcCallbackWaitStrategy.awaitCallback(channel, Duration.ofMillis(300)));
      assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(5)) < 0);
      assertTrue(channel.isTerminated());
    } finally {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void interruptionIsPreservedAndChannelIsClosed() {
    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 1).usePlaintext().build();
    try {
      Thread.currentThread().interrupt();
      assertThrows(IllegalStateException.class,
          () -> GrpcCallbackWaitStrategy.awaitCallback(channel, Duration.ofSeconds(5)));
      assertTrue(Thread.currentThread().isInterrupted());
      assertTrue(channel.isTerminated());
    } finally {
      Thread.interrupted();
    }
  }

  private static ManagedChannel channel(Server server) {
    return ManagedChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext().build();
  }
}
