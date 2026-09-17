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

package io.dapr.workflows.task.client;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.dapr.durabletask.implementation.protobuf.Orchestration;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.dapr.workflows.task.serialization.JacksonDataConverter;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrchestrationMetadataTest {

  private static final String INSTANCE_ID = "instance-1";
  private static final String NAME = "DemoWorkflow";
  private static final Instant CREATED_AT = Instant.parse("2025-01-01T10:00:00Z");
  private static final Instant UPDATED_AT = Instant.parse("2025-01-01T10:05:00Z");

  private static Orchestration.WorkflowState.Builder state() {
    return Orchestration.WorkflowState.newBuilder()
        .setInstanceId(INSTANCE_ID)
        .setName(NAME)
        .setWorkflowStatus(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_RUNNING)
        .setCreatedTimestamp(timestamp(CREATED_AT))
        .setLastUpdatedTimestamp(timestamp(UPDATED_AT));
  }

  private static Timestamp timestamp(Instant instant) {
    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }

  private static OrchestrationMetadata metadata(Orchestration.WorkflowState.Builder state,
                                                boolean requestedInputsAndOutputs) {
    return new OrchestrationMetadata(state.build(), new JacksonDataConverter(), requestedInputsAndOutputs);
  }

  @Test
  public void shouldExposeTheStateItWasBuiltFrom() {
    OrchestrationMetadata metadata = metadata(state(), true);

    assertEquals(NAME, metadata.getName());
    assertEquals(INSTANCE_ID, metadata.getInstanceId());
    assertEquals(WorkflowRuntimeStatus.RUNNING, metadata.getRuntimeStatus());
    assertEquals(CREATED_AT, metadata.getCreatedAt());
    assertEquals(UPDATED_AT, metadata.getLastUpdatedAt());
    assertTrue(metadata.isInstanceFound());
  }

  @Test
  public void shouldBuildFromAGetInstanceResponse() {
    OrchestratorService.GetInstanceResponse response = OrchestratorService.GetInstanceResponse.newBuilder()
        .setExists(true)
        .setWorkflowState(state())
        .build();

    OrchestrationMetadata metadata =
        new OrchestrationMetadata(response, new JacksonDataConverter(), true);

    assertEquals(INSTANCE_ID, metadata.getInstanceId());
    assertEquals(NAME, metadata.getName());
  }

  @Test
  public void shouldReportNotFoundWhenNeitherNameNorIdIsSet() {
    OrchestrationMetadata metadata = metadata(
        Orchestration.WorkflowState.newBuilder()
            .setWorkflowStatus(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_RUNNING),
        true);

    assertFalse(metadata.isInstanceFound());
    assertFalse(metadata.isRunning(), "an instance that was not found is not running");
  }

  @Test
  public void shouldBeRunningOnlyWhenFoundAndInTheRunningStatus() {
    assertTrue(metadata(state(), true).isRunning());
    assertFalse(metadata(state()
        .setWorkflowStatus(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_PENDING), true).isRunning());
  }

  @Test
  public void shouldTreatTheThreeTerminalStatusesAsCompleted() {
    assertTrue(metadata(state()
        .setWorkflowStatus(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_COMPLETED), true).isCompleted());
    assertTrue(metadata(state()
        .setWorkflowStatus(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_FAILED), true).isCompleted());
    assertTrue(metadata(state()
        .setWorkflowStatus(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_TERMINATED), true).isCompleted());

    assertFalse(metadata(state(), true).isCompleted(), "running is not a terminal status");
    assertFalse(metadata(state()
        .setWorkflowStatus(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_SUSPENDED), true).isCompleted());
  }

  @Test
  public void shouldDeserializeInputOutputAndCustomStatus() {
    OrchestrationMetadata metadata = metadata(state()
        .setInput(StringValue.of("\"the input\""))
        .setOutput(StringValue.of("42"))
        .setCustomStatus(StringValue.of("\"halfway\"")), true);

    assertEquals("the input", metadata.readInputAs(String.class));
    assertEquals(42, metadata.readOutputAs(Integer.class));
    assertEquals("halfway", metadata.readCustomStatusAs(String.class));
    assertTrue(metadata.isCustomStatusFetched());
  }

  @Test
  public void shouldReturnNullForPayloadsTheSidecarLeftEmpty() {
    OrchestrationMetadata metadata = metadata(state(), true);

    assertNull(metadata.readInputAs(String.class));
    assertNull(metadata.readOutputAs(String.class));
    assertNull(metadata.readCustomStatusAs(String.class));
    assertFalse(metadata.isCustomStatusFetched());
  }

  @Test
  public void shouldRefuseToReadPayloadsWhenTheyWereNotFetched() {
    OrchestrationMetadata metadata = metadata(state().setInput(StringValue.of("\"the input\"")), false);

    assertThrows(IllegalStateException.class, () -> metadata.readInputAs(String.class));
    assertThrows(IllegalStateException.class, () -> metadata.readOutputAs(String.class));
    assertThrows(IllegalStateException.class, () -> metadata.readCustomStatusAs(String.class));
  }

  @Test
  public void shouldDescribeItselfWithoutPayloadsWhenThereAreNone() {
    String description = metadata(state(), true).toString();

    assertEquals("[Name: 'DemoWorkflow', ID: 'instance-1', RuntimeStatus: RUNNING, "
        + "CreatedAt: 2025-01-01T10:00:00Z, LastUpdatedAt: 2025-01-01T10:05:00Z, Input: '', Output: '']",
        description);
  }

  @Test
  public void shouldTrimLongPayloadsInItsDescription() {
    String longPayload = "\"" + "x".repeat(80) + "\"";

    String description = metadata(state()
        .setInput(StringValue.of(longPayload))
        .setOutput(StringValue.of(longPayload)), true).toString();

    assertTrue(description.contains("Input: '" + longPayload.substring(0, 50) + "...'"),
        "a payload longer than 50 characters is trimmed: " + description);
    assertTrue(description.contains("Output: '" + longPayload.substring(0, 50) + "...'"), description);
  }
}
