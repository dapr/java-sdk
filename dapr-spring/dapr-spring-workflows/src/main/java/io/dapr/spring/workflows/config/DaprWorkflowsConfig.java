package io.dapr.spring.workflows.config;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.runtime.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;

import java.util.Map;

@Configuration
@ComponentScan("io.dapr.spring.workflows.config")
public class DaprWorkflowsConfig implements ApplicationContextAware {
  private static final Logger LOGGER = LoggerFactory.getLogger(DaprWorkflowsConfig.class);
  @Autowired
  private WorkflowRuntimeBuilder workflowRuntimeBuilder;

  private static ApplicationContext context;

  /**
   * Register workflows and activities to the workflowRuntimeBuilder.
   */
  public void registerWorkflowsAndActivities() {
    LOGGER.info("Registering Dapr Workflows and Activities");
    Map<String, Workflow> workflowBeans = context.getBeansOfType(Workflow.class);
    for (Workflow w :  workflowBeans.values()) {
      LOGGER.info("Dapr Workflow: '{}' registered", w.getClass().getName());
      workflowRuntimeBuilder.registerWorkflow(w.getClass());
    }

    Map<String, WorkflowActivity> workflowActivitiesBeans = context.getBeansOfType(WorkflowActivity.class);
    for (WorkflowActivity a :  workflowActivitiesBeans.values()) {
      LOGGER.info("Dapr Workflow Activity: '{}' registered", a.getClass().getName());
      workflowRuntimeBuilder.registerActivity(a.getClass());
    }

    try (WorkflowRuntime runtime = workflowRuntimeBuilder.build()) {
      LOGGER.info("Starting workflow runtime ... ");
      runtime.start(false);
    }
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    context = applicationContext;
    registerWorkflowsAndActivities();
  }
}
