package io.dapr.workflows.saga;

/**
 * Saga context.
 */
public interface SagaContext {
  /**
   * Register a compensation activity.
   * 
   * @param activityClassName name of the activity class
   * @param activityInput     input of the activity to be compensated
   */
  void registerCompensation(String activityClassName, Object activityInput);

  /**
   * Compensate all registered activities.
   * 
   */
  void compensate();

}
