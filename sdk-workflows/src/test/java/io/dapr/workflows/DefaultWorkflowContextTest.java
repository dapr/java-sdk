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

import io.dapr.durabletask.CompositeTaskFailedException;
import io.dapr.durabletask.RetryContext;
import io.dapr.durabletask.RetryHandler;
import io.dapr.durabletask.Task;
import io.dapr.durabletask.TaskCanceledException;
import io.dapr.durabletask.TaskOptions;
import io.dapr.durabletask.TaskOrchestrationContext;
import io.dapr.workflows.runtime.DefaultWorkflowContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class DefaultWorkflowContextTest {
  private DefaultWorkflowContext context;
  private DefaultWorkflowContext contextWithClass;
  private TaskOrchestrationContext mockInnerContext;
  private WorkflowContext testWorkflowContext;

  @BeforeEach
  public void setUp() {
    mockInnerContext = mock(TaskOrchestrationContext.class);
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
      public Task<Void> waitForExternalEvent(String name, Duration timeout) throws TaskCanceledException {
        return null;
      }

      @Override
      public Task<Void> waitForExternalEvent(String name) throws TaskCanceledException {
        return null;
      }

      @Override
      public <V> Task<V> callActivity(String name, Object input, WorkflowTaskOptions options, Class<V> returnType) {
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
      public Task<Void> createTimer(ZonedDateTime zonedDateTime) {
        return null;
      }

      @Override
      public <V> V getInput(Class<V> targetType) {
        return null;
      }

      @Override
      public <V> Task<V> callChildWorkflow(String name, @Nullable Object input, @Nullable String instanceID,
                                           @Nullable WorkflowTaskOptions options, Class<V> returnType) {
        return null;
      }

      @Override
      public void continueAsNew(Object input, boolean preserveUnprocessedEvents) {
      }

      @Override
      public void setCustomStatus(Object status) {

      }
    };
    context = new DefaultWorkflowContext(mockInnerContext);
    contextWithClass = new DefaultWorkflowContext(mockInnerContext, testWorkflowContext.getClass());
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
    assertThrows(IllegalArgumentException.class, () ->
        context = new DefaultWorkflowContext(mockInnerContext, (Logger)null));  }

  @Test
  public void DaprWorkflowContextWithEmptyLogger() {
    assertThrows(IllegalArgumentException.class, () -> context = new DefaultWorkflowContext(null, (Logger)null));
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
    DefaultWorkflowContext testContext = new DefaultWorkflowContext(mockInnerContext, mockLogger);

    String expectedArg = "test print";
    testContext.getLogger().info(expectedArg);

    verify(mockLogger, times(0)).info(any(String.class));
  }

  @Test
  public void getLoggerFirstTimeTest() {
    Logger mockLogger = mock(Logger.class);
    when(context.isReplaying()).thenReturn(false);
    DefaultWorkflowContext testContext = new DefaultWorkflowContext(mockInnerContext, mockLogger);

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
  public void createTimerWithZonedDateTimeTest() {
    ZonedDateTime now = ZonedDateTime.now();
    context.createTimer(now);
    verify(mockInnerContext, times(1)).createTimer(now);
  }

  @Test
  public void callChildWorkflowWithName() {
    String expectedName = "TestActivity";

    context.callChildWorkflow(expectedName);
    verify(mockInnerContext, times(1)).callSubOrchestrator(expectedName, null, null, null, null);
  }

  @Test
  public void callChildWorkflowWithRetryPolicy() {
    String expectedName = "TestActivity";
    String expectedInput = "TestInput";
    String expectedInstanceId = "TestInstanceId";
    WorkflowTaskRetryPolicy retryPolicy = WorkflowTaskRetryPolicy.newBuilder()
        .setMaxNumberOfAttempts(1)
        .setFirstRetryInterval(Duration.ofSeconds(10))
        .build();
    WorkflowTaskOptions executionOptions = new WorkflowTaskOptions(retryPolicy);
    ArgumentCaptor<TaskOptions> captor = ArgumentCaptor.forClass(TaskOptions.class);

    context.callChildWorkflow(expectedName, expectedInput, expectedInstanceId, executionOptions, String.class);

    verify(mockInnerContext, times(1))
        .callSubOrchestrator(
            eq(expectedName),
            eq(expectedInput),
            eq(expectedInstanceId),
            captor.capture(),
            eq(String.class)
        );

    TaskOptions taskOptions = captor.getValue();

    assertEquals(retryPolicy.getMaxNumberOfAttempts(), taskOptions.getRetryPolicy().getMaxNumberOfAttempts());
    assertEquals(retryPolicy.getFirstRetryInterval(), taskOptions.getRetryPolicy().getFirstRetryInterval());
    assertEquals(Duration.ZERO, taskOptions.getRetryPolicy().getRetryTimeout());
    assertNull(taskOptions.getRetryHandler());
  }

  @Test
  public void callChildWorkflowWithRetryHandler() {
    String expectedName = "TestActivity";
    String expectedInput = "TestInput";
    String expectedInstanceId = "TestInstanceId";

    WorkflowTaskRetryHandler retryHandler = spy(new WorkflowTaskRetryHandler() {
      @Override
      public boolean handle(WorkflowTaskRetryContext retryContext) {
        return true;
      }
    });

    WorkflowTaskOptions executionOptions = new WorkflowTaskOptions(retryHandler);
    ArgumentCaptor<TaskOptions> captor = ArgumentCaptor.forClass(TaskOptions.class);

    context.callChildWorkflow(expectedName, expectedInput, expectedInstanceId, executionOptions, String.class);

    verify(mockInnerContext, times(1))
            .callSubOrchestrator(
                    eq(expectedName),
                    eq(expectedInput),
                    eq(expectedInstanceId),
                    captor.capture(),
                    eq(String.class)
            );

    TaskOptions taskOptions = captor.getValue();

    RetryHandler durableRetryHandler = taskOptions.getRetryHandler();
    RetryContext retryContext = mock(RetryContext.class, invocationOnMock -> null);

    durableRetryHandler.handle(retryContext);

    verify(retryHandler, times(1)).handle(any());
    assertNull(taskOptions.getRetryPolicy());
  }

  @Test
  public void callChildWorkflowWithRetryPolicyAndHandler() {
    String expectedName = "TestActivity";
    String expectedInput = "TestInput";
    String expectedInstanceId = "TestInstanceId";

    WorkflowTaskRetryPolicy retryPolicy = WorkflowTaskRetryPolicy.newBuilder()
            .setMaxNumberOfAttempts(1)
            .setFirstRetryInterval(Duration.ofSeconds(10))
            .build();

    WorkflowTaskRetryHandler retryHandler = spy(new WorkflowTaskRetryHandler() {
      @Override
      public boolean handle(WorkflowTaskRetryContext retryContext) {
        return true;
      }
    });

    WorkflowTaskOptions executionOptions = new WorkflowTaskOptions(retryPolicy, retryHandler);
    ArgumentCaptor<TaskOptions> captor = ArgumentCaptor.forClass(TaskOptions.class);

    context.callChildWorkflow(expectedName, expectedInput, expectedInstanceId, executionOptions, String.class);

    verify(mockInnerContext, times(1))
            .callSubOrchestrator(
                    eq(expectedName),
                    eq(expectedInput),
                    eq(expectedInstanceId),
                    captor.capture(),
                    eq(String.class)
            );

    TaskOptions taskOptions = captor.getValue();

    RetryHandler durableRetryHandler = taskOptions.getRetryHandler();
    RetryContext retryContext = mock(RetryContext.class, invocationOnMock -> null);

    durableRetryHandler.handle(retryContext);

    verify(retryHandler, times(1)).handle(any());
    assertEquals(retryPolicy.getMaxNumberOfAttempts(), taskOptions.getRetryPolicy().getMaxNumberOfAttempts());
    assertEquals(retryPolicy.getFirstRetryInterval(), taskOptions.getRetryPolicy().getFirstRetryInterval());
    assertEquals(Duration.ZERO, taskOptions.getRetryPolicy().getRetryTimeout());
  }

  @Test
  public void callChildWorkflow() {
    String expectedName = "TestActivity";
    String expectedInput = "TestInput";

    context.callChildWorkflow(expectedName, expectedInput, String.class);
    verify(mockInnerContext, times(1)).callSubOrchestrator(expectedName, expectedInput, null, null, String.class);
  }

  @Test
  public void setCustomStatusWorkflow() {
    String customStatus = "CustomStatus";

    context.setCustomStatus(customStatus);
    verify(mockInnerContext, times(1)).setCustomStatus(customStatus);

  }

  @Test
  public void newUuidTest() {
    context.newUuid();
    verify(mockInnerContext, times(1)).newUuid();
  }

  @Test
  public void newUuidTestNoImplementationExceptionTest() {
    RuntimeException runtimeException = assertThrows(RuntimeException.class, testWorkflowContext::newUuid);
    String expectedMessage = "No implementation found.";
    assertEquals(expectedMessage, runtimeException.getMessage());
  }

  @Test
  public void workflowRetryPolicyRetryTimeoutValueShouldHaveRightValueWhenBeingSet() {
    String expectedName = "TestActivity";
    String expectedInput = "TestInput";
    String expectedInstanceId = "TestInstanceId";
    WorkflowTaskRetryPolicy retryPolicy = WorkflowTaskRetryPolicy.newBuilder()
            .setMaxNumberOfAttempts(1)
            .setFirstRetryInterval(Duration.ofSeconds(10))
            .setRetryTimeout(Duration.ofSeconds(10))
            .build();
    WorkflowTaskOptions executionOptions = new WorkflowTaskOptions(retryPolicy);
    ArgumentCaptor<TaskOptions> captor = ArgumentCaptor.forClass(TaskOptions.class);

    context.callChildWorkflow(expectedName, expectedInput, expectedInstanceId, executionOptions, String.class);

    verify(mockInnerContext, times(1))
            .callSubOrchestrator(
                    eq(expectedName),
                    eq(expectedInput),
                    eq(expectedInstanceId),
                    captor.capture(),
                    eq(String.class)
            );

    TaskOptions taskOptions = captor.getValue();

    assertEquals(Duration.ofSeconds(10), taskOptions.getRetryPolicy().getRetryTimeout());
  }

  @Test
  public void workflowRetryPolicyRetryThrowIllegalArgumentWhenNullRetryTimeoutIsSet() {
    assertThrows(IllegalArgumentException.class, () ->
            WorkflowTaskRetryPolicy.newBuilder()
                    .setMaxNumberOfAttempts(1)
                    .setFirstRetryInterval(Duration.ofSeconds(10))
                    .setRetryTimeout(null)
                    .build());
  }

  @Test
  public void workflowRetryPolicyRetryThrowIllegalArgumentWhenRetryTimeoutIsLessThanMaxRetryInterval() {
    assertThrows(IllegalArgumentException.class, () -> WorkflowTaskRetryPolicy.newBuilder()
            .setMaxNumberOfAttempts(1)
            .setFirstRetryInterval(Duration.ofSeconds(10))
            .setRetryTimeout(Duration.ofSeconds(9))
            .build());
  }
}
