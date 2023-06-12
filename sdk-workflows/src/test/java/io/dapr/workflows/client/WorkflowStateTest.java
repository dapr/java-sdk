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

import com.microsoft.durabletask.FailureDetails;
import com.microsoft.durabletask.OrchestrationMetadata;
import com.microsoft.durabletask.OrchestrationRuntimeStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class WorkflowStateTest {

  private OrchestrationMetadata mockOrchestrationMetadata;
  private WorkflowState workflowMetadata;

  @Before
  public void setUp() throws Exception {
    mockOrchestrationMetadata = mock(OrchestrationMetadata.class);
    workflowMetadata = new WorkflowState(mockOrchestrationMetadata);
  }

  @Test
  public void getInstanceId() {
    // Arrange
    String expected = "instanceId";
    when(mockOrchestrationMetadata.getInstanceId()).thenReturn(expected);

    // Act
    String result = workflowMetadata.getInstanceId();

    // Assert
    verify(mockOrchestrationMetadata, times(1)).getInstanceId();
    Assert.assertEquals(result, expected);
  }

  @Test
  public void getName() {
    // Arrange
    String expected = "WorkflowName";
    when(mockOrchestrationMetadata.getName()).thenReturn(expected);

    // Act
    String result = workflowMetadata.getName();

    // Assert
    verify(mockOrchestrationMetadata, times(1)).getName();
    Assert.assertEquals(result, expected);
  }

  @Test
  public void getCreatedAt() {
    // Arrange
    Instant expected = Instant.now();
    when(mockOrchestrationMetadata.getCreatedAt()).thenReturn(expected);

    // Act
    Instant result = workflowMetadata.getCreatedAt();

    // Assert
    verify(mockOrchestrationMetadata, times(1)).getCreatedAt();
    Assert.assertEquals(result, expected);
  }

  @Test
  public void getLastUpdatedAt() {
    // Arrange
    Instant expected = Instant.now();
    when(mockOrchestrationMetadata.getLastUpdatedAt()).thenReturn(expected);

    // Act
    Instant result = workflowMetadata.getLastUpdatedAt();

    // Assert
    verify(mockOrchestrationMetadata, times(1)).getLastUpdatedAt();
    Assert.assertEquals(result, expected);
  }

  @Test
  public void getFailureDetails() {
    // Arrange
    FailureDetails mockFailureDetails = mock(FailureDetails.class);
    when(mockFailureDetails.getErrorType()).thenReturn("errorType");
    when(mockFailureDetails.getErrorMessage()).thenReturn("errorMessage");
    when(mockFailureDetails.getStackTrace()).thenReturn("stackTrace");

    OrchestrationMetadata orchestrationMetadata = mock(OrchestrationMetadata.class);
    when(orchestrationMetadata.getFailureDetails()).thenReturn(mockFailureDetails);
 
    // Act
    WorkflowState metadata = new WorkflowState(orchestrationMetadata);
    WorkflowFailureDetails result = metadata.getFailureDetails();

    // Assert
    verify(orchestrationMetadata, times(1)).getFailureDetails();
    Assert.assertEquals(result.getErrorType(), mockFailureDetails.getErrorType());
    Assert.assertEquals(result.getErrorMessage(), mockFailureDetails.getErrorMessage());
    Assert.assertEquals(result.getStackTrace(), mockFailureDetails.getStackTrace());
  }

  @Test
  public void getRuntimeStatus() {
    // Arrange
    WorkflowRuntimeStatus expected = WorkflowRuntimeStatus.RUNNING;
    when(mockOrchestrationMetadata.getRuntimeStatus()).thenReturn(OrchestrationRuntimeStatus.RUNNING);

    // Act
    WorkflowRuntimeStatus result = workflowMetadata.getRuntimeStatus();

    // Assert
    verify(mockOrchestrationMetadata, times(1)).getRuntimeStatus();
    Assert.assertEquals(result, expected);
  }

  @Test
  public void isRunning() {
    // Arrange
    boolean expected = true;
    when(mockOrchestrationMetadata.isRunning()).thenReturn(expected);

    // Act
    boolean result = workflowMetadata.isRunning();

    // Assert
    verify(mockOrchestrationMetadata, times(1)).isRunning();
    Assert.assertEquals(result, expected);
  }

  @Test
  public void isCompleted() {
    // Arrange
    boolean expected = true;
    when(mockOrchestrationMetadata.isCompleted()).thenReturn(expected);

    // Act
    boolean result = workflowMetadata.isCompleted();

    // Assert
    verify(mockOrchestrationMetadata, times(1)).isCompleted();
    Assert.assertEquals(result, expected);
  }

  @Test
  public void getSerializedInput() {
    // Arrange
    String expected = "{input: \"test\"}";
    when(mockOrchestrationMetadata.getSerializedInput()).thenReturn(expected);

    // Act
    String result = workflowMetadata.getSerializedInput();

    // Assert
    verify(mockOrchestrationMetadata, times(1)).getSerializedInput();
    Assert.assertEquals(result, expected);
  }

  @Test
  public void getSerializedOutput() {
    // Arrange
    String expected = "{output: \"test\"}";
    when(mockOrchestrationMetadata.getSerializedOutput()).thenReturn(expected);

    // Act
    String result = workflowMetadata.getSerializedOutput();

    // Assert
    verify(mockOrchestrationMetadata, times(1)).getSerializedOutput();
    Assert.assertEquals(result, expected);
  }

  @Test
  public void readInputAs() {
    // Arrange
    String expected = "[{property: \"test input\"}}]";
    when(mockOrchestrationMetadata.readInputAs(String.class)).thenReturn(expected);

    // Act
    String result = workflowMetadata.readInputAs(String.class);

    // Assert
    verify(mockOrchestrationMetadata, times(1)).readInputAs(String.class);
    Assert.assertEquals(result, expected);
  }

  @Test
  public void readOutputAs() {
    // Arrange
    String expected = "[{property: \"test output\"}}]";
    when(mockOrchestrationMetadata.readOutputAs(String.class)).thenReturn(expected);

    // Act
    String result = workflowMetadata.readOutputAs(String.class);

    // Assert
    verify(mockOrchestrationMetadata, times(1)).readOutputAs(String.class);
    Assert.assertEquals(result, expected);
  }

  @Test
  public void testToString() {
    // Arrange
    String expected = "string value";
    when(mockOrchestrationMetadata.toString()).thenReturn(expected);

    // Act
    String result = workflowMetadata.toString();

    // Assert
    //verify(mockOrchestrationMetadata, times(1)).toString();
    Assert.assertEquals(result, expected);
  }
}
