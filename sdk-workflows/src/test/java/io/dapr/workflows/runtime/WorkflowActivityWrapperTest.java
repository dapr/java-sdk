package io.dapr.workflows.runtime;

import com.microsoft.durabletask.TaskActivityContext;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class WorkflowActivityWrapperTest {
  public static class TestActivity implements WorkflowActivity {
    @Override
    public Object run(WorkflowActivityContext ctx) {
      String activityContextName = ctx.getName();
      return ctx.getInput(String.class) + " world! from " + activityContextName;
    }
  }

  @Test
  public void getName() throws NoSuchMethodException {
    WorkflowActivityWrapper<TestActivity> wrapper = new WorkflowActivityWrapper<>(
        WorkflowActivityWrapperTest.TestActivity.class);
    Assert.assertEquals(
        "io.dapr.workflows.runtime.WorkflowActivityWrapperTest.TestActivity",
        wrapper.getName()
    );
  }

  @Test
  public void createWithClass() throws NoSuchMethodException {
    TaskActivityContext mockContext = mock(TaskActivityContext.class);
    WorkflowActivityWrapper<TestActivity> wrapper = new WorkflowActivityWrapper<>(
        WorkflowActivityWrapperTest.TestActivity.class);
    when(mockContext.getInput(String.class)).thenReturn("Hello");
    when(mockContext.getName()).thenReturn("TestActivityContext");
    Object result = wrapper.create().run(mockContext);
    verify(mockContext, times(1)).getInput(String.class);
    Assert.assertEquals("Hello world! from TestActivityContext", result);
  }
}
