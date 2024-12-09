package io.dapr.workflows.saga;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.dapr.workflows.runtime.saga.DefaultSagaContext;
import org.junit.Test;

import io.dapr.workflows.WorkflowContext;

public class DefaultSagaContextTest {

  @Test
  public void testDaprSagaContextImpl_IllegalArgumentException() {
    Saga saga = mock(Saga.class);
    WorkflowContext workflowContext = mock(WorkflowContext.class);

    assertThrows(IllegalArgumentException.class, () -> {
      new DefaultSagaContext(saga, null);
    });

    assertThrows(IllegalArgumentException.class, () -> {
      new DefaultSagaContext(null, workflowContext);
    });
  }

  @Test
  public void test_registerCompensation() {
    Saga saga = mock(Saga.class);
    WorkflowContext workflowContext = mock(WorkflowContext.class);
    DefaultSagaContext ctx = new DefaultSagaContext(saga, workflowContext);

    String activityClassName = "name1";
    Object activityInput = new Object();
    doNothing().when(saga).registerCompensation(activityClassName, activityInput);

    ctx.registerCompensation(activityClassName, activityInput);
    verify(saga, times(1)).registerCompensation(activityClassName, activityInput);
  }

  @Test
  public void test_compensate() {
    Saga saga = mock(Saga.class);
    WorkflowContext workflowContext = mock(WorkflowContext.class);
    DefaultSagaContext ctx = new DefaultSagaContext(saga, workflowContext);

    doNothing().when(saga).compensate(workflowContext);

    ctx.compensate();
    verify(saga, times(1)).compensate(workflowContext);
  }
}
