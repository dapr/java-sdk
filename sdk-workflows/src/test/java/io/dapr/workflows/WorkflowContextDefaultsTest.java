/*
 * Copyright 2025 The Dapr Authors
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

import io.dapr.workflows.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

/**
 * Covers the shorthand forms {@link WorkflowContext} provides as default methods. Each one must
 * delegate to the full form with the arguments the caller left out filled in.
 */
public class WorkflowContextDefaultsTest {

  private static final String ACTIVITY = "DemoActivity";
  private static final String CHILD = "ChildWorkflow";
  private static final String EVENT = "Approval";
  private static final String INSTANCE_ID = "instance-1";

  private WorkflowContext context;

  @BeforeEach
  public void setUp() {
    context = mock(WorkflowContext.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  public void shouldTreatVarargsAsAListForAllOfAndAnyOf() {
    Task<String> first = mock(Task.class);
    Task<String> second = mock(Task.class);

    context.allOf(first, second);
    context.anyOf(first, second);

    verify(context).allOf(Arrays.asList(first, second));
    verify(context).anyOf(Arrays.asList(first, second));
  }

  @Test
  public void shouldDefaultAnActivityWithNoReturnTypeToVoid() {
    context.callActivity(ACTIVITY);

    verify(context).callActivity(ACTIVITY, Void.class);
  }

  @Test
  public void shouldDefaultAnActivityWithNoOptionsToNoOptions() {
    context.callActivity(ACTIVITY, "the input");
    context.callActivity(ACTIVITY, "the input", String.class);

    verify(context).callActivity(ACTIVITY, "the input", null, Void.class);
    verify(context).callActivity(ACTIVITY, "the input", null, String.class);
  }

  @Test
  public void shouldDefaultAnActivityWithNoInputToNoInput() {
    context.callActivity(ACTIVITY, String.class);

    verify(context).callActivity(ACTIVITY, null, null, String.class);
  }

  @Test
  public void shouldDefaultAnActivityWithOptionsButNoReturnTypeToVoid() {
    WorkflowTaskOptions options = new WorkflowTaskOptions((WorkflowTaskRetryPolicy) null);

    context.callActivity(ACTIVITY, "the input", options);

    verify(context).callActivity(ACTIVITY, "the input", options, Void.class);
  }

  @Test
  public void shouldContinueAsNewPreservingUnprocessedEventsByDefault() {
    context.continueAsNew("the input");

    verify(context).continueAsNew("the input", true);
  }

  @Test
  public void shouldSendAnEventWithNoPayloadAsANullPayload() {
    context.sendEvent(INSTANCE_ID, EVENT);

    verify(context).sendEvent(INSTANCE_ID, EVENT, null);
  }

  @Test
  public void shouldDefaultChildWorkflowArgumentsThatWereLeftOut() {
    WorkflowTaskOptions options = new WorkflowTaskOptions((WorkflowTaskRetryPolicy) null);

    context.callChildWorkflow(CHILD);
    context.callChildWorkflow(CHILD, "the input");
    context.callChildWorkflow(CHILD, "the input", String.class);
    context.callChildWorkflow(CHILD, "the input", INSTANCE_ID, String.class);
    context.callChildWorkflow(CHILD, "the input", INSTANCE_ID, options);

    verify(context).callChildWorkflow(CHILD, null);
    verify(context).callChildWorkflow(CHILD, "the input", Void.class);
    verify(context).callChildWorkflow(CHILD, "the input", null, String.class);
    verify(context).callChildWorkflow(CHILD, "the input", INSTANCE_ID, null, String.class);
    verify(context).callChildWorkflow(CHILD, "the input", INSTANCE_ID, options, Void.class);
  }

  @Test
  public void shouldDefaultAnExternalEventWithNoDataTypeToVoid() {
    Duration timeout = Duration.ofMinutes(1);

    context.waitForExternalEvent(EVENT, timeout);
    context.waitForExternalEvent(EVENT);

    verify(context).waitForExternalEvent(EVENT, timeout, Void.class);
    verify(context).waitForExternalEvent(EVENT, Void.class);
  }

  @Test
  public void shouldWaitForeverWhenNoTimeoutIsGiven() {
    context.waitForExternalEvent(EVENT, String.class);

    verify(context).waitForExternalEvent(eq(EVENT), isNull(), eq(String.class));
  }

  @Test
  public void shouldSayItHasNoUuidImplementationRatherThanReturnARandomOne() {
    RuntimeException thrown = assertThrows(RuntimeException.class, () -> context.newUuid());

    assertEquals("No implementation found.", thrown.getMessage(),
        "a context that cannot produce a deterministic Uuid must not fall back to a random one");
  }

  @Test
  public void shouldPassAListStraightThroughToAllOf() {
    List<Task<String>> tasks = Arrays.asList(mock(Task.class), mock(Task.class));

    context.allOf(tasks);

    verify(context).allOf(any(List.class));
  }
}
