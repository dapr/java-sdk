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

import com.microsoft.durabletask.DurableTaskClient;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.grpc.ManagedChannel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

public class DaprWorkflowClientTest {
  private static Constructor<DaprWorkflowClient> constructor;
  private DaprWorkflowClient client;
  private DurableTaskClient mockInnerClient;
  private ManagedChannel mockGrpcChannel;

  public class TestWorkflow extends Workflow {
    @Override
    public WorkflowStub create() {
      return ctx -> { };
    }
  }

  @BeforeClass
  public static void beforeAll() {
        constructor =
        Constructor.class.cast(Arrays.stream(DaprWorkflowClient.class.getDeclaredConstructors())
            .filter(c -> c.getParameters().length == 2).peek(c -> c.setAccessible(true)).findFirst().get());
  }

  @Before
  public void setUp() throws Exception {
    mockInnerClient = mock(DurableTaskClient.class);
    mockGrpcChannel = mock(ManagedChannel.class);
    when(mockGrpcChannel.shutdown()).thenReturn(mockGrpcChannel);

    client = constructor.newInstance(mockInnerClient, mockGrpcChannel);
  }

  @Test
  public void EmptyConstructor() {
    assertDoesNotThrow(DaprWorkflowClient::new);
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
  public void terminateWorkflow() {
    String expectedArgument = "TestWorkflowInstanceId";

    client.terminateWorkflow(expectedArgument, null);
    verify(mockInnerClient, times(1)).terminate(expectedArgument, null);
  }

  @Test
  public void close() throws InterruptedException {
    client.close();
    verify(mockInnerClient, times(1)).close();
    verify(mockGrpcChannel, times(1)).shutdown();
  }

  @Test
  public void closeWithInnerClientRuntimeException() throws InterruptedException {
    doThrow(RuntimeException.class).when(mockInnerClient).close();

    assertThrows(RuntimeException.class, () -> { client.close(); });
    verify(mockInnerClient, times(1)).close();
    verify(mockGrpcChannel, times(1)).shutdown();
  }
}
