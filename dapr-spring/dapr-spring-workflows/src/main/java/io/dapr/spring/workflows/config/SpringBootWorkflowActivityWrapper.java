package io.dapr.spring.workflows.config;

import com.microsoft.durabletask.TaskActivity;
import com.microsoft.durabletask.TaskActivityFactory;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.runtime.DefaultWorkflowActivityContext;

/**
 * SpringBootWorkflowActivityWrapper wraps WorkflowActivities as TaskActivityFactory for Durable Tasks framework.
 *
 * @param <T> activity class to wrap.
 */
public class SpringBootWorkflowActivityWrapper<T extends WorkflowActivity> implements TaskActivityFactory {

  private T activityBean;

  /**
   * Constructor for SpringBootWorkflowActivityWrapper.
   *
   * @param activityBean that will be used by Durable Task Framework.
   */
  public SpringBootWorkflowActivityWrapper(T activityBean) {
    this.activityBean = activityBean;
  }

  @Override
  public String getName() {
    return this.activityBean.getClass().getCanonicalName();
  }

  @Override
  public TaskActivity create() {
    return ctx -> {
      return activityBean.run(new DefaultWorkflowActivityContext(ctx));
    };
  }
}
