package io.dapr.workflows;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class WorkflowTest {

  @Test
  public void testWorkflow() {
    WorkflowStub stub = mock(WorkflowStub.class);
    Workflow workflow = new TestWorkflow(stub);

    WorkflowContext ctx = mock(WorkflowContext.class);
    doNothing().when(stub).run(ctx);
    workflow.run(ctx);

    verify(stub, times(1)).run(eq(ctx));
  }

  @Test
  public void testWorkflow_throwException() {
    WorkflowStub stub = mock(WorkflowStub.class);
    Workflow workflow = new TestWorkflow(stub);
    WorkflowContext ctx = mock(WorkflowContext.class);
    Exception e = new RuntimeException();
    doThrow(e).when(stub).run(ctx);

    // should throw the exception, not catch
    assertThrows(RuntimeException.class, () -> {
      workflow.run(ctx);
    });
    verify(stub, times(1)).run(eq(ctx));
  }

  public static class TestWorkflow implements Workflow {
    private final WorkflowStub stub;

    public TestWorkflow(WorkflowStub stub) {
      this.stub = stub;
    }

    @Override
    public WorkflowStub create() {
      return stub;
    }
  }
}
