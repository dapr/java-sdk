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

package io.dapr.workflows.runtime;

import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskOrchestrationContext;
import io.dapr.workflows.DaprWorkflowContextImpl;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DaprWorkflowContextImplTest {
  private DaprWorkflowContextImpl context;
  private TaskOrchestrationContext mockInnerContext;

  @Before
  public void setUp() {
    mockInnerContext = mock(TaskOrchestrationContext.class);
    context = new DaprWorkflowContextImpl(mockInnerContext);
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
  public void waitForExternalEventTest() {
    String expectedEvent = "TestEvent";
    Duration expectedDuration = Duration.ofSeconds(1);

    context.waitForExternalEvent(expectedEvent, expectedDuration);
    verify(mockInnerContext, times(1)).waitForExternalEvent(expectedEvent, expectedDuration, Void.class);
  }

  @Test
  public void callActivityTest() {
    String expectedName = "TestActivity";
    String expectedInput = "TestInput";

    context.callActivity(expectedName, expectedInput, String.class);
    verify(mockInnerContext, times(1)).callActivity(expectedName, expectedInput, null, String.class);
  }


  @Test(expected = IllegalArgumentException.class)
  public void DaprWorkflowContextWithEmptyInnerContext() {
    context = new DaprWorkflowContextImpl(mockInnerContext, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void DaprWorkflowContextWithEmptyLogger() {
    context = new DaprWorkflowContextImpl(null, null);
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
    DaprWorkflowContextImpl testContext = new DaprWorkflowContextImpl(mockInnerContext, mockLogger);

    String expectedArg = "test print";
    testContext.getLogger().info(expectedArg);

    verify(mockLogger, times(0)).info(any(String.class));
  }

  @Test
  public void getLoggerFirstTimeTest() {
    Logger mockLogger = mock(Logger.class);
    when(context.isReplaying()).thenReturn(false);
    DaprWorkflowContextImpl testContext = new DaprWorkflowContextImpl(mockInnerContext, mockLogger);

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
  public void callSubWorkflow() {
    String expectedName = "TestActivity";
    String expectedInput = "TestInput";

    context.callSubWorkflow(expectedName, expectedInput, String.class);
    verify(mockInnerContext, times(1)).callSubOrchestrator(expectedName, expectedInput, null, null, String.class);
  }
}
