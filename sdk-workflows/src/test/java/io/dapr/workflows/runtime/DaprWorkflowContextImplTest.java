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
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.time.Duration;

import static org.mockito.Mockito.*;

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
    context.getName().block();
    verify(mockInnerContext, times(1)).getName();
  }

  @Test
  public void getInstanceIdTest() {
    context.getInstanceId().block();
    verify(mockInnerContext, times(1)).getInstanceId();
  }

  @Test
  public void waitForExternalEventTest() {
    doReturn(mock(Task.class))
        .when(mockInnerContext).waitForExternalEvent(any(String.class), any(Duration.class));
    DaprWorkflowContextImpl testContext = new DaprWorkflowContextImpl(mockInnerContext);
    String expectedEvent = "TestEvent";
    Duration expectedDuration = Duration.ofSeconds(1);

    testContext.waitForExternalEvent(expectedEvent, expectedDuration).block();
    verify(mockInnerContext, times(1)).waitForExternalEvent(expectedEvent, expectedDuration);
  }

  @Test(expected = IllegalArgumentException.class)
  public void DaprWorkflowContextWithEmptyInnerContext() {
    context = new DaprWorkflowContextImpl(null, null);
  }

  @Test
  public void completeTest() {
    context.complete(null).block();
    verify(mockInnerContext, times(1)).complete(null);
  }

  @Test
  public void getLoggerReplayingTest() {
    Logger mockLogger = mock(Logger.class);
    when(mockInnerContext.getIsReplaying()).thenReturn(true);
    DaprWorkflowContextImpl testContext = new DaprWorkflowContextImpl(mockInnerContext, mockLogger);

    String expectedArg = "test print";
    testContext.getLogger().info(expectedArg);

    verify(mockLogger, times(0)).info(any(String.class));
  }

  @Test
  public void getLoggerFirstTimeTest() {
    Logger mockLogger = mock(Logger.class);
    when(mockInnerContext.getIsReplaying()).thenReturn(false);
    DaprWorkflowContextImpl testContext = new DaprWorkflowContextImpl(mockInnerContext, mockLogger);

    String expectedArg = "test print";
    testContext.getLogger().info(expectedArg);

    verify(mockLogger, times(1)).info(expectedArg);
  }
}
