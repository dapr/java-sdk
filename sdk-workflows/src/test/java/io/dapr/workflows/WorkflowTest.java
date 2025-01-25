package io.dapr.workflows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import com.microsoft.durabletask.interruption.ContinueAsNewInterruption;
import com.microsoft.durabletask.interruption.OrchestratorBlockedException;

import io.dapr.workflows.saga.SagaCompensationException;
import io.dapr.workflows.saga.SagaContext;
import io.dapr.workflows.saga.SagaOptions;

public class WorkflowTest {

  @Test
  public void testWorkflow_WithoutSaga() {
    WorkflowStub stub = mock(WorkflowStub.class);
    Workflow workflow = new WorkflowWithoutSaga(stub);
    assertNull(workflow.getSagaOption());
    assertFalse(workflow.isSagaEnabled());

    WorkflowContext ctx = mock(WorkflowContext.class);
    doNothing().when(stub).run(ctx);
    workflow.run(ctx);

    verify(stub, times(1)).run(eq(ctx));
  }

  @Test
  public void testWorkflow_WithoutSaga_throwException() {
    WorkflowStub stub = mock(WorkflowStub.class);
    Workflow workflow = new WorkflowWithoutSaga(stub);
    WorkflowContext ctx = mock(WorkflowContext.class);
    Exception e = new RuntimeException();
    doThrow(e).when(stub).run(ctx);

    // should throw the exception, not catch
    assertThrows(RuntimeException.class, () -> {
      workflow.run(ctx);
    });
    verify(stub, times(1)).run(eq(ctx));
  }

  @Test
  public void testWorkflow_WithSaga() {
    WorkflowStub stub = mock(WorkflowStub.class);
    Workflow workflow = new WorkflowWithSaga(stub);
    assertNotNull(workflow.getSagaOption());
    assertTrue(workflow.isSagaEnabled());

    WorkflowContext ctx = mock(WorkflowContext.class);
    doNothing().when(stub).run(ctx);
    workflow.run(ctx);

    verify(stub, times(1)).run(eq(ctx));
  }

  @Test
  public void testWorkflow_WithSaga_shouldNotCatch_OrchestratorBlockedException() {
    WorkflowStub stub = mock(WorkflowStub.class);
    Workflow workflow = new WorkflowWithSaga(stub);

    WorkflowContext ctx = mock(WorkflowContext.class);
    Exception e = new OrchestratorBlockedException("test");
    doThrow(e).when(stub).run(ctx);

    // should not catch OrchestratorBlockedException
    assertThrows(OrchestratorBlockedException.class, () -> {
      workflow.run(ctx);
    });
    verify(stub, times(1)).run(eq(ctx));
  }

  @Test
  public void testWorkflow_WithSaga_shouldNotCatch_ContinueAsNewInterruption() {
    WorkflowStub stub = mock(WorkflowStub.class);
    Workflow workflow = new WorkflowWithSaga(stub);

    WorkflowContext ctx = mock(WorkflowContext.class);
    Exception e = new ContinueAsNewInterruption("test");
    doThrow(e).when(stub).run(ctx);

    // should not catch ContinueAsNewInterruption
    assertThrows(ContinueAsNewInterruption.class, () -> {
      workflow.run(ctx);
    });
    verify(stub, times(1)).run(eq(ctx));
  }

  @Test
  public void testWorkflow_WithSaga_shouldNotCatch_SagaCompensationException() {
    WorkflowStub stub = mock(WorkflowStub.class);
    Workflow workflow = new WorkflowWithSaga(stub);

    WorkflowContext ctx = mock(WorkflowContext.class);
    Exception e = new SagaCompensationException("test", null);
    doThrow(e).when(stub).run(ctx);

    // should not catch SagaCompensationException
    assertThrows(SagaCompensationException.class, () -> {
      workflow.run(ctx);
    });
    verify(stub, times(1)).run(eq(ctx));
  }

  @Test
  public void testWorkflow_WithSaga_triggerCompensate() {
    WorkflowStub stub = mock(WorkflowStub.class);
    Workflow workflow = new WorkflowWithSaga(stub);

    WorkflowContext ctx = mock(WorkflowContext.class);
    Exception e = new RuntimeException("test", null);
    doThrow(e).when(stub).run(ctx);
    SagaContext sagaContext = mock(SagaContext.class);
    doReturn(sagaContext).when(ctx).getSagaContext();
    doNothing().when(sagaContext).compensate();

    assertThrows(RuntimeException.class, () -> {
      workflow.run(ctx);
    });
    verify(stub, times(1)).run(eq(ctx));
    verify(sagaContext, times(1)).compensate();
  }

  @Test
  public void testWorkflow_WithSaga_compensateFaile() {
    WorkflowStub stub = mock(WorkflowStub.class);
    Workflow workflow = new WorkflowWithSaga(stub);

    WorkflowContext ctx = mock(WorkflowContext.class);
    Exception e = new RuntimeException("workflow fail", null);
    doThrow(e).when(stub).run(ctx);
    SagaContext sagaContext = mock(SagaContext.class);
    doReturn(sagaContext).when(ctx).getSagaContext();
    Exception e2 = new RuntimeException("compensate fail", null);
    doThrow(e2).when(sagaContext).compensate();

    try {
      workflow.run(ctx);
      fail("sholdd throw exception");
    } catch (Exception ex) {
      assertEquals(e2.getMessage(), ex.getMessage());
      assertEquals(1, ex.getSuppressed().length);
      assertEquals(e.getMessage(), ex.getSuppressed()[0].getMessage());
    }

    verify(stub, times(1)).run(eq(ctx));
    verify(sagaContext, times(1)).compensate();
  }

  public static class WorkflowWithoutSaga implements Workflow {
    private final WorkflowStub stub;

    public WorkflowWithoutSaga(WorkflowStub stub) {
      this.stub = stub;
    }

    @Override
    public WorkflowStub create() {
      return stub;
    }
  }

  public static class WorkflowWithSaga implements Workflow {
    private final WorkflowStub stub;

    public WorkflowWithSaga(WorkflowStub stub) {
      this.stub = stub;
    }

    @Override
    public WorkflowStub create() {
      return stub;
    }

    @Override
    public SagaOptions getSagaOption() {
      return SagaOptions.newBuilder()
          .setParallelCompensation(false)
          .build();
    }
  }
}
