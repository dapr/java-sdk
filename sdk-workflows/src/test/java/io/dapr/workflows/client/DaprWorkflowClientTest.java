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

import io.dapr.durabletask.DurableTaskClient;
import io.dapr.durabletask.NewOrchestrationInstanceOptions;
import io.dapr.durabletask.OrchestrationMetadata;
import io.dapr.durabletask.OrchestrationRuntimeStatus;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowContext;
import io.dapr.workflows.WorkflowStub;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DaprWorkflowClientTest {

  private static Constructor<DaprWorkflowClient> constructor;
  private DaprWorkflowClient client;
  private DurableTaskClient mockInnerClient;
  private ManagedChannel mockGrpcChannel;

  public static class TestWorkflow implements Workflow {
    @Override
    public WorkflowStub create() {
      return WorkflowContext::getInstanceId;
    }
  }

  @BeforeAll
  public static void beforeAll() {
    constructor =
        Constructor.class.cast(Arrays.stream(DaprWorkflowClient.class.getDeclaredConstructors())
            .filter(c -> c.getParameters().length == 2).map(c -> {
              c.setAccessible(true);
              return c;
            }).findFirst().get());
  }

  @BeforeEach
  public void setUp() throws Exception {
    mockInnerClient = mock(DurableTaskClient.class);
    mockGrpcChannel = mock(ManagedChannel.class);

    when(mockGrpcChannel.shutdown()).thenReturn(mockGrpcChannel);

    client = constructor.newInstance(mockInnerClient, mockGrpcChannel);
  }

  @Test
  public void EmptyConstructor() {
    assertDoesNotThrow(() -> new DaprWorkflowClient());
  }

  @Test
  public void scheduleNewWorkflowWithArgName() {
    String expectedName = TestWorkflow.class.getCanonicalName();

    client.scheduleNewWorkflow(TestWorkflow.class);

    verify(mockInnerClient, times(1)).scheduleNewOrchestrationInstance(expectedName);
  }

  @Test
  public void scheduleNewWorkflowWithArgsNameInput() {
    String expectedName = TestWorkflow.class.getCanonicalName();
    Object expectedInput = new Object();

    client.scheduleNewWorkflow(TestWorkflow.class, expectedInput);

    verify(mockInnerClient, times(1))
        .scheduleNewOrchestrationInstance(expectedName, expectedInput);
  }

  @Test
  public void scheduleNewWorkflowWithArgsNameInputInstance() {
    String expectedName = TestWorkflow.class.getCanonicalName();
    Object expectedInput = new Object();
    String expectedInstanceId = "myTestInstance123";

    client.scheduleNewWorkflow(TestWorkflow.class, expectedInput, expectedInstanceId);

    verify(mockInnerClient, times(1))
        .scheduleNewOrchestrationInstance(expectedName, expectedInput, expectedInstanceId);
  }

  @Test
  public void scheduleNewWorkflowWithNewWorkflowOption() {
    String expectedName = TestWorkflow.class.getCanonicalName();
    Instant expectedStartTime = Instant.now();
    Object expectedInput = new Object();
    NewWorkflowOptions newWorkflowOptions = new NewWorkflowOptions();
    newWorkflowOptions.setInput(expectedInput).setStartTime(expectedStartTime);

    mockInnerClient.scheduleNewOrchestrationInstance(any(String.class), any(NewOrchestrationInstanceOptions.class));
    client.scheduleNewWorkflow(TestWorkflow.class, newWorkflowOptions);

    ArgumentCaptor<NewOrchestrationInstanceOptions> captor = ArgumentCaptor.forClass(
        NewOrchestrationInstanceOptions.class
    );

    verify(mockInnerClient, times(1))
        .scheduleNewOrchestrationInstance(eq(expectedName), captor.capture());

    assertEquals(expectedStartTime, captor.getValue().getStartTime());
    assertEquals(expectedInput, captor.getValue().getInput());
  }

  @Test
  public void terminateWorkflow() {
    String expectedArgument = "TestWorkflowInstanceId";

    client.terminateWorkflow(expectedArgument, null);
    verify(mockInnerClient, times(1)).terminate(expectedArgument, null);
  }

  @Test
  public void getInstanceMetadata() {

    // Arrange
    String instanceId = "TestWorkflowInstanceId";

    OrchestrationMetadata expectedMetadata = mock(OrchestrationMetadata.class);
    when(expectedMetadata.getInstanceId()).thenReturn(instanceId);
    when(expectedMetadata.getName()).thenReturn("WorkflowName");
    when(expectedMetadata.getRuntimeStatus()).thenReturn(OrchestrationRuntimeStatus.RUNNING);
    when(mockInnerClient.getInstanceMetadata(instanceId, true)).thenReturn(expectedMetadata);

    // Act
    WorkflowState metadata = client.getWorkflowState(instanceId, true);

    // Assert
    verify(mockInnerClient, times(1)).getInstanceMetadata(instanceId, true);
    assertNotEquals(metadata, null);
    assertEquals(metadata.getWorkflowId(), expectedMetadata.getInstanceId());
    assertEquals(metadata.getName(), expectedMetadata.getName());
    assertEquals(metadata.isRunning(), expectedMetadata.isRunning());
    assertEquals(metadata.isCompleted(), expectedMetadata.isCompleted());
  }

  @Test
  public void waitForInstanceStart() throws TimeoutException {

    // Arrange
    String instanceId = "TestWorkflowInstanceId";
    Duration timeout = Duration.ofSeconds(10);

    OrchestrationMetadata expectedMetadata = mock(OrchestrationMetadata.class);
    when(expectedMetadata.getInstanceId()).thenReturn(instanceId);
    when(mockInnerClient.waitForInstanceStart(instanceId, timeout, true)).thenReturn(expectedMetadata);

    // Act
    WorkflowState result = client.waitForWorkflowStart(instanceId, timeout, true);

    // Assert
    verify(mockInnerClient, times(1)).waitForInstanceStart(instanceId, timeout, true);
    assertNotEquals(result, null);
    assertEquals(result.getWorkflowId(), expectedMetadata.getInstanceId());
  }

  @Test
  public void waitForInstanceCompletion() throws TimeoutException {

    // Arrange
    String instanceId = "TestWorkflowInstanceId";
    Duration timeout = Duration.ofSeconds(10);

    OrchestrationMetadata expectedMetadata = mock(OrchestrationMetadata.class);
    when(expectedMetadata.getInstanceId()).thenReturn(instanceId);
    when(mockInnerClient.waitForInstanceCompletion(instanceId, timeout, true)).thenReturn(expectedMetadata);

    // Act
    WorkflowState result = client.waitForWorkflowCompletion(instanceId, timeout, true);

    // Assert
    verify(mockInnerClient, times(1)).waitForInstanceCompletion(instanceId, timeout, true);
    assertNotEquals(result, null);
    assertEquals(result.getWorkflowId(), expectedMetadata.getInstanceId());
  }

  @Test
  public void raiseEvent() {
    String expectedInstanceId = "TestWorkflowInstanceId";
    String expectedEventName = "TestEventName";
    Object expectedEventPayload = new Object();
    client.raiseEvent(expectedInstanceId, expectedEventName, expectedEventPayload);
    verify(mockInnerClient, times(1)).raiseEvent(expectedInstanceId,
        expectedEventName, expectedEventPayload);
  }

  @Test
  public void suspendResumeInstance() {
    String expectedArgument = "TestWorkflowInstanceId";
    client.suspendWorkflow(expectedArgument, "suspending workflow instance");
    client.resumeWorkflow(expectedArgument, "resuming workflow instance");
    verify(mockInnerClient, times(1)).suspendInstance(expectedArgument,
            "suspending workflow instance");
    verify(mockInnerClient, times(1)).resumeInstance(expectedArgument,
            "resuming workflow instance");
  }

  @Test
  public void purgeInstance() {
    String expectedArgument = "TestWorkflowInstanceId";
    client.purgeWorkflow(expectedArgument);
    verify(mockInnerClient, times(1)).purgeInstance(expectedArgument);
  }

  @Test
  public void close() throws InterruptedException {
    client.close();
    verify(mockInnerClient, times(1)).close();
    verify(mockGrpcChannel, times(1)).shutdown();
  }
}
