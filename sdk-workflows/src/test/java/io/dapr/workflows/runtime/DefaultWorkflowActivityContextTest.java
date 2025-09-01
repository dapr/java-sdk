package io.dapr.workflows.runtime;

import io.dapr.durabletask.TaskActivityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultWorkflowActivityContextTest {

  @Test
  @DisplayName("Should successfully create context and return correct values for all methods")
  void shouldSuccessfullyCreateContextAndReturnCorrectValuesForAllMethods() {
    TaskActivityContext mockInnerContext = mock(TaskActivityContext.class);
    DefaultWorkflowActivityContext context = new DefaultWorkflowActivityContext(mockInnerContext);

    when(mockInnerContext.getName()).thenReturn("TestActivity");
    when(mockInnerContext.getInput(any())).thenReturn("TestInput");
    when(mockInnerContext.getTaskExecutionId()).thenReturn("TestExecutionId");

    assertNotNull(context.getLogger());
    assertEquals("TestActivity", context.getName());

    String input = context.getInput(String.class);

    assertEquals("TestInput", input);
    assertEquals("TestExecutionId", context.getTaskExecutionId());
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when context parameter is null")
  void shouldThrowIllegalArgumentExceptionWhenContextParameterIsNull() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      new DefaultWorkflowActivityContext(null);
    });
    assertEquals("Context cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when logger parameter is null")
  void shouldThrowIllegalArgumentExceptionWhenLoggerParameterIsNull() {
    TaskActivityContext mockInnerContext = mock(TaskActivityContext.class);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      new DefaultWorkflowActivityContext(mockInnerContext, null);
    });
    assertEquals("Logger cannot be null", exception.getMessage());
  }
}
