/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.spring.workflows.config;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.annotations.ActivityDefinition;
import io.dapr.workflows.annotations.WorkflowDefinition;
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

      // Get the workflowDefinition annotation from the workflow class and validate it
      // If the annotation is not present, register the instance
      // If preset register with the workflowDefinition annotation values
      WorkflowDefinition workflowDefinition = workflow.getClass().getAnnotation(WorkflowDefinition.class);

      if (workflowDefinition == null) {
        // No annotation present, register the instance with default behavior
        LOGGER.info("Dapr Workflow: '{}' registered", workflow.getClass().getName());
        workflowRuntimeBuilder.registerWorkflow(workflow);
        continue;
      }

      // Register with annotation values
      String workflowName = workflowDefinition.name();
      String workflowVersion = workflowDefinition.version();
      boolean isLatest = workflowDefinition.isLatest();

      workflowRuntimeBuilder.registerWorkflow(workflowName, workflow, workflowVersion, isLatest);
    }

    Map<String, WorkflowActivity> workflowActivitiesBeans = applicationContext.getBeansOfType(WorkflowActivity.class);

    for (WorkflowActivity activity :  workflowActivitiesBeans.values()) {
      LOGGER.info("Dapr Workflow Activity: '{}' registered", activity.getClass().getName());
      ActivityDefinition activityDefinition = activity.getClass().getAnnotation(ActivityDefinition.class);
      if (activityDefinition == null) {
        workflowRuntimeBuilder.registerActivity(activity);
        continue;
      }

      workflowRuntimeBuilder.registerActivity(activityDefinition.name(), activity);
    }

    WorkflowRuntime runtime = workflowRuntimeBuilder.build();
    LOGGER.info("Starting workflow runtime ... ");
    runtime.start(false);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    registerWorkflowsAndActivities(applicationContext);
  }
}
