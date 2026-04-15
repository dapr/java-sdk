/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.spring.observation.client;

import io.dapr.config.Properties;
import io.dapr.internal.opencensus.GrpcHelper;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.NewWorkflowOptions;
import io.dapr.workflows.client.WorkflowState;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import reactor.util.context.Context;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * A {@link DaprWorkflowClient} subclass that creates Micrometer Observation spans (bridged to
 * OpenTelemetry) for each non-deprecated method call.
 *
 * <p>Because this class extends {@link DaprWorkflowClient}, consumers can keep injecting
 * {@code DaprWorkflowClient} without any code changes. Deprecated methods fall through to the
 * parent implementation without any observation.
 *
 * <p><b>Trace propagation:</b> an {@link OtelTracingClientInterceptor} is registered on the gRPC
 * channel. For each synchronous workflow RPC, the observation opens an OTel scope (via
 * {@link Observation#openScope()}) before calling {@code super.*}, making the observation span the
 * current OTel span in thread-local. The interceptor then reads {@link Span#current()} and injects
 * its W3C {@code traceparent} (and {@code grpc-trace-bin}) into the gRPC request headers so the
 * Dapr sidecar receives the full trace context.
 *
 * <p><b>Constructor note:</b> calling {@code super(properties, interceptor)} eagerly creates a gRPC
 * {@code ManagedChannel}, but the actual TCP connection is established lazily on the first RPC call,
 * so construction succeeds even when the Dapr sidecar is not yet available.
 */
public class ObservationDaprWorkflowClient extends DaprWorkflowClient {

  private static final OtelTracingClientInterceptor TRACING_INTERCEPTOR =
      new OtelTracingClientInterceptor();

  private final ObservationRegistry observationRegistry;

  /**
   * Creates a new {@code ObservationDaprWorkflowClient}.
   *
   * @param properties          connection properties for the underlying gRPC channel
   * @param observationRegistry the Micrometer {@link ObservationRegistry} used to create spans
   */
  public ObservationDaprWorkflowClient(Properties properties,
                                        ObservationRegistry observationRegistry) {
    super(properties, TRACING_INTERCEPTOR);
    this.observationRegistry = Objects.requireNonNull(observationRegistry,
        "observationRegistry must not be null");
  }

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  private Observation observation(String name) {
    return Observation.createNotStarted(name, observationRegistry);
  }

  // -------------------------------------------------------------------------
  // scheduleNewWorkflow — only String-based "leaf" overloads are overridden.
  // Class<T>-based overloads in the parent delegate to this.scheduleNewWorkflow(String, ...)
  // via dynamic dispatch, so they naturally pick up these observations.
  // -------------------------------------------------------------------------

  @Override
  public <T extends Workflow> String scheduleNewWorkflow(String name) {
    Observation obs = observation("dapr.workflow.schedule")
        .highCardinalityKeyValue("dapr.workflow.name", name)
        .start();
    try {
      try (Observation.Scope ignored = obs.openScope()) {
        return super.scheduleNewWorkflow(name);
      }
    } catch (RuntimeException e) {
      obs.error(e);
      throw e;
    } finally {
      obs.stop();
    }
  }

  @Override
  public <T extends Workflow> String scheduleNewWorkflow(String name, Object input) {
    Observation obs = observation("dapr.workflow.schedule")
        .highCardinalityKeyValue("dapr.workflow.name", name)
        .start();
    try {
      try (Observation.Scope ignored = obs.openScope()) {
        return super.scheduleNewWorkflow(name, input);
      }
    } catch (RuntimeException e) {
      obs.error(e);
      throw e;
    } finally {
      obs.stop();
    }
  }

  @Override
  public <T extends Workflow> String scheduleNewWorkflow(String name, Object input,
                                                          String instanceId) {
    Observation obs = observation("dapr.workflow.schedule")
        .highCardinalityKeyValue("dapr.workflow.name", name)
        .highCardinalityKeyValue("dapr.workflow.instance_id",
            instanceId != null ? instanceId : "")
        .start();
    try {
      try (Observation.Scope ignored = obs.openScope()) {
        return super.scheduleNewWorkflow(name, input, instanceId);
      }
    } catch (RuntimeException e) {
      obs.error(e);
      throw e;
    } finally {
      obs.stop();
    }
  }

  @Override
  public <T extends Workflow> String scheduleNewWorkflow(String name,
                                                          NewWorkflowOptions options) {
    String instanceId = options != null && options.getInstanceId() != null
        ? options.getInstanceId() : "";
    Observation obs = observation("dapr.workflow.schedule")
        .highCardinalityKeyValue("dapr.workflow.name", name)
        .highCardinalityKeyValue("dapr.workflow.instance_id", instanceId)
        .start();
    try {
      try (Observation.Scope ignored = obs.openScope()) {
        return super.scheduleNewWorkflow(name, options);
      }
    } catch (RuntimeException e) {
      obs.error(e);
      throw e;
    } finally {
      obs.stop();
    }
  }

  // -------------------------------------------------------------------------
  // Lifecycle operations
  // -------------------------------------------------------------------------

  @Override
  public void suspendWorkflow(String workflowInstanceId, @Nullable String reason) {
    Observation obs = observation("dapr.workflow.suspend")
        .highCardinalityKeyValue("dapr.workflow.instance_id", workflowInstanceId)
        .start();
    try {
      try (Observation.Scope ignored = obs.openScope()) {
        super.suspendWorkflow(workflowInstanceId, reason);
      }
    } catch (RuntimeException e) {
      obs.error(e);
      throw e;
    } finally {
      obs.stop();
    }
  }

  @Override
  public void resumeWorkflow(String workflowInstanceId, @Nullable String reason) {
    Observation obs = observation("dapr.workflow.resume")
        .highCardinalityKeyValue("dapr.workflow.instance_id", workflowInstanceId)
        .start();
    try {
      try (Observation.Scope ignored = obs.openScope()) {
        super.resumeWorkflow(workflowInstanceId, reason);
      }
    } catch (RuntimeException e) {
      obs.error(e);
      throw e;
    } finally {
      obs.stop();
    }
  }

  @Override
  public void terminateWorkflow(String workflowInstanceId, @Nullable Object output) {
    Observation obs = observation("dapr.workflow.terminate")
        .highCardinalityKeyValue("dapr.workflow.instance_id", workflowInstanceId)
        .start();
    try {
      try (Observation.Scope ignored = obs.openScope()) {
        super.terminateWorkflow(workflowInstanceId, output);
      }
    } catch (RuntimeException e) {
      obs.error(e);
      throw e;
    } finally {
      obs.stop();
    }
  }

  // -------------------------------------------------------------------------
  // State queries
  // -------------------------------------------------------------------------

  @Override
  @Nullable
  public WorkflowState getWorkflowState(String instanceId, boolean getInputsAndOutputs) {
    Observation obs = observation("dapr.workflow.get_state")
        .highCardinalityKeyValue("dapr.workflow.instance_id", instanceId)
        .start();
    try {
      try (Observation.Scope ignored = obs.openScope()) {
        return super.getWorkflowState(instanceId, getInputsAndOutputs);
      }
    } catch (RuntimeException e) {
      obs.error(e);
      throw e;
    } finally {
      obs.stop();
    }
  }

  // -------------------------------------------------------------------------
  // Waiting
  // -------------------------------------------------------------------------

  @Override
  @Nullable
  public WorkflowState waitForWorkflowStart(String instanceId, Duration timeout,
                                             boolean getInputsAndOutputs) throws TimeoutException {
    Observation obs = observation("dapr.workflow.wait_start")
        .highCardinalityKeyValue("dapr.workflow.instance_id", instanceId)
        .start();
    try {
      try (Observation.Scope ignored = obs.openScope()) {
        return super.waitForWorkflowStart(instanceId, timeout, getInputsAndOutputs);
      }
    } catch (TimeoutException e) {
      obs.error(e);
      throw e;
    } catch (RuntimeException e) {
      obs.error(e);
      throw e;
    } finally {
      obs.stop();
    }
  }

  @Override
  @Nullable
  public WorkflowState waitForWorkflowCompletion(String instanceId, Duration timeout,
                                                  boolean getInputsAndOutputs)
      throws TimeoutException {
    Observation obs = observation("dapr.workflow.wait_completion")
        .highCardinalityKeyValue("dapr.workflow.instance_id", instanceId)
        .start();
    try {
      try (Observation.Scope ignored = obs.openScope()) {
        return super.waitForWorkflowCompletion(instanceId, timeout, getInputsAndOutputs);
      }
    } catch (TimeoutException e) {
      obs.error(e);
      throw e;
    } catch (RuntimeException e) {
      obs.error(e);
      throw e;
    } finally {
      obs.stop();
    }
  }

  // -------------------------------------------------------------------------
  // Events
  // -------------------------------------------------------------------------

  @Override
  public void raiseEvent(String workflowInstanceId, String eventName, Object eventPayload) {
    Observation obs = observation("dapr.workflow.raise_event")
        .highCardinalityKeyValue("dapr.workflow.instance_id", workflowInstanceId)
        .highCardinalityKeyValue("dapr.workflow.event_name", eventName)
        .start();
    try {
      try (Observation.Scope ignored = obs.openScope()) {
        super.raiseEvent(workflowInstanceId, eventName, eventPayload);
      }
    } catch (RuntimeException e) {
      obs.error(e);
      throw e;
    } finally {
      obs.stop();
    }
  }

  // -------------------------------------------------------------------------
  // Cleanup
  // -------------------------------------------------------------------------

  @Override
  public boolean purgeWorkflow(String workflowInstanceId) {
    Observation obs = observation("dapr.workflow.purge")
        .highCardinalityKeyValue("dapr.workflow.instance_id", workflowInstanceId)
        .start();
    try {
      try (Observation.Scope ignored = obs.openScope()) {
        return super.purgeWorkflow(workflowInstanceId);
      }
    } catch (RuntimeException e) {
      obs.error(e);
      throw e;
    } finally {
      obs.stop();
    }
  }

  // Deprecated methods (getInstanceState, waitForInstanceStart, waitForInstanceCompletion,
  // purgeInstance) are intentionally not overridden — they fall through to the parent
  // implementation without any observation.

  // -------------------------------------------------------------------------
  // gRPC interceptor: injects the current OTel span's traceparent into headers
  // -------------------------------------------------------------------------

  /**
   * A gRPC {@link ClientInterceptor} that reads the current OTel span from the thread-local
   * context (set by {@link Observation#openScope()}) and injects its W3C {@code traceparent},
   * {@code tracestate}, and {@code grpc-trace-bin} headers into every outbound RPC call.
   *
   * <p>The interceptor is stateless: it reads {@link Span#current()} lazily at call time, so the
   * same instance can be shared across all calls on the channel.
   */
  private static final class OtelTracingClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method, CallOptions options, Channel channel) {
      return new ForwardingClientCall.SimpleForwardingClientCall<>(channel.newCall(method, options)) {
        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
          SpanContext spanCtx = Span.current().getSpanContext();
          if (spanCtx.isValid()) {
            // Build a Reactor Context with the OTel span's values and delegate to GrpcHelper,
            // which writes traceparent, tracestate AND grpc-trace-bin (the binary format that
            // older Dapr sidecar versions require for gRPC trace propagation).
            Context reactorCtx = Context.of("traceparent",
                TraceContextFormat.formatW3cTraceparent(spanCtx));
            String traceState = TraceContextFormat.formatTraceState(spanCtx);
            if (!traceState.isEmpty()) {
              reactorCtx = reactorCtx.put("tracestate", traceState);
            }
            GrpcHelper.populateMetadata(reactorCtx, headers);
          }
          super.start(responseListener, headers);
        }
      };
    }
  }
}
