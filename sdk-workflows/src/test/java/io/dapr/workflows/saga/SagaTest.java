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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.dapr.workflows.WorkflowActivityContext;
import io.dapr.workflows.WorkflowTaskOptions;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.microsoft.durabletask.Task;

import io.dapr.workflows.WorkflowContext;
import io.dapr.workflows.WorkflowActivity;

public class SagaTest {

  public static WorkflowContext createMockContext() {
    WorkflowContext workflowContext = mock(WorkflowContext.class);
    when(workflowContext.callActivity(anyString(), any(), eq((WorkflowTaskOptions) null))).thenAnswer(new ActivityAnswer());
    when(workflowContext.allOf(anyList())).thenAnswer(new AllActivityAnswer());

    return workflowContext;
  }

  @Test
  public void testSaga_IllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> {
      new Saga(null);
    });
  }

  @Test
  public void testregisterCompensation() {
    SagaOptions config = SagaOptions.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(true).build();
    Saga saga = new Saga(config);

    saga.registerCompensation(MockActivity.class.getName(), new MockActivityInput());
  }

  @Test
  public void testregisterCompensation_IllegalArgument() {
    SagaOptions config = SagaOptions.newBuilder()
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
    MockCompensationActivity.compensateOrder.clear();

    SagaOptions config = SagaOptions.newBuilder()
        .setParallelCompensation(true).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input3);

    saga.compensate(createMockContext());

    assertEquals(3, MockCompensationActivity.compensateOrder.size());
  }

  @Test
  public void testCompensateInParallel_exception_1failed() {
    MockCompensationActivity.compensateOrder.clear();

    SagaOptions config = SagaOptions.newBuilder()
        .setParallelCompensation(true).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    input2.setThrowException(true);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input3);

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate(createMockContext());
    });
    assertNotNull(exception.getCause());
    // 3 compentation activities, 2 succeed, 1 failed
    assertEquals(0, exception.getSuppressed().length);
    assertEquals(2, MockCompensationActivity.compensateOrder.size());
  }

  @Test
  public void testCompensateInParallel_exception_2failed() {
    MockCompensationActivity.compensateOrder.clear();

    SagaOptions config = SagaOptions.newBuilder()
        .setParallelCompensation(true).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    input2.setThrowException(true);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    input3.setThrowException(true);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input3);

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate(createMockContext());
    });
    assertNotNull(exception.getCause());
    // 3 compentation activities, 1 succeed, 2 failed
    assertEquals(1, MockCompensationActivity.compensateOrder.size());
  }

  @Test
  public void testCompensateInParallel_exception_3failed() {
    MockCompensationActivity.compensateOrder.clear();

    SagaOptions config = SagaOptions.newBuilder()
        .setParallelCompensation(true).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    input1.setThrowException(true);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    input2.setThrowException(true);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    input3.setThrowException(true);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input3);

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate(createMockContext());
    });
    assertNotNull(exception.getCause());
    // 3 compentation activities, 0 succeed, 3 failed
    assertEquals(0, MockCompensationActivity.compensateOrder.size());
  }

  @Test
  public void testCompensateSequentially() {
    MockCompensationActivity.compensateOrder.clear();

    SagaOptions config = SagaOptions.newBuilder()
        .setParallelCompensation(false).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input3);

    saga.compensate(createMockContext());

    assertEquals(3, MockCompensationActivity.compensateOrder.size());

    // the order should be 3 / 2 / 1
    assertEquals(Integer.valueOf(3), MockCompensationActivity.compensateOrder.get(0));
    assertEquals(Integer.valueOf(2), MockCompensationActivity.compensateOrder.get(1));
    assertEquals(Integer.valueOf(1), MockCompensationActivity.compensateOrder.get(2));
  }

  @Test
  public void testCompensateSequentially_continueWithError() {
    MockCompensationActivity.compensateOrder.clear();

    SagaOptions config = SagaOptions.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(true)
        .build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    input2.setThrowException(true);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input3);

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate(createMockContext());
    });
    assertNotNull(exception.getCause());
    assertEquals(0, exception.getSuppressed().length);

    // 3 compentation activities, 2 succeed, 1 failed
    assertEquals(2, MockCompensationActivity.compensateOrder.size());
    // the order should be 3 / 1
    assertEquals(Integer.valueOf(3), MockCompensationActivity.compensateOrder.get(0));
    assertEquals(Integer.valueOf(1), MockCompensationActivity.compensateOrder.get(1));
  }

  @Test
  public void testCompensateSequentially_continueWithError_suppressed() {
    MockCompensationActivity.compensateOrder.clear();

    SagaOptions config = SagaOptions.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(true)
        .build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    input2.setThrowException(true);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    input3.setThrowException(true);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input3);

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate(createMockContext());
    });
    assertNotNull(exception.getCause());
    assertEquals(1, exception.getSuppressed().length);

    // 3 compentation activities, 1 succeed, 2 failed
    assertEquals(1, MockCompensationActivity.compensateOrder.size());
    // the order should be 3 / 1
    assertEquals(Integer.valueOf(1), MockCompensationActivity.compensateOrder.get(0));
  }

  @Test
  public void testCompensateSequentially_notContinueWithError() {
    MockCompensationActivity.compensateOrder.clear();

    SagaOptions config = SagaOptions.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(false)
        .build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input1);
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    input2.setThrowException(true);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input2);
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockCompensationActivity.class.getName(), input3);

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate(createMockContext());
    });
    assertNotNull(exception.getCause());
    assertEquals(0, exception.getSuppressed().length);

    // 3 compentation activities, 1 succeed, 1 failed and not continue
    assertEquals(1, MockCompensationActivity.compensateOrder.size());
    // the order should be 3 / 1
    assertEquals(Integer.valueOf(3), MockCompensationActivity.compensateOrder.get(0));
  }

  public static class MockActivity implements WorkflowActivity {

    @Override
    public Object run(WorkflowActivityContext ctx) {
      MockActivityOutput output = new MockActivityOutput();
      output.setSucceed(true);
      return output;
    }
  }

  public static class MockCompensationActivity implements WorkflowActivity {

    private static final List<Integer> compensateOrder = Collections.synchronizedList(new ArrayList<>());

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

  public static class ActivityAnswer implements Answer<Task<Void>> {

    @Override
    public Task<Void> answer(InvocationOnMock invocation) throws Throwable {
      Object[] args = invocation.getArguments();
      String name = (String) args[0];
      Object input = args[1];

      WorkflowActivity activity;
      WorkflowActivityContext activityContext = Mockito.mock(WorkflowActivityContext.class);
      try {
        activity = (WorkflowActivity) Class.forName(name).getDeclaredConstructor().newInstance();
      } catch (Exception e) {
        fail(e);
        return null;
      }

      Task<Void> task = mock(Task.class);
      when(task.await()).thenAnswer(invocation1 -> {
        Mockito.doReturn(input).when(activityContext).getInput(Mockito.any());
        activity.run(activityContext);
        return null;
      });
      return task;
    }

  }

  public static class AllActivityAnswer implements Answer<Task<Void>> {
    @Override
    public Task<Void> answer(InvocationOnMock invocation) throws Throwable {
      Object[] args = invocation.getArguments();
      List<Task<Void>> tasks = (List<Task<Void>>) args[0];

      ExecutorService executor = Executors.newFixedThreadPool(5);
      List<Callable<Void>> compensationTasks = new ArrayList<>();
      for (Task<Void> task : tasks) {
        Callable<Void> compensationTask = new Callable<Void>() {
          @Override
          public Void call() {
            return task.await();
          }
        };
        compensationTasks.add(compensationTask);
      }

      List<Future<Void>> resultFutures;
      try {
        resultFutures = executor.invokeAll(compensationTasks, 2, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        fail(e);
        return null;
      }

      Task<Void> task = mock(Task.class);
      when(task.await()).thenAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          Exception exception = null;
          for (Future<Void> resultFuture : resultFutures) {
            try {
              resultFuture.get();
            } catch (Exception e) {
              exception = e;
            }
          }
          if (exception != null) {
            throw exception;
          }
          return null;
        }
      });
      return task;
    }
  }

}
