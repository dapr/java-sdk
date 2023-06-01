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
import io.grpc.ManagedChannel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

public class DaprWorkflowClientTest {
  private static Constructor<DaprWorkflowClient> constructor;
  private DaprWorkflowClient client;
  private DurableTaskClient mockInnerClient;
  private ManagedChannel mockGrpcChannel;

  @BeforeClass
  public static void beforeAll() {
        constructor =
        Constructor.class.cast(Arrays.stream(DaprWorkflowClient.class.getDeclaredConstructors())
            .filter(c -> c.getParameters().length == 2).map(c -> {
              c.setAccessible(true);
              return c;
            }).findFirst().get());
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
    String expectedName = "TestWorkflow";

    client.scheduleNewWorkflow(expectedName);

    verify(mockInnerClient, times(1)).scheduleNewOrchestrationInstance(expectedName);
  }

  @Test
  public void scheduleNewWorkflowWithArgsNameInput() {
    String expectedName = "TestWorkflow";
    Object expectedInput = new Object();

    client.scheduleNewWorkflow(expectedName, expectedInput);

    verify(mockInnerClient, times(1))
        .scheduleNewOrchestrationInstance(expectedName, expectedInput);
  }

  @Test
  public void scheduleNewWorkflowWithArgsNameInputInstance() {
    String expectedName = "TestWorkflow";
    Object expectedInput = new Object();
    String expectedInstanceId = "myTestInstance123";

    client.scheduleNewWorkflow(expectedName, expectedInput, expectedInstanceId);

    verify(mockInnerClient, times(1))
        .scheduleNewOrchestrationInstance(expectedName, expectedInput, expectedInstanceId);
  }

  @Test
  public void terminateWorkflow() {
    String expectedArgument = "TestWorkflow";

    client.terminateWorkflow(expectedArgument, null);
    verify(mockInnerClient, times(1)).terminate(expectedArgument, null);
  }

  @Test
  public void close() throws InterruptedException {
    client.close();
    verify(mockInnerClient, times(1)).close();
    verify(mockGrpcChannel, times(1)).shutdown();
  }
}
