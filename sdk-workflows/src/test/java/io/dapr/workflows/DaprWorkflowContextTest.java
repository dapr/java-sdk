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

package io.dapr.workflows;

import com.microsoft.durabletask.CompositeTaskFailedException;
import com.microsoft.durabletask.RetryPolicy;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskCanceledException;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;

import io.dapr.workflows.runtime.DaprWorkflowContext;
import io.dapr.workflows.saga.Saga;
import io.dapr.workflows.saga.SagaContext;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DaprWorkflowContextTest {
  private DaprWorkflowContext context;
  private TaskOrchestrationContext mockInnerContext;
  private WorkflowContext testWorkflowContext;

  @BeforeEach
  public void setUp() {
    mockInnerContext = mock(TaskOrchestrationContext.class);
    context = new DaprWorkflowContext(mockInnerContext);
    testWorkflowContext = new WorkflowContext() {
      @Override
      public Logger getLogger() {
        return null;
      }

      @Override
      public String getName() {
        return null;
      }

      @Override
      public String getInstanceId() {
        return null;
      }

      @Override
      public Instant getCurrentInstant() {
        return null;
      }

      @Override
      public void complete(Object output) {

      }

      @Override
      public <V> Task<V> waitForExternalEvent(String name, Duration timeout, Class<V> dataType)
          throws TaskCanceledException {
        return null;
      }

      @Override
      public <V> Task<Void> waitForExternalEvent(String name, Duration timeout) throws TaskCanceledException {
        return null;
      }

      @Override
      public <V> Task<Void> waitForExternalEvent(String name) throws TaskCanceledException {
        return null;
      }

      @Override
      public <V> Task<V> callActivity(String name, Object input, TaskOptions options, Class<V> returnType) {
        return null;
      }

      @Override
      public boolean isReplaying() {
        return false;
      }

      @Override
      public <V> Task<List<V>> allOf(List<Task<V>> tasks) throws CompositeTaskFailedException {
        return null;
      }

      @Override
      public Task<Task<?>> anyOf(List<Task<?>> tasks) {
        return null;
      }

      @Override
      public Task<Void> createTimer(Duration duration) {
        return null;
      }

      @Override
      public <V> V getInput(Class<V> targetType) {
        return null;
      }

      @Override
      public <V> Task<V> callChildWorkflow(String name, @Nullable Object input, @Nullable String instanceID,
                                         @Nullable TaskOptions options, Class<V> returnType) {
        return null;
      }

      @Override
      public void continueAsNew(Object input, boolean preserveUnprocessedEvents) {

      }

      @Override
      public SagaContext getSagaContext() {
        return null;
      }
    };
  }

  @Test
  public void getNameTest() {
    context.getName();
    verify(mockInnerContext, times(1)).getName();
  }

  @Test
  public void getInstanceIdTest() {
    context.getInstanceId();
    verify(mockInnerContext, times(1)).getInstanceId();
  }

  @Test
  public void getCurrentInstantTest() {
    context.getCurrentInstant();
    verify(mockInnerContext, times(1)).getCurrentInstant();
  }

  @Test
  public void waitForExternalEventWithEventAndDurationTest() {
    String expectedEvent = "TestEvent";
    Duration expectedDuration = Duration.ofSeconds(1);

    context.waitForExternalEvent(expectedEvent, expectedDuration);
    verify(mockInnerContext, times(1)).waitForExternalEvent(expectedEvent, expectedDuration, Void.class);
  }

  @Test
  public void waitForExternalEventTest() {
    String expectedEvent = "TestEvent";
    Duration expectedDuration = Duration.ofSeconds(1);

    context.waitForExternalEvent(expectedEvent, expectedDuration, String.class);
    verify(mockInnerContext, times(1)).waitForExternalEvent(expectedEvent, expectedDuration, String.class);
  }

  @Test
  public void callActivityTest() {
    String expectedName = "TestActivity";
    String expectedInput = "TestInput";

    context.callActivity(expectedName, expectedInput, String.class);
    verify(mockInnerContext, times(1)).callActivity(expectedName, expectedInput, null, String.class);
  }

  @Test
  public void DaprWorkflowContextWithEmptyInnerContext() {
    assertThrows(IllegalArgumentException.class, () -> {
      context = new DaprWorkflowContext(mockInnerContext, (Logger)null);
    });  }

  @Test
  public void DaprWorkflowContextWithEmptyLogger() {
    assertThrows(IllegalArgumentException.class, () -> {
      context = new DaprWorkflowContext(null, (Logger)null);
    });
  }

  @Test
  public void completeTest() {
    context.complete(null);
    verify(mockInnerContext, times(1)).complete(null);
  }

  @Test
  public void getIsReplayingTest() {
    context.isReplaying();
    verify(mockInnerContext, times(1)).getIsReplaying();
  }

  @Test
  public void getLoggerReplayingTest() {
    Logger mockLogger = mock(Logger.class);
    when(context.isReplaying()).thenReturn(true);
    DaprWorkflowContext testContext = new DaprWorkflowContext(mockInnerContext, mockLogger);

    String expectedArg = "test print";
    testContext.getLogger().info(expectedArg);

    verify(mockLogger, times(0)).info(any(String.class));
  }

  @Test
  public void getLoggerFirstTimeTest() {
    Logger mockLogger = mock(Logger.class);
    when(context.isReplaying()).thenReturn(false);
    DaprWorkflowContext testContext = new DaprWorkflowContext(mockInnerContext, mockLogger);

    String expectedArg = "test print";
    testContext.getLogger().info(expectedArg);

    verify(mockLogger, times(1)).info(expectedArg);
  }

  @Test
  public void continueAsNewTest() {
    String expectedInput = "TestInput";
    context.continueAsNew(expectedInput);
    verify(mockInnerContext, times(1)).continueAsNew(expectedInput);
  }

  @Test
  public void allOfTest() {
    Task<Void> t1 = mockInnerContext.callActivity("task1");
    Task<Void> t2 = mockInnerContext.callActivity("task2");
    List<Task<Void>> taskList = Arrays.asList(t1, t2);
    context.allOf(taskList);
    verify(mockInnerContext, times(1)).allOf(taskList);
  }

  @Test
  public void anyOfTest() {
    Task<Void> t1 = mockInnerContext.callActivity("task1");
    Task<Void> t2 = mockInnerContext.callActivity("task2");
    Task<Void> t3 = mockInnerContext.callActivity("task3");
    List<Task<?>> taskList = Arrays.asList(t1, t2);

    context.anyOf(taskList);
    verify(mockInnerContext, times(1)).anyOf(taskList);

    context.anyOf(t1, t2, t3);
    verify(mockInnerContext, times(1)).anyOf(Arrays.asList(t1, t2, t3));
  }

  @Test
  public void createTimerTest() {
    context.createTimer(Duration.ofSeconds(10));
    verify(mockInnerContext, times(1)).createTimer(Duration.ofSeconds(10));
  }

  @Test
  public void createTimerWithZonedDateTimeThrowsTest() {
    assertThrows(UnsupportedOperationException.class, () -> context.createTimer(ZonedDateTime.now()));
  }

  @Test
  public void callChildWorkflowWithName() {
    String expectedName = "TestActivity";

    context.callChildWorkflow(expectedName);
    verify(mockInnerContext, times(1)).callSubOrchestrator(expectedName, null, null, null, null);
  }

  @Test
  public void callChildWorkflowWithOptions() {
    String expectedName = "TestActivity";
    String expectedInput = "TestInput";
    String expectedInstanceId = "TestInstanceId";
    TaskOptions expectedOptions = new TaskOptions(new RetryPolicy(1, Duration.ofSeconds(10)));

    context.callChildWorkflow(expectedName, expectedInput, expectedInstanceId, expectedOptions, String.class);
    verify(mockInnerContext, times(1)).callSubOrchestrator(expectedName, expectedInput, expectedInstanceId,
        expectedOptions, String.class);
  }

  @Test
  public void callChildWorkflow() {
    String expectedName = "TestActivity";
    String expectedInput = "TestInput";

    context.callChildWorkflow(expectedName, expectedInput, String.class);
    verify(mockInnerContext, times(1)).callSubOrchestrator(expectedName, expectedInput, null, null, String.class);
  }

  @Test
  public void newUuidTest() {
    context.newUuid();
    verify(mockInnerContext, times(1)).newUUID();
  }

  @Test
  public void newUuidTestNoImplementationExceptionTest() {
    RuntimeException runtimeException = assertThrows(RuntimeException.class, testWorkflowContext::newUuid);
    String expectedMessage = "No implementation found.";
    assertEquals(expectedMessage, runtimeException.getMessage());
  }

  @Test
  public void getSagaContextTest_sagaEnabled() {
    Saga saga = mock(Saga.class);
    WorkflowContext context = new DaprWorkflowContext(mockInnerContext, saga);

    SagaContext sagaContext = context.getSagaContext();
    assertNotNull("SagaContext should not be null", sagaContext);
  }

  @Test
  public void getSagaContextTest_sagaDisabled() {
    WorkflowContext context = new DaprWorkflowContext(mockInnerContext);
    assertThrows(UnsupportedOperationException.class, () -> {
      context.getSagaContext();
    });
  }
}
