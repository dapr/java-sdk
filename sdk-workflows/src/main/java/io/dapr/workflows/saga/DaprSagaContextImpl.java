package io.dapr.workflows.saga;

import io.dapr.workflows.WorkflowContext;

/**
 * Dapr Saga Context implementation.
 */
public class DaprSagaContextImpl implements SagaContext {

  private final Saga saga;
  private final WorkflowContext workflowContext;

  /**
   * Constructor to build up instance.
   * 
   * @param saga Saga instance.
   * @param workflowContext Workflow context.
   * @throws IllegalArgumentException if saga or workflowContext is null.
   */
  public DaprSagaContextImpl(Saga saga, WorkflowContext workflowContext) {
    if (saga == null) {
      throw new IllegalArgumentException("Saga should not be null");
    }
    if (workflowContext == null) {
      throw new IllegalArgumentException("workflowContext should not be null");
    }

    this.saga = saga;
    this.workflowContext = workflowContext;
  }

  @Override
  public void registerCompensation(String activityClassName, Object activityInput) {
    this.saga.registerCompensation(activityClassName, activityInput);
  }

  @Override
  public void compensate() {
    this.saga.compensate(workflowContext);
  }
}
