/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.workflows.client;

import io.dapr.durabletask.FailureDetails;
import io.dapr.durabletask.OrchestrationMetadata;
import io.dapr.durabletask.OrchestrationRuntimeStatus;
import io.dapr.workflows.runtime.DefaultWorkflowState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorkflowStateTest {

  private OrchestrationMetadata mockOrchestrationMetadata;
  private WorkflowState workflowMetadata;

  @BeforeEach
  public void setUp() {
    mockOrchestrationMetadata = mock(OrchestrationMetadata.class);
    workflowMetadata = new DefaultWorkflowState(mockOrchestrationMetadata);
  }

  @Test
  public void getInstanceId() {
    String expected = "instanceId";

    when(mockOrchestrationMetadata.getInstanceId()).thenReturn(expected);

    String result = workflowMetadata.getWorkflowId();

    verify(mockOrchestrationMetadata, times(1)).getInstanceId();
    assertEquals(expected, result);
  }

  @Test
  public void getName() {
    String expected = "WorkflowName";

    when(mockOrchestrationMetadata.getName()).thenReturn(expected);

    String result = workflowMetadata.getName();

    verify(mockOrchestrationMetadata, times(1)).getName();
    assertEquals(expected, result);
  }

  @Test
  public void getCreatedAt() {
    Instant expected = Instant.now();
    when(mockOrchestrationMetadata.getCreatedAt()).thenReturn(expected);

    Instant result = workflowMetadata.getCreatedAt();

    verify(mockOrchestrationMetadata, times(1)).getCreatedAt();
    assertEquals(expected, result);
  }

  @Test
  public void getLastUpdatedAt() {
    Instant expected = Instant.now();

    when(mockOrchestrationMetadata.getLastUpdatedAt()).thenReturn(expected);

    Instant result = workflowMetadata.getLastUpdatedAt();

    verify(mockOrchestrationMetadata, times(1)).getLastUpdatedAt();
    assertEquals(expected, result);
  }

  @Test
  public void getFailureDetails() {
    FailureDetails mockFailureDetails = mock(FailureDetails.class);

    when(mockFailureDetails.getErrorType()).thenReturn("errorType");
    when(mockFailureDetails.getErrorMessage()).thenReturn("errorMessage");
    when(mockFailureDetails.getStackTrace()).thenReturn("stackTrace");

    OrchestrationMetadata orchestrationMetadata = mock(OrchestrationMetadata.class);
    when(orchestrationMetadata.getFailureDetails()).thenReturn(mockFailureDetails);

    WorkflowState metadata = new DefaultWorkflowState(orchestrationMetadata);
    WorkflowFailureDetails result = metadata.getFailureDetails();

    verify(orchestrationMetadata, times(1)).getFailureDetails();
    assertEquals(mockFailureDetails.getErrorType(), result.getErrorType());
    assertEquals(mockFailureDetails.getErrorMessage(), result.getErrorMessage());
    assertEquals(mockFailureDetails.getStackTrace(), result.getStackTrace());
  }

  @Test
  public void getRuntimeStatus() {
    WorkflowRuntimeStatus expected = WorkflowRuntimeStatus.RUNNING;

    when(mockOrchestrationMetadata.getRuntimeStatus()).thenReturn(OrchestrationRuntimeStatus.RUNNING);

    WorkflowRuntimeStatus result = workflowMetadata.getRuntimeStatus();

    verify(mockOrchestrationMetadata, times(1)).getRuntimeStatus();
    assertEquals(expected, result);
  }

  @Test
  public void isRunning() {
    boolean expected = true;

    when(mockOrchestrationMetadata.isRunning()).thenReturn(expected);

    boolean result = workflowMetadata.isRunning();

    verify(mockOrchestrationMetadata, times(1)).isRunning();
    assertEquals(expected, result);
  }

  @Test
  public void isCompleted() {
    boolean expected = true;

    when(mockOrchestrationMetadata.isCompleted()).thenReturn(expected);

    boolean result = workflowMetadata.isCompleted();

    verify(mockOrchestrationMetadata, times(1)).isCompleted();
    assertEquals(expected, result);
  }

  @Test
  public void getSerializedInput() {
    String expected = "{input: \"test\"}";

    when(mockOrchestrationMetadata.getSerializedInput()).thenReturn(expected);

    String result = workflowMetadata.getSerializedInput();

    verify(mockOrchestrationMetadata, times(1)).getSerializedInput();
    assertEquals(expected, result);
  }

  @Test
  public void getSerializedOutput() {
    String expected = "{output: \"test\"}";

    when(mockOrchestrationMetadata.getSerializedOutput()).thenReturn(expected);

    String result = workflowMetadata.getSerializedOutput();

    verify(mockOrchestrationMetadata, times(1)).getSerializedOutput();
    assertEquals(expected, result);
  }

  @Test
  public void readInputAs() {
    String expected = "[{property: \"test input\"}}]";

    when(mockOrchestrationMetadata.readInputAs(String.class)).thenReturn(expected);

    String result = workflowMetadata.readInputAs(String.class);

    verify(mockOrchestrationMetadata, times(1)).readInputAs(String.class);
    assertEquals(expected, result);
  }

  @Test
  public void readOutputAs() {
    String expected = "[{property: \"test output\"}}]";

    when(mockOrchestrationMetadata.readOutputAs(String.class)).thenReturn(expected);

    String result = workflowMetadata.readOutputAs(String.class);

    verify(mockOrchestrationMetadata, times(1)).readOutputAs(String.class);
    assertEquals(expected, result);
  }

  @Test
  public void testToString() {
    String expected = "string value";

    when(mockOrchestrationMetadata.toString()).thenReturn(expected);

    String result = workflowMetadata.toString();

    assertEquals(expected, result);
  }

  @Test
  public void testWithNoMetadata() {
    String message = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      DefaultWorkflowState workflowState = new DefaultWorkflowState(null);
    }).getMessage();

    Assertions.assertTrue(message.contains("OrchestrationMetadata cannot be null"));
  }
}
