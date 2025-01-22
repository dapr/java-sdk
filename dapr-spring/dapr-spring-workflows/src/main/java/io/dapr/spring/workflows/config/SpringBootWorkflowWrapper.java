package io.dapr.spring.workflows.config;

import com.microsoft.durabletask.TaskOrchestration;
import com.microsoft.durabletask.TaskOrchestrationFactory;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.runtime.DefaultWorkflowContext;
import io.dapr.workflows.saga.Saga;

import java.lang.reflect.InvocationTargetException;

public class SpringBootWorkflowWrapper<T extends Workflow> implements TaskOrchestrationFactory {

  private T workflowBean;

  public SpringBootWorkflowWrapper(T workflowBean) {
    this.workflowBean = workflowBean;
  }

  @Override
  public String getName() {
    return this.workflowBean.getClass().getCanonicalName();
  }

  @Override
  public TaskOrchestration create() {
    return ctx -> {
      if (workflowBean.getSagaOption() != null) {
        Saga saga = new Saga(workflowBean.getSagaOption());
        workflowBean.run(new DefaultWorkflowContext(ctx, saga));
      } else {
        workflowBean.run(new DefaultWorkflowContext(ctx));
      }
    };
  }
}
