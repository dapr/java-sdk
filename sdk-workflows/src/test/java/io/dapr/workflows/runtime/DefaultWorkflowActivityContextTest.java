package io.dapr.workflows.runtime;

import io.dapr.durabletask.TaskActivityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultWorkflowActivityContextTest {

  private DefaultWorkflowActivityContext context;

  @BeforeEach
  void setUp() {
    TaskActivityContext mockInnerContext = mock(TaskActivityContext.class);
    context = new DefaultWorkflowActivityContext(mockInnerContext);

    when(mockInnerContext.getName()).thenReturn("TestActivity");
    when(mockInnerContext.getInput(any())).thenReturn("TestInput");
    when(mockInnerContext.getTaskExecutionId()).thenReturn("TestExecutionId");
  }

  @Test
  void getLogger() {
    assertNotNull(context.getLogger());
  }

  @Test
  void getName() {
    assertEquals("TestActivity", context.getName());
  }

  @Test
  void getInput() {
    String input = context.getInput(String.class);
    assertEquals("TestInput", input);
  }

  @Test
  void getTaskExecutionId() {
    assertEquals("TestExecutionId", context.getTaskExecutionId());
  }
}
