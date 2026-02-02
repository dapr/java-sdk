package io.dapr.workflows.runtime;

import io.dapr.durabletask.TaskActivityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    when(mockInnerContext.getTraceParent()).thenReturn("00244654132154564654");

    assertNotNull(context.getLogger());
    assertEquals("TestActivity", context.getName());

    String input = context.getInput(String.class);

    assertEquals("TestInput", input);
    assertEquals("TestExecutionId", context.getTaskExecutionId());
    assertEquals("00244654132154564654", context.getTraceParent());
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when context parameter is null")
  void shouldThrowIllegalArgumentExceptionWhenContextParameterIsNull() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      new DefaultWorkflowActivityContext(null, TaskActivityContext.class);
    });
    assertEquals("Context cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when logger parameter is null")
  void shouldThrowIllegalArgumentExceptionWhenLoggerParameterIsNull() {
    TaskActivityContext mockInnerContext = mock(TaskActivityContext.class);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      new DefaultWorkflowActivityContext(mockInnerContext, (Logger) null);
    });
    assertEquals("Logger cannot be null", exception.getMessage());
  }
}
