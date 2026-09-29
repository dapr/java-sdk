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
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the worker advertises WORKER_CAPABILITY_HEALTH_PING and discards HealthPing work items
 * without logging them or reconnecting.
 */
class DurableTaskGrpcWorkerHealthPingTest {

  private static final Logger WORKER_LOGGER = Logger.getLogger(DurableTaskGrpcWorker.class.getPackage().getName());
  private static final String UNKNOWN_WORK_ITEM_PREFIX = "Received and dropped an unknown";

  private final List<LogRecord> unknownWorkItemLogs = new CopyOnWriteArrayList<>();
  private final Handler logHandler = new Handler() {
    @Override
    public void publish(LogRecord logRecord) {
      if (logRecord.getMessage() != null && logRecord.getMessage().startsWith(UNKNOWN_WORK_ITEM_PREFIX)) {
        unknownWorkItemLogs.add(logRecord);
      }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
  };

  private DurableTaskGrpcWorker worker;
  private Server server;
  private ManagedChannel channel;

  @AfterEach
  void tearDown() throws Exception {
    WORKER_LOGGER.removeHandler(logHandler);
    if (worker != null) {
      worker.close();
    }
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (server != null) {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void advertisesHealthPingAndIgnoresPingsWithoutReconnecting() throws Exception {
    AtomicInteger streams = new AtomicInteger();
    AtomicReference<List<OrchestratorService.WorkerCapability>> captured = new AtomicReference<>();
    WORKER_LOGGER.addHandler(logHandler);

    String serverName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(serverName).directExecutor()
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void getWorkItems(OrchestratorService.GetWorkItemsRequest request,
              StreamObserver<OrchestratorService.WorkItem> responseObserver) {
            streams.incrementAndGet();
            captured.compareAndSet(null, request.getCapabilitiesList());
            OrchestratorService.WorkItem ping = OrchestratorService.WorkItem.newBuilder()
                .setHealthPing(OrchestratorService.HealthPing.getDefaultInstance())
                .build();
            for (int i = 0; i < 3; i++) {
              responseObserver.onNext(ping);
            }
            // The trailing unknown item follows the pings, so its warning means the pings were handled.
            responseObserver.onNext(OrchestratorService.WorkItem.getDefaultInstance());
            // Keep the stream open so the worker does not reconnect.
          }
        })
        .build()
        .start();
    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    worker = new DurableTaskGrpcWorkerBuilder().grpcChannel(channel).build();
    worker.start();

    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (unknownWorkItemLogs.isEmpty() && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }

    assertEquals(1, unknownWorkItemLogs.size(), "only the trailing unknown item may be logged, never a ping");
    assertEquals(OrchestratorService.WorkItem.RequestCase.REQUEST_NOT_SET,
        unknownWorkItemLogs.get(0).getParameters()[0]);
    assertEquals(1, streams.get(), "health pings must not make the worker reconnect");
    assertNotNull(captured.get());
    assertTrue(captured.get().contains(OrchestratorService.WorkerCapability.WORKER_CAPABILITY_HEALTH_PING),
        "the worker must advertise WORKER_CAPABILITY_HEALTH_PING");
  }
}
