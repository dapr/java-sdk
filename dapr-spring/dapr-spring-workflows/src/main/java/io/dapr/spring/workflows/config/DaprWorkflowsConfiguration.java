package io.dapr.spring.workflows.config;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class DaprWorkflowsConfiguration implements ApplicationContextAware {
  private static final Logger LOGGER = LoggerFactory.getLogger(DaprWorkflowsConfiguration.class);

  private final WorkflowRuntimeBuilder workflowRuntimeBuilder;

  public DaprWorkflowsConfiguration(WorkflowRuntimeBuilder workflowRuntimeBuilder) {
    this.workflowRuntimeBuilder = workflowRuntimeBuilder;
  }

  /**
   * Register workflows and activities to the workflowRuntimeBuilder.
   * @param applicationContext Spring Application Context
   */
  private void registerWorkflowsAndActivities(ApplicationContext applicationContext) {
    LOGGER.info("Registering Dapr Workflows and Activities");

    Map<String, Workflow> workflowBeans = applicationContext.getBeansOfType(Workflow.class);

    for (Workflow workflow :  workflowBeans.values()) {
      LOGGER.info("Dapr Workflow: '{}' registered", workflow.getClass().getName());

      workflowRuntimeBuilder.registerWorkflow(workflow);
    }

    Map<String, WorkflowActivity> workflowActivitiesBeans = applicationContext.getBeansOfType(WorkflowActivity.class);

    for (WorkflowActivity activity :  workflowActivitiesBeans.values()) {
      LOGGER.info("Dapr Workflow Activity: '{}' registered", activity.getClass().getName());

      workflowRuntimeBuilder.registerActivity(activity);
    }

    try (WorkflowRuntime runtime = workflowRuntimeBuilder.build()) {
      LOGGER.info("Starting workflow runtime ... ");
      runtime.start(false);
    }
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    registerWorkflowsAndActivities(applicationContext);
  }
}
