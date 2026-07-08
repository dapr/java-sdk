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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that scheduling an orchestration propagates the caller's trace context
 * to the sidecar when a Tracer is configured, and stays untraced when not.
 */
class DurableTaskGrpcClientTracingTest {

  private static final String ORCHESTRATION_NAME = "TestOrchestration";

  private Server server;
  private ManagedChannel channel;
  private DurableTaskClient client;
  private final AtomicReference<OrchestratorService.CreateInstanceRequest> capturedRequest = new AtomicReference<>();
  private final AtomicReference<SpanContext> spanContextDuringCall = new AtomicReference<>();

  @BeforeEach
  void setUp() throws Exception {
    String serverName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void startInstance(
              OrchestratorService.CreateInstanceRequest request,
              StreamObserver<OrchestratorService.CreateInstanceResponse> responseObserver) {
            capturedRequest.set(request);
            // directExecutor() runs this handler on the client thread, so this observes
            // the span the client made current for the duration of the call.
            spanContextDuringCall.set(Span.current().getSpanContext());
            responseObserver.onNext(OrchestratorService.CreateInstanceResponse.newBuilder()
                .setInstanceId(request.getInstanceId())
                .build());
            responseObserver.onCompleted();
          }
        })
        .build()
        .start();
    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (client != null) {
      client.close();
    }
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (server != null) {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void scheduleWithoutTracerDoesNotSetParentTraceContext() {
    client = new DurableTaskGrpcClientBuilder()
        .grpcChannel(channel)
        .build();

    client.scheduleNewOrchestrationInstance(ORCHESTRATION_NAME);

    assertFalse(capturedRequest.get().hasParentTraceContext());
  }

  @Test
  void scheduleWithTracerPropagatesCallerTraceContext() {
    SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build();
    try {
      Tracer tracer = tracerProvider.get("test");
      client = new DurableTaskGrpcClientBuilder()
          .grpcChannel(channel)
          .tracer(tracer)
          .build();

      Span callerSpan = tracer.spanBuilder("caller").startSpan();
      try (Scope scope = callerSpan.makeCurrent()) {
        client.scheduleNewOrchestrationInstance(ORCHESTRATION_NAME);
      } finally {
        callerSpan.end();
      }

      OrchestratorService.CreateInstanceRequest request = capturedRequest.get();
      assertTrue(request.hasParentTraceContext());

      // traceparent format: 00-<trace-id>-<span-id>-<flags>
      String traceParent = request.getParentTraceContext().getTraceParent();
      String[] parts = traceParent.split("-");
      assertEquals(4, parts.length);
      // The scheduled orchestration must join the caller's trace, through a child
      // span distinct from the caller's own span.
      assertEquals(callerSpan.getSpanContext().getTraceId(), parts[1]);
      assertFalse(callerSpan.getSpanContext().getSpanId().equals(parts[2]));

      // The scheduling span must be current while the sidecar call runs, so nested
      // instrumentation attaches to it. It must match the propagated trace context.
      assertEquals(parts[1], spanContextDuringCall.get().getTraceId());
      assertEquals(parts[2], spanContextDuringCall.get().getSpanId());
    } finally {
      tracerProvider.close();
    }
  }

  @Test
  void scheduleWithNoOpTracerDoesNotSetParentTraceContext() {
    // A tracer that produces invalid span contexts (e.g. OpenTelemetry no-op)
    // must not attach an unusable trace context to the request.
    Tracer noopTracer = io.opentelemetry.api.OpenTelemetry.noop().getTracer("noop");
    client = new DurableTaskGrpcClientBuilder()
        .grpcChannel(channel)
        .tracer(noopTracer)
        .build();

    client.scheduleNewOrchestrationInstance(ORCHESTRATION_NAME);

    assertFalse(capturedRequest.get().hasParentTraceContext());
  }
}
