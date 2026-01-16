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

package io.dapr.durabletask.runner;

import com.google.protobuf.StringValue;
import io.dapr.durabletask.FailureDetails;
import io.dapr.durabletask.TaskActivityExecutor;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActivityRunner extends DurableRunner {
  private static final Logger logger = Logger.getLogger(ActivityRunner.class.getPackage().getName());

  private final OrchestratorService.ActivityRequest activityRequest;
  private final TaskActivityExecutor taskActivityExecutor;

  /**
   * Constructor.
   *
   * <p> This class executes the activity requests</p>
   *
   * @param workItem             work item to be executed
   * @param taskActivityExecutor executor for the activity
   * @param sidecarClient        sidecar client to communicate with the sidecar
   * @param tracer               tracer to be used for tracing
   */
  public ActivityRunner(
      OrchestratorService.WorkItem workItem,
      TaskActivityExecutor taskActivityExecutor,
      TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub sidecarClient,
      @Nullable Tracer tracer) {
    super(workItem, sidecarClient, tracer);
    this.activityRequest = workItem.getActivityRequest();
    this.taskActivityExecutor = taskActivityExecutor;
  }

  @Override
  public void run() {
    if (tracer != null) {
      runWithTracing();
    } else {
      runWithoutTracing();
    }
  }

  private void runWithTracing() {
    Context parentContext = extractTraceContext();

    Span span = tracer.spanBuilder("activity:" + activityRequest.getName())
        .setParent(parentContext)
        .setSpanKind(SpanKind.INTERNAL)
        .setAttribute("durabletask.task.instance_id",
            activityRequest.getOrchestrationInstance().getInstanceId())
        .setAttribute("durabletask.task.id", activityRequest.getTaskId())
        .setAttribute("durabletask.activity.name", activityRequest.getName())
        .startSpan();

    try (Scope scope = span.makeCurrent()) {
      executeActivity();
    } catch (Throwable e) {
      logger.log(Level.WARNING, "Failed to complete activity task.", e);
      span.setStatus(StatusCode.ERROR, "Failed to complete activity task");
      span.recordException(e);
    } finally {
      span.end();
    }
  }

  private void runWithoutTracing() {
    try {
      executeActivity();
    } catch (Throwable e) {
      logger.log(Level.WARNING, "Failed to complete activity task.", e);
    }
  }

  private void executeActivity() throws Throwable {
    String output = null;
    OrchestratorService.TaskFailureDetails failureDetails = null;
    Throwable failureException = null;
    try {
      output = taskActivityExecutor.execute(
          activityRequest.getName(),
          activityRequest.getInput().getValue(),
          activityRequest.getTaskExecutionId(),
          activityRequest.getTaskId(),
          activityRequest.getParentTraceContext().getTraceParent());
    } catch (Throwable e) {
      failureDetails = OrchestratorService.TaskFailureDetails.newBuilder()
          .setErrorType(e.getClass().getName())
          .setErrorMessage(e.getMessage())
          .setStackTrace(StringValue.of(FailureDetails.getFullStackTrace(e)))
          .build();
      failureException = e;
    }

    OrchestratorService.ActivityResponse.Builder responseBuilder = OrchestratorService.ActivityResponse
        .newBuilder()
        .setInstanceId(activityRequest.getOrchestrationInstance().getInstanceId())
        .setTaskId(activityRequest.getTaskId())
        .setCompletionToken(workItem.getCompletionToken());

    if (output != null) {
      responseBuilder.setResult(StringValue.of(output));
    }

    if (failureDetails != null) {
      responseBuilder.setFailureDetails(failureDetails);
    }

    try {
      this.sidecarClient.completeActivityTask(responseBuilder.build());
    } catch (StatusRuntimeException e) {
      logException(e);
      throw e;
    }

    if (failureException != null) {
      throw failureException;
    }
  }

  private Context extractTraceContext() {
    if (!activityRequest.hasParentTraceContext()) {
      return Context.current();
    }

    OrchestratorService.TraceContext traceContext = activityRequest.getParentTraceContext();
    String traceParent = traceContext.getTraceParent();

    if (traceParent.isEmpty()) {
      return Context.current();
    }

    Map<String, String> carrier = new HashMap<>();
    carrier.put("traceparent", traceParent);
    if (traceContext.hasTraceState()) {
      carrier.put("tracestate", traceContext.getTraceState().getValue());
    }

    TextMapGetter<Map<String, String>> getter = new TextMapGetter<>() {
      @Override
      public Iterable<String> keys(Map<String, String> carrier) {
        return carrier.keySet();
      }

      @Override
      public String get(Map<String, String> carrier, String key) {
        return carrier.get(key);
      }
    };

    return W3CTraceContextPropagator.getInstance()
        .extract(Context.current(), carrier, getter);
  }
}
