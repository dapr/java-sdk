/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.springboot.examples.workertwo;

import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkflowController {

  public static final String FULL_VERSION_WORKFLOW = "FullVersionWorkflow";
  private final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  /**
   * Track customer endpoint.
   *
   * @return confirmation that the workflow instance was created for a given customer
   */
  @PostMapping("/version/full")
  public String createWorkflowInstance() {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(FULL_VERSION_WORKFLOW);
    logger.info("Workflow instance {} started", instanceId);
    return "New Workflow Instance created with instanceId: " + instanceId;
  }

  /**
   * Request customer follow-up.
   *
   * @return confirmation that the follow-up was requested
   */
  @PostMapping("/version/full/followup/{instanceId}")
  public String followUp(@PathVariable("instanceId") String instanceId) {
    logger.info("Follow-up requested: {}", instanceId);
    if (instanceId == null || instanceId.isEmpty()) {
      return "InstanceId is empty";
    }
    daprWorkflowClient.raiseEvent(instanceId, "followup", null);
    return "Follow-up requested for instanceId: " + instanceId;
  }

  /**
   * Request customer workflow instance status.
   *
   * @return the workflow instance status for a given customer
   */
  @PostMapping("/version/full/status/{instanceId}")
  public String getStatus(@PathVariable("instanceId") String instanceId) {
    logger.info("Status requested: {}", instanceId);

    if (instanceId == null || instanceId.isEmpty()) {
      return "N/A";
    }

    WorkflowState instanceState = daprWorkflowClient.getWorkflowState(instanceId, true);
    assert instanceState != null;
    var result = instanceState.readOutputAs(String.class);
    return "Workflow for instanceId: " + instanceId + " is completed[" + instanceState.getRuntimeStatus() + "] with " + result;
  }
}

