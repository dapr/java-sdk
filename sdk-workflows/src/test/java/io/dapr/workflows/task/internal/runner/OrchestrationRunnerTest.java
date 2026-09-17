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
import com.google.protobuf.Timestamp;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.Orchestration;
import io.dapr.durabletask.implementation.protobuf.OrchestratorActions;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.workflows.task.OrchestratorFunction;
import io.dapr.workflows.task.TaskOrchestration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrchestrationRunnerTest {

  private static final String INSTANCE_ID = "instance-1";
  private static final Instant TIMESTAMP = Instant.parse("2026-01-01T00:00:00Z");

  private static OrchestratorService.WorkflowRequest request(String input) {
    HistoryEvents.ExecutionStartedEvent.Builder started = HistoryEvents.ExecutionStartedEvent.newBuilder()
        .setName("DemoWorkflow")
        .setWorkflowInstance(Orchestration.WorkflowInstance.newBuilder().setInstanceId(INSTANCE_ID));

    if (input != null) {
      started.setInput(StringValue.of(input));
    }

    return OrchestratorService.WorkflowRequest.newBuilder()
        .setInstanceId(INSTANCE_ID)
        .addNewEvents(HistoryEvents.HistoryEvent.newBuilder()
            .setEventId(-1)
            .setTimestamp(Timestamp.newBuilder().setSeconds(TIMESTAMP.getEpochSecond()))
            .setExecutionStarted(started))
        .build();
  }

  private static OrchestratorService.WorkflowResponse parse(byte[] responseBytes) {
    try {
      return OrchestratorService.WorkflowResponse.parseFrom(responseBytes);
    } catch (Exception e) {
      throw new AssertionError("the runner returned bytes that are not a WorkflowResponse", e);
    }
  }

  @Test
  public void shouldRunAnOrchestrationAndReturnItsCompletionAction() {
    TaskOrchestration orchestration = ctx -> ctx.complete("done");

    byte[] responseBytes = OrchestrationRunner.loadAndRun(request(null).toByteArray(), orchestration);

    OrchestratorService.WorkflowResponse response = parse(responseBytes);
    assertEquals(INSTANCE_ID, response.getInstanceId());
    assertEquals(1, response.getActionsCount());

    OrchestratorActions.CompleteWorkflowAction completion = response.getActions(0).getCompleteWorkflow();
    assertEquals(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_COMPLETED, completion.getWorkflowStatus());
    assertEquals("\"done\"", completion.getResult().getValue());
  }

  @Test
  public void shouldPassTheRequestInputToTheOrchestratorFunction() {
    OrchestratorFunction<String> function = ctx -> ctx.getInput(String.class) + " world";

    byte[] responseBytes =
        OrchestrationRunner.loadAndRun(request("\"hello\"").toByteArray(), function);

    OrchestratorActions.CompleteWorkflowAction completion =
        parse(responseBytes).getActions(0).getCompleteWorkflow();
    assertEquals("\"hello world\"", completion.getResult().getValue());
  }

  @Test
  public void shouldReportAFailingOrchestrationAsFailed() {
    TaskOrchestration orchestration = ctx -> {
      throw new IllegalStateException("workflow blew up");
    };

    byte[] responseBytes = OrchestrationRunner.loadAndRun(request(null).toByteArray(), orchestration);

    OrchestratorActions.CompleteWorkflowAction completion =
        parse(responseBytes).getActions(0).getCompleteWorkflow();
    assertEquals(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_FAILED, completion.getWorkflowStatus());
    assertTrue(completion.getFailureDetails().getErrorMessage().contains("workflow blew up"),
        completion.getFailureDetails().getErrorMessage());
  }

  @Test
  public void shouldReturnTheCustomStatusTheOrchestrationSet() {
    TaskOrchestration orchestration = ctx -> {
      ctx.setCustomStatus("halfway");
      ctx.complete(null);
    };

    byte[] responseBytes = OrchestrationRunner.loadAndRun(request(null).toByteArray(), orchestration);

    assertEquals("\"halfway\"", parse(responseBytes).getCustomStatus().getValue());
  }

  @Test
  public void shouldAcceptAndReturnBase64WhenCalledWithStrings() {
    String encodedRequest = Base64.getEncoder().encodeToString(request(null).toByteArray());

    String encodedResponse =
        OrchestrationRunner.loadAndRun(encodedRequest, (TaskOrchestration) ctx -> ctx.complete("done"));

    OrchestratorService.WorkflowResponse response = parse(Base64.getDecoder().decode(encodedResponse));
    assertEquals(INSTANCE_ID, response.getInstanceId());
  }

  @Test
  public void shouldAcceptAndReturnBase64ForTheFunctionOverloadToo() {
    String encodedRequest = Base64.getEncoder().encodeToString(request("\"hello\"").toByteArray());

    String encodedResponse = OrchestrationRunner.loadAndRun(
        encodedRequest, (OrchestratorFunction<String>) ctx -> ctx.getInput(String.class));

    OrchestratorActions.CompleteWorkflowAction completion =
        parse(Base64.getDecoder().decode(encodedResponse)).getActions(0).getCompleteWorkflow();
    assertEquals("\"hello\"", completion.getResult().getValue());
  }

  @Test
  public void shouldRejectMissingArguments() {
    byte[] validRequest = request(null).toByteArray();

    assertThrows(IllegalArgumentException.class,
        () -> OrchestrationRunner.loadAndRun(validRequest, (TaskOrchestration) null));
    assertThrows(IllegalArgumentException.class,
        () -> OrchestrationRunner.loadAndRun(validRequest, (OrchestratorFunction<String>) null));
    assertThrows(IllegalArgumentException.class,
        () -> OrchestrationRunner.loadAndRun((byte[]) null, (TaskOrchestration) ctx -> ctx.complete(null)));
    assertThrows(IllegalArgumentException.class,
        () -> OrchestrationRunner.loadAndRun(new byte[0], (TaskOrchestration) ctx -> ctx.complete(null)));
  }

  @Test
  public void shouldRejectBytesThatAreNotAWorkflowRequest() {
    byte[] notProtobuf = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> OrchestrationRunner.loadAndRun(notProtobuf, (TaskOrchestration) ctx -> ctx.complete(null)));

    assertTrue(thrown.getMessage().contains("valid protobuf"), thrown.getMessage());
  }
}
