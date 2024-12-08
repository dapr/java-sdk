package io.dapr.workflows;

public interface WorkflowActivityContext {
  String getName();

  <T> T getInput(Class<T> targetType);
}
