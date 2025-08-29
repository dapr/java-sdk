package io.dapr.workflows.runtime;

import io.dapr.durabletask.TaskActivityContext;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class WorkflowActivityClassWrapperTest {
  public static class TestActivity implements WorkflowActivity {
    @Override
    public Object run(WorkflowActivityContext ctx) {
      String activityContextName = ctx.getName();
      return ctx.getInput(String.class) + " world! from " + activityContextName + " with task execution key " + ctx.getTaskExecutionId();
    }
  }

  @Test
  public void getName() {
    WorkflowActivityClassWrapper<TestActivity> wrapper = new WorkflowActivityClassWrapper<>(TestActivity.class);

    assertEquals(
        "io.dapr.workflows.runtime.WorkflowActivityClassWrapperTest.TestActivity",
        wrapper.getName()
    );
  }

  @Test
  public void createWithClass() {
    TaskActivityContext mockContext = mock(TaskActivityContext.class);
    WorkflowActivityClassWrapper<TestActivity> wrapper = new WorkflowActivityClassWrapper<>(TestActivity.class);

    when(mockContext.getInput(String.class)).thenReturn("Hello");
    when(mockContext.getName()).thenReturn("TestActivityContext");
    when(mockContext.getTaskExecutionId()).thenReturn("123");

    Object result = wrapper.create().run(mockContext);

    verify(mockContext, times(1)).getInput(String.class);
    assertEquals("Hello world! from TestActivityContext with task execution key 123", result);
  }
}
