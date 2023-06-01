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

import com.microsoft.durabletask.TaskOrchestrationContext;
import org.junit.Test;
import org.junit.Before;

import java.io.PrintStream;
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
    context.getName();
    verify(mockInnerContext, times(1)).getName();
  }

  @Test
  public void getInstanceIdTest() {
    context.getInstanceId();
    verify(mockInnerContext, times(1)).getInstanceId();
  }

  @Test
  public void waitForExternalEventTest() {
    String expectedEvent = "TestEvent";
    Duration expectedDuration = Duration.ofSeconds(1);

    context.waitForExternalEvent(expectedEvent, expectedDuration);
    verify(mockInnerContext, times(1)).waitForExternalEvent(expectedEvent, expectedDuration);
  }

  @Test
  public void waitForExternalEventTimeoutTest() {
    String expectedEvent = "TestEvent";
    Duration expectedDuration = Duration.ofSeconds(1);

    context.waitForExternalEvent(expectedEvent, expectedDuration);
    verify(mockInnerContext, times(1)).waitForExternalEvent(expectedEvent, expectedDuration);
  }

  @Test(expected = IllegalArgumentException.class)
  public void DaprWorkflowContextWithEmptyInnerContext() {
    context = new DaprWorkflowContextImpl(null);
  }

  @Test
  public void completeTest() {
    context.complete(null);
    verify(mockInnerContext, times(1)).complete(null);
  }

  @Test
  public void getOutReplayingTest() {
    PrintStream mockOut = mock(PrintStream.class);
    PrintStream originalOut = System.out;
    System.setOut(mockOut);
    when(mockInnerContext.getIsReplaying()).thenReturn(true);
    DaprWorkflowContextImpl testContext = new DaprWorkflowContextImpl(mockInnerContext);

    String expectedArg = "test print";
    testContext.getOut().println(expectedArg);

    verify(mockOut, times(0)).println(any(String.class));
    System.setOut(originalOut);
  }

  @Test
  public void getOutFirstTimeTest() {
    PrintStream mockOut = mock(PrintStream.class);
    PrintStream originalOut = System.out;
    System.setOut(mockOut);
    when(mockInnerContext.getIsReplaying()).thenReturn(false);
    DaprWorkflowContextImpl testContext = new DaprWorkflowContextImpl(mockInnerContext);

    String expectedArg = "test print";
    testContext.getOut().println(expectedArg);

    verify(mockOut, times(1)).println(expectedArg);
    System.setOut(originalOut);
  }

  @Test
  public void getErrReplayingTest() {
    PrintStream mockErr = mock(PrintStream.class);
    PrintStream originalErr = System.err;
    System.setOut(mockErr);
    when(mockInnerContext.getIsReplaying()).thenReturn(true);
    DaprWorkflowContextImpl testContext = new DaprWorkflowContextImpl(mockInnerContext);

    String expectedArg = "test print";
    testContext.getErr().println(expectedArg);

    verify(mockErr, times(0)).println(any(String.class));
    System.setOut(originalErr);
  }

  @Test
  public void getErrFirstTimeTest() {
    PrintStream mockErr = mock(PrintStream.class);
    PrintStream originalErr = System.err;
    System.setErr(mockErr);
    when(mockInnerContext.getIsReplaying()).thenReturn(false);
    DaprWorkflowContextImpl testContext = new DaprWorkflowContextImpl(mockInnerContext);

    String expectedArg = "test print";
    testContext.getErr().println(expectedArg);

    verify(mockErr, times(1)).println(expectedArg);
    System.setOut(originalErr);
  }
}
