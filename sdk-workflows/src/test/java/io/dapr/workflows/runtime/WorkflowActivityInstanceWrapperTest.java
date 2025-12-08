package io.dapr.workflows.runtime;

import io.dapr.durabletask.TaskActivityContext;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorkflowActivityInstanceWrapperTest {
  public static class TestActivity implements WorkflowActivity {
    @Override
    public Object run(WorkflowActivityContext ctx) {
      String activityContextName = ctx.getName();
      return ctx.getInput(String.class) + " world! from " + activityContextName;
    }
  }

  @Test
  public void getName() {
    WorkflowActivityInstanceWrapper<TestActivity> wrapper = new WorkflowActivityInstanceWrapper<>(new TestActivity());

    assertEquals(
        "io.dapr.workflows.runtime.WorkflowActivityInstanceWrapperTest.TestActivity",
        wrapper.getName()
    );
  }

  @Test
  public void createWithInstance() {
    TaskActivityContext mockContext = mock(TaskActivityContext.class);
    WorkflowActivityInstanceWrapper<TestActivity> wrapper = new WorkflowActivityInstanceWrapper<>(new TestActivity());

    when(mockContext.getInput(String.class)).thenReturn("Hello");
    when(mockContext.getName()).thenReturn("TestActivityContext");

    Object result = wrapper.create().run(mockContext);

    verify(mockContext, times(1)).getInput(String.class);
    assertEquals("Hello world! from TestActivityContext", result);
  }
}
