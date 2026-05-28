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

package io.dapr.durabletask;

import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies DurableTaskGrpcWorker recovers from an extended sidecar outage even when the
 * underlying gRPC channel's exponential backoff has grown past the worker's retry interval.
 *
 * <p>Without forcing a backoff reset on each retry, gRPC-Java's default reconnect policy
 * (1s initial, 1.6x multiplier, 120s max) lets the channel's internal backoff timer outrun
 * the worker's 5s retry loop. The worker keeps calling getWorkItems(), but the channel is
 * mid-backoff and fails fast with UNAVAILABLE. The bug surfaces in production under chaos
 * faults that kill daprd for ~30s — by the time daprd is back, the channel may be 30+s
 * into a 60s backoff and miss the recovery window.</p>
 *
 * <p>Uses a real TCP gRPC server (NettyServerBuilder) so the channel exercises its real
 * connect-and-backoff state machine. In-process channels do not have this behavior.</p>
 */
class DurableTaskGrpcWorkerChannelBackoffTest {

  // Long enough for the channel's exponential backoff to grow past the worker's 5s
  // retry interval (per default policy, ~6 failures get to ~16s backoff).
  private static final long EXTENDED_OUTAGE_MILLIS = 30_000L;

  // Once the second server is up, a fixed-fix worker should reconnect within one
  // retry cycle. Bug-version worker is stuck in a long channel backoff and won't.
  private static final long RECONNECT_DEADLINE_MILLIS = 10_000L;

  private DurableTaskGrpcWorker worker;
  private ManagedChannel channel;
  private Server server;

  @AfterEach
  void tearDown() throws Exception {
    if (worker != null) {
      worker.close();
    }
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (server != null && !server.isShutdown()) {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void workerReconnectsAfterExtendedSidecarOutage() throws Exception {
    int port = pickFreePort();

    // Phase 1: server v1 up; worker connects.
    CountDownLatch v1Connected = new CountDownLatch(1);
    server = startServer(port, request -> v1Connected.countDown());
    channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();

    worker = new DurableTaskGrpcWorkerBuilder().grpcChannel(channel).build();
    worker.start();

    assertTrue(v1Connected.await(10, TimeUnit.SECONDS), "worker did not establish initial stream to server v1");

    // Phase 2: kill the sidecar; let the channel exhaust several backoff cycles.
    server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    server = null;
    Thread.sleep(EXTENDED_OUTAGE_MILLIS);

    // Phase 3: sidecar back; worker should reconnect within one retry cycle.
    AtomicInteger v2CallCount = new AtomicInteger();
    CountDownLatch v2Connected = new CountDownLatch(1);
    server = startServer(port, request -> {
      v2CallCount.incrementAndGet();
      v2Connected.countDown();
    });

    boolean reconnected = v2Connected.await(RECONNECT_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
    assertTrue(reconnected,
        "worker failed to reconnect within " + RECONNECT_DEADLINE_MILLIS + "ms after sidecar restart"
            + " (channel state=" + channel.getState(false) + ", v2 calls=" + v2CallCount.get() + ")");
  }

  private static int pickFreePort() throws Exception {
    try (ServerSocket s = new ServerSocket(0)) {
      s.setReuseAddress(true);
      return s.getLocalPort();
    }
  }

  private static Server startServer(int port, java.util.function.Consumer<OrchestratorService.GetWorkItemsRequest> onCall)
      throws Exception {
    return NettyServerBuilder.forPort(port)
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void getWorkItems(
              OrchestratorService.GetWorkItemsRequest request,
              StreamObserver<OrchestratorService.WorkItem> responseObserver) {
            onCall.accept(request);
            // Hold the stream open; the worker only loops on stream termination.
          }
        })
        .build()
        .start();
  }
}
