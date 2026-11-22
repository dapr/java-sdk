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

package io.dapr.springboot.examples.simpleworkflow;

import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.dapr.workflows.client.WorkflowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableDaprWorkflows
public class OnboardingRestController {

  private final Logger logger = LoggerFactory.getLogger(OnboardingRestController.class);

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  /**
   * Start the onboarding workflow for a customer without waiting for it to finish.
   * @param customer to onboard
   * @return the id of the workflow instance, which is the customer id
   */
  @PostMapping("/onboarding")
  public String startOnboarding(@RequestBody Customer customer) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(CustomerOnboardingWorkflow.class, customer,
        customer.getId());
    logger.info("Workflow instance " + instanceId + " started");
    return instanceId;
  }

  /**
   * Get the status of an onboarding workflow instance.
   * @param instanceId id of the workflow instance
   * @return the runtime status of the instance, its result once completed or its error if it failed
   */
  @GetMapping("/onboarding/{instanceId}")
  public ResponseEntity<OnboardingStatus> getOnboarding(@PathVariable("instanceId") String instanceId) {
    WorkflowState state = daprWorkflowClient.getWorkflowState(instanceId, true);
    if (state == null) {
      return ResponseEntity.notFound().build();
    }
    OnboardingResult result = null;
    String error = null;
    if (state.getRuntimeStatus() == WorkflowRuntimeStatus.COMPLETED) {
      result = state.readOutputAs(OnboardingResult.class);
    } else if (state.getRuntimeStatus() == WorkflowRuntimeStatus.FAILED && state.getFailureDetails() != null) {
      error = state.getFailureDetails().getErrorMessage();
    }
    return ResponseEntity.ok(new OnboardingStatus(instanceId, state.getRuntimeStatus(), result, error));
  }

}
