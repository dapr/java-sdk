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

package io.dapr.workflows.task.internal.runner;

import com.google.protobuf.StringValue;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.Orchestration;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.dapr.workflows.task.internal.TaskActivityExecutor;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ActivityRunnerTest {

  private static final String INSTANCE_ID = "instance-1";
  private static final String ACTIVITY_NAME = "DemoActivity";
  private static final String COMPLETION_TOKEN = "token-1";
  private static final int TASK_ID = 7;
  private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
  private static final String TRACE_PARENT = "00-" + TRACE_ID + "-b7ad6b7169203331-01";

  private TaskActivityExecutor executor;
  private TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub sidecarClient;

  /** The span the runner made current while the activity ran, captured from inside the activity. */
  private final AtomicReference<SpanContext> spanDuringActivity = new AtomicReference<>();

  @BeforeEach
  public void setUp() {
    executor = mock(TaskActivityExecutor.class);
    sidecarClient = mock(TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub.class);
    spanDuringActivity.set(null);

    Channel channel = mock(Channel.class);
    when(channel.authority()).thenReturn("localhost:4001");
    when(sidecarClient.getChannel()).thenReturn(channel);
  }

  private static Tracer tracer() {
    return SdkTracerProvider.builder().build().get("activity-runner-test");
  }

  private static OrchestratorService.WorkItem workItem(Orchestration.TraceContext traceContext) {
    OrchestratorService.ActivityRequest.Builder activityRequest = OrchestratorService.ActivityRequest.newBuilder()
        .setName(ACTIVITY_NAME)
        .setTaskId(TASK_ID)
        .setTaskExecutionId("execution-1")
        .setInput(StringValue.of("\"the input\""))
        .setWorkflowInstance(Orchestration.WorkflowInstance.newBuilder().setInstanceId(INSTANCE_ID));

    if (traceContext != null) {
      activityRequest.setParentTraceContext(traceContext);
    }

    return OrchestratorService.WorkItem.newBuilder()
        .setCompletionToken(COMPLETION_TOKEN)
        .setActivityRequest(activityRequest)
        .build();
  }

  private void activityReturns(String output) throws Throwable {
    when(executor.execute(any(), any(), any(), eq(TASK_ID), any(), isNull())).thenAnswer(invocation -> {
      spanDuringActivity.set(Span.current().getSpanContext());
      return output;
    });
  }

  private OrchestratorService.ActivityResponse capturedResponse() {
    ArgumentCaptor<OrchestratorService.ActivityResponse> captor =
        ArgumentCaptor.forClass(OrchestratorService.ActivityResponse.class);
    verify(sidecarClient).completeActivityTask(captor.capture());

    return captor.getValue();
  }

  @Test
  public void shouldReportTheActivityResultBackToTheSidecar() throws Throwable {
    when(executor.execute(eq(ACTIVITY_NAME), eq("\"the input\""), eq("execution-1"), eq(TASK_ID),
        any(), isNull())).thenReturn("\"the output\"");

    new ActivityRunner(workItem(null), executor, sidecarClient, null).run();

    OrchestratorService.ActivityResponse response = capturedResponse();
    assertEquals(INSTANCE_ID, response.getInstanceId());
    assertEquals(TASK_ID, response.getTaskId());
    assertEquals(COMPLETION_TOKEN, response.getCompletionToken());
    assertEquals("\"the output\"", response.getResult().getValue());
    assertFalse(response.hasFailureDetails());
  }

  @Test
  public void shouldSendNoResultWhenTheActivityReturnedNull() throws Throwable {
    activityReturns(null);

    new ActivityRunner(workItem(null), executor, sidecarClient, null).run();

    OrchestratorService.ActivityResponse response = capturedResponse();
    assertFalse(response.hasResult(), "a null activity result must not be sent as an empty string");
    assertFalse(response.hasFailureDetails());
  }

  @Test
  public void shouldReportAFailingActivityAsFailureDetails() throws Throwable {
    when(executor.execute(any(), any(), any(), eq(TASK_ID), any(), isNull()))
        .thenThrow(new IllegalStateException("activity blew up"));

    new ActivityRunner(workItem(null), executor, sidecarClient, null).run();

    OrchestratorService.ActivityResponse response = capturedResponse();
    assertTrue(response.hasFailureDetails());
    assertEquals("java.lang.IllegalStateException", response.getFailureDetails().getErrorType());
    assertEquals("activity blew up", response.getFailureDetails().getErrorMessage());
    assertTrue(response.getFailureDetails().getStackTrace().getValue().startsWith("\tat "),
        "the stack trace carries the frames, with the type reported separately as the error type");
    assertFalse(response.hasResult());
  }

  @Test
  public void shouldSwallowASidecarFailureSoTheWorkerThreadSurvives() throws Throwable {
    activityReturns("\"the output\"");
    when(sidecarClient.completeActivityTask(any())).thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

    ActivityRunner runner = new ActivityRunner(workItem(null), executor, sidecarClient, null);

    assertDoesNotThrow(runner::run, "a runner that threw would kill its worker thread");
    verify(sidecarClient).completeActivityTask(any());
  }

  @Test
  public void shouldSwallowASidecarFailureWhenTracingIsOnToo() throws Throwable {
    activityReturns("\"the output\"");
    when(sidecarClient.completeActivityTask(any())).thenThrow(new StatusRuntimeException(Status.CANCELLED));

    ActivityRunner runner = new ActivityRunner(workItem(null), executor, sidecarClient, tracer());

    assertDoesNotThrow(runner::run);
    verify(sidecarClient).completeActivityTask(any());
  }

  @Test
  public void shouldPassThePropagatedHistoryWhenTheRequestCarriesOne() throws Throwable {
    HistoryEvents.PropagatedHistory history = HistoryEvents.PropagatedHistory.newBuilder().build();
    OrchestratorService.WorkItem item = OrchestratorService.WorkItem.newBuilder()
        .setCompletionToken(COMPLETION_TOKEN)
        .setActivityRequest(OrchestratorService.ActivityRequest.newBuilder()
            .setName(ACTIVITY_NAME)
            .setTaskId(TASK_ID)
            .setWorkflowInstance(Orchestration.WorkflowInstance.newBuilder().setInstanceId(INSTANCE_ID))
            .setPropagatedHistory(history))
        .build();

    new ActivityRunner(item, executor, sidecarClient, null).run();

    verify(executor).execute(eq(ACTIVITY_NAME), any(), any(), eq(TASK_ID), any(), eq(history));
  }

  @Test
  public void shouldRunTheActivityInsideNoSpanWhenThereIsNoTracer() throws Throwable {
    activityReturns("\"the output\"");

    new ActivityRunner(workItem(null), executor, sidecarClient, null).run();

    assertFalse(spanDuringActivity.get().isValid(), "without a tracer the activity runs untraced");
  }

  @Test
  public void shouldRunTheActivityInsideASpanWhenATracerIsSupplied() throws Throwable {
    activityReturns("\"the output\"");

    new ActivityRunner(workItem(null), executor, sidecarClient, tracer()).run();

    assertTrue(spanDuringActivity.get().isValid(), "the activity must run inside the runner's span");
  }

  @Test
  public void shouldContinueTheTraceFromTheParentTraceContext() throws Throwable {
    Orchestration.TraceContext traceContext = Orchestration.TraceContext.newBuilder()
        .setTraceParent(TRACE_PARENT)
        .setTraceState(StringValue.of("vendor=value"))
        .build();

    activityReturns("\"the output\"");

    new ActivityRunner(workItem(traceContext), executor, sidecarClient, tracer()).run();

    assertEquals(TRACE_ID, spanDuringActivity.get().getTraceId(),
        "the span must join the trace the parent trace context names");
  }

  @Test
  public void shouldStartItsOwnTraceWhenTheParentTraceParentIsEmpty() throws Throwable {
    Orchestration.TraceContext traceContext = Orchestration.TraceContext.newBuilder()
        .setTraceParent("")
        .build();

    activityReturns("\"the output\"");

    new ActivityRunner(workItem(traceContext), executor, sidecarClient, tracer()).run();

    assertTrue(spanDuringActivity.get().isValid());
    assertNotEquals(TRACE_ID, spanDuringActivity.get().getTraceId(),
        "an empty traceparent gives nothing to continue from");
  }
}
