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
limitations under the License.
*/

package io.dapr.workflows.internal;

import com.google.protobuf.Empty;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GrpcChannelKeepaliveTest {

  private static final Duration TEST_INTERVAL = Duration.ofMillis(50);

  private final AtomicInteger helloCount = new AtomicInteger();
  private final AtomicBoolean failPings = new AtomicBoolean(false);

  private Server server;
  private ManagedChannel channel;

  @BeforeEach
  public void setUp() throws IOException {
    String serverName = InProcessServerBuilder.generateName();
    this.server = InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void hello(Empty request, StreamObserver<Empty> responseObserver) {
            helloCount.incrementAndGet();
            if (failPings.get()) {
              responseObserver.onError(Status.UNAVAILABLE.asRuntimeException());
              return;
            }
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
          }
        })
        .build()
        .start();
    this.channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
  }

  @AfterEach
  public void tearDown() {
    this.channel.shutdownNow();
    this.server.shutdownNow();
  }

  @Test
  public void noPingsBeforeStart() throws InterruptedException {
    try (GrpcChannelKeepalive keepalive = new GrpcChannelKeepalive(channel, "test-keepalive", TEST_INTERVAL)) {
      Thread.sleep(TEST_INTERVAL.toMillis() * 4);
      assertEquals(0, helloCount.get());
    }
  }

  @Test
  public void pingsPeriodically() throws InterruptedException {
    try (GrpcChannelKeepalive keepalive = new GrpcChannelKeepalive(channel, "test-keepalive", TEST_INTERVAL)) {
      keepalive.start();
      // A second start must not schedule a second ping loop.
      keepalive.start();
      awaitHelloCountAtLeast(2);
    }
  }

  @Test
  public void continuesPingingAfterFailures() throws InterruptedException {
    failPings.set(true);
    try (GrpcChannelKeepalive keepalive = new GrpcChannelKeepalive(channel, "test-keepalive", TEST_INTERVAL)) {
      keepalive.start();
      awaitHelloCountAtLeast(2);
      failPings.set(false);
      awaitHelloCountAtLeast(helloCount.get() + 2);
    }
  }

  @Test
  public void closeStopsPinging() throws InterruptedException {
    GrpcChannelKeepalive keepalive = new GrpcChannelKeepalive(channel, "test-keepalive", TEST_INTERVAL);
    keepalive.start();
    awaitHelloCountAtLeast(1);
    keepalive.close();
    // Allow an already in-flight ping to finish before snapshotting.
    Thread.sleep(TEST_INTERVAL.toMillis() * 2);
    int countAfterClose = helloCount.get();
    Thread.sleep(TEST_INTERVAL.toMillis() * 4);
    assertEquals(countAfterClose, helloCount.get());
  }

  @Test
  public void keepaliveThreadIsDaemonAndStopsOnClose() throws InterruptedException {
    String threadName = "test-keepalive-lifecycle";
    try (GrpcChannelKeepalive keepalive = new GrpcChannelKeepalive(channel, threadName, TEST_INTERVAL)) {
      keepalive.start();
      awaitHelloCountAtLeast(1);
      Thread thread = findThread(threadName);
      assertTrue(thread != null && thread.isDaemon(), "expected a live daemon keepalive thread");
    }
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
    while (findThread(threadName) != null && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
    assertTrue(findThread(threadName) == null, "keepalive thread should terminate after close()");
  }

  private void awaitHelloCountAtLeast(int expected) throws InterruptedException {
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
    while (helloCount.get() < expected && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
    assertTrue(helloCount.get() >= expected,
        "expected at least " + expected + " hello pings, got " + helloCount.get());
  }

  private static Thread findThread(String name) {
    return Thread.getAllStackTraces().keySet().stream()
        .filter(t -> t.getName().equals(name) && t.isAlive())
        .findFirst()
        .orElse(null);
  }
}
