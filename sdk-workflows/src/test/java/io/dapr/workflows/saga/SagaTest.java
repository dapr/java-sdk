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

package io.dapr.workflows.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import com.microsoft.durabletask.CompositeTaskFailedException;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskCanceledException;
import com.microsoft.durabletask.TaskOptions;

import io.dapr.workflows.WorkflowContext;
import io.dapr.workflows.runtime.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowActivityContext;

public class SagaTest {

  private WorkflowContext createMockContext(String name, String id) {
    WorkflowContext mockContext = mock(WorkflowContext.class);

    Mockito.doReturn(name).when(mockContext).getName();
    Mockito.doReturn(id).when(mockContext).getInstanceId();
    return mockContext;
  }

  @Test
  public void testSaga_IllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> {
      new Saga(null);
    });
  }

  @Test
  public void testregisterCompensation() {
    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(true).build();
    Saga saga = new Saga(config);

    saga.registerCompensation(MockActivity.class.getName(), new MockActivityInput());
  }

  @Test
  public void testregisterCompensation_IllegalArgument() {
    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(true).build();
    Saga saga = new Saga(config);

    assertThrows(IllegalArgumentException.class, () -> {
      saga.registerCompensation(null, "input");
    });
    assertThrows(IllegalArgumentException.class, () -> {
      saga.registerCompensation("", "input");
    });
  }

  @Test
  public void testCompensateInParallel() {
    MockCompentationActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(true).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input3);

    saga.compensate(new MockWorkflowContext());

    assertEquals(3, MockCompentationActivity.compensateOrder.size());
  }

  @Test
  public void testCompensateInParallel_exception() {
    MockCompentationActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(true).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    input2.setThrowException(true);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input3);

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate(new MockWorkflowContext());
    });
    assertNotNull(exception.getCause());
    // 3 compentation activities, 2 succeed, 1 failed
    assertEquals(0, exception.getSuppressed().length);
    assertEquals(2, MockCompentationActivity.compensateOrder.size());
  }

  @Test
  public void testCompensateInParallel_exception_suppressed() {
    MockCompentationActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(true).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    input2.setThrowException(true);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    input3.setThrowException(true);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input3);

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate(new MockWorkflowContext());
    });
    assertNotNull(exception.getCause());
    // 3 compentation activities, 1 succeed, 2 failed
    assertEquals(1, exception.getSuppressed().length);
    assertEquals(1, MockCompentationActivity.compensateOrder.size());
  }

  @Test
  public void testCompensateSequentially() {
    MockCompentationActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(false).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input3);

    saga.compensate(new MockWorkflowContext());

    assertEquals(3, MockCompentationActivity.compensateOrder.size());

    // the order should be 3 / 2 / 1
    assertEquals(Integer.valueOf(3), MockCompentationActivity.compensateOrder.get(0));
    assertEquals(Integer.valueOf(2), MockCompentationActivity.compensateOrder.get(1));
    assertEquals(Integer.valueOf(1), MockCompentationActivity.compensateOrder.get(2));
  }

  @Test
  public void testCompensateSequentially_continueWithError() {
    MockCompentationActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(true)
        .build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    input2.setThrowException(true);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input3);

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate(new MockWorkflowContext());
    });
    assertNotNull(exception.getCause());
    assertEquals(0, exception.getSuppressed().length);

    // 3 compentation activities, 2 succeed, 1 failed
    assertEquals(2, MockCompentationActivity.compensateOrder.size());
    // the order should be 3 / 1
    assertEquals(Integer.valueOf(3), MockCompentationActivity.compensateOrder.get(0));
    assertEquals(Integer.valueOf(1), MockCompentationActivity.compensateOrder.get(1));
  }

  @Test
  public void testCompensateSequentially_continueWithError_suppressed() {
    MockCompentationActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(true)
        .build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    input2.setThrowException(true);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    input3.setThrowException(true);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input3);

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate(new MockWorkflowContext());
    });
    assertNotNull(exception.getCause());
    assertEquals(1, exception.getSuppressed().length);

    // 3 compentation activities, 1 succeed, 2 failed
    assertEquals(1, MockCompentationActivity.compensateOrder.size());
    // the order should be 3 / 1
    assertEquals(Integer.valueOf(1), MockCompentationActivity.compensateOrder.get(0));
  }

  @Test
  public void testCompensateSequentially_notContinueWithError() {
    MockCompentationActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(false)
        .build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    input2.setThrowException(true);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockCompentationActivity.class.getName(), input3);

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate(new MockWorkflowContext());
    });
    assertNotNull(exception.getCause());
    assertEquals(0, exception.getSuppressed().length);

    // 3 compentation activities, 1 succeed, 1 failed and not continue
    assertEquals(1, MockCompentationActivity.compensateOrder.size());
    // the order should be 3 / 1
    assertEquals(Integer.valueOf(3), MockCompentationActivity.compensateOrder.get(0));
  }

  public static class MockActivity implements WorkflowActivity {

    @Override
    public Object run(WorkflowActivityContext ctx) {
      MockActivityOutput output = new MockActivityOutput();
      output.setSucceed(true);
      return output;
    }
  }

  public static class MockCompentationActivity implements CompensatableWorkflowActivity {

    private static List<Integer> compensateOrder = Collections.synchronizedList(new ArrayList<>());

    @Override
    public Object run(WorkflowActivityContext ctx) {
      MockActivityInput input = ctx.getInput(MockActivityInput.class);

      if (input.isThrowException()) {
        throw new RuntimeException("compensate failed: order=" + input.getOrder());
      }

      compensateOrder.add(input.getOrder());
      return null;
    }
  }

  public static class MockActivityInput {
    private int order = 0;
    private boolean throwException;

    public int getOrder() {
      return order;
    }

    public void setOrder(int order) {
      this.order = order;
    }

    public boolean isThrowException() {
      return throwException;
    }

    public void setThrowException(boolean throwException) {
      this.throwException = throwException;
    }
  }

  public static class MockActivityOutput {
    private boolean succeed;

    public boolean isSucceed() {
      return succeed;
    }

    public void setSucceed(boolean succeed) {
      this.succeed = succeed;
    }
  }

  public static class MockWorkflowContext implements WorkflowContext {

    @Override
    public Logger getLogger() {
      throw new UnsupportedOperationException("Unimplemented method 'getLogger'");
    }

    @Override
    public String getName() {
      throw new UnsupportedOperationException("Unimplemented method 'getName'");
    }

    @Override
    public String getInstanceId() {
      throw new UnsupportedOperationException("Unimplemented method 'getInstanceId'");
    }

    @Override
    public Instant getCurrentInstant() {
      throw new UnsupportedOperationException("Unimplemented method 'getCurrentInstant'");
    }

    @Override
    public void complete(Object output) {
      throw new UnsupportedOperationException("Unimplemented method 'complete'");
    }

    @Override
    public <V> Task<V> waitForExternalEvent(String name, Duration timeout, Class<V> dataType)
        throws TaskCanceledException {
      throw new UnsupportedOperationException("Unimplemented method 'waitForExternalEvent'");
    }

    @Override
    public <V> Task<Void> waitForExternalEvent(String name, Duration timeout) throws TaskCanceledException {
      throw new UnsupportedOperationException("Unimplemented method 'waitForExternalEvent'");
    }

    @Override
    public <V> Task<Void> waitForExternalEvent(String name) throws TaskCanceledException {
      throw new UnsupportedOperationException("Unimplemented method 'waitForExternalEvent'");
    }

    @Override
    public <V> Task<V> callActivity(String name, Object input, TaskOptions options, Class<V> returnType) {
      WorkflowActivity activity;
      WorkflowActivityContext activityContext;
      try {
        activity = (WorkflowActivity) Class.forName(name).getDeclaredConstructor().newInstance();
        activityContext = Mockito.mock(WorkflowActivityContext.class);
        Mockito.doReturn(input).when(activityContext).getInput(Mockito.any());
      } catch (Exception e) {
        fail(e);
        return null;
      }

      activity.run(activityContext);
      return null;
    }

    @Override
    public boolean isReplaying() {
      throw new UnsupportedOperationException("Unimplemented method 'isReplaying'");
    }

    @Override
    public <V> Task<List<V>> allOf(List<Task<V>> tasks) throws CompositeTaskFailedException {
      throw new UnsupportedOperationException("Unimplemented method 'allOf'");
    }

    @Override
    public Task<Task<?>> anyOf(List<Task<?>> tasks) {
      throw new UnsupportedOperationException("Unimplemented method 'anyOf'");
    }

    @Override
    public Task<Void> createTimer(Duration duration) {
      throw new UnsupportedOperationException("Unimplemented method 'createTimer'");
    }

    @Override
    public <V> V getInput(Class<V> targetType) {
      throw new UnsupportedOperationException("Unimplemented method 'getInput'");
    }

    @Override
    public <V> Task<V> callSubWorkflow(String name, Object input, String instanceID, TaskOptions options,
        Class<V> returnType) {
      throw new UnsupportedOperationException("Unimplemented method 'callSubWorkflow'");
    }

    @Override
    public void continueAsNew(Object input, boolean preserveUnprocessedEvents) {
      throw new UnsupportedOperationException("Unimplemented method 'continueAsNew'");
    }

    @Override
    public Saga getSaga() {
      throw new UnsupportedOperationException("Unimplemented method 'getSaga'");
    }

  }
}
