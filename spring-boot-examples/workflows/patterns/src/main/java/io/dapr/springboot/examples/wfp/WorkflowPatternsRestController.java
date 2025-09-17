/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.springboot.examples.wfp;

import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import io.dapr.springboot.examples.wfp.chain.ChainWorkflow;
import io.dapr.springboot.examples.wfp.child.ParentWorkflow;
import io.dapr.springboot.examples.wfp.continueasnew.CleanUpLog;
import io.dapr.springboot.examples.wfp.continueasnew.ContinueAsNewWorkflow;
import io.dapr.springboot.examples.wfp.externalevent.Decision;
import io.dapr.springboot.examples.wfp.externalevent.ExternalEventWorkflow;
import io.dapr.springboot.examples.wfp.fanoutin.FanOutInWorkflow;
import io.dapr.springboot.examples.wfp.fanoutin.Result;
import io.dapr.springboot.examples.wfp.remoteendpoint.Payload;
import io.dapr.springboot.examples.wfp.remoteendpoint.RemoteEndpointWorkflow;
import io.dapr.springboot.examples.wfp.timer.DurationTimerWorkflow;
import io.dapr.springboot.examples.wfp.timer.ZonedDateTimeTimerWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@RestController
@EnableDaprWorkflows
public class WorkflowPatternsRestController {

  private final Logger logger = LoggerFactory.getLogger(WorkflowPatternsRestController.class);

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private CleanUpLog cleanUpLog;

  private Map<String, String> ordersToApprove = new HashMap<>();



  /**
   * Run Chain Demo Workflow
   * @return the output of the ChainWorkflow execution
   */
  @PostMapping("wfp/chain")
  public String chain() throws TimeoutException {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(ChainWorkflow.class);
    logger.info("Workflow instance " + instanceId + " started");
    return daprWorkflowClient
            .waitForInstanceCompletion(instanceId, Duration.ofSeconds(10), true)
            .readOutputAs(String.class);
  }


  /**
   * Run Child Demo Workflow
   * @return confirmation that the workflow instance was created for the workflow pattern child
   */
  @PostMapping("wfp/child")
  public String child() throws TimeoutException {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(ParentWorkflow.class);
    logger.info("Workflow instance " + instanceId + " started");
    return daprWorkflowClient
            .waitForInstanceCompletion(instanceId, Duration.ofSeconds(10), true)
            .readOutputAs(String.class);
  }


  /**
   * Run Fan Out/in Demo Workflow
   * @return confirmation that the workflow instance was created for the workflow pattern faninout
   */
  @PostMapping("wfp/fanoutin")
  public Result fanOutIn(@RequestBody List<String> listOfStrings) throws TimeoutException {

    String instanceId = daprWorkflowClient.scheduleNewWorkflow(FanOutInWorkflow.class, listOfStrings);
    logger.info("Workflow instance " + instanceId + " started");

    // Block until the orchestration completes. Then print the final status, which includes the output.
    WorkflowInstanceStatus workflowInstanceStatus = daprWorkflowClient.waitForInstanceCompletion(
            instanceId,
            Duration.ofSeconds(30),
            true);
    logger.info("workflow instance with ID: %s completed with result: %s%n", instanceId,
            workflowInstanceStatus.readOutputAs(Result.class));
    return workflowInstanceStatus.readOutputAs(Result.class);
  }

    /**
   * Run External Event Workflow Pattern
   * @return confirmation that the workflow instance was created for the workflow pattern externalevent
   */
  @PostMapping("wfp/externalevent")
  public String externalEvent(@RequestParam("orderId") String orderId) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(ExternalEventWorkflow.class);
    ordersToApprove.put(orderId, instanceId);
    logger.info("Workflow instance " + instanceId + " started");
    return instanceId;
  }

  @PostMapping("wfp/externalevent-continue")
  public Decision externalEventContinue(@RequestParam("orderId") String orderId, @RequestParam("decision") Boolean decision)
          throws TimeoutException {
    String instanceId = ordersToApprove.get(orderId);
    logger.info("Workflow instance " + instanceId + " continue");
    daprWorkflowClient.raiseEvent(instanceId, "Approval", decision);
    WorkflowInstanceStatus workflowInstanceStatus = daprWorkflowClient
            .waitForInstanceCompletion(instanceId, null, true);
    return workflowInstanceStatus.readOutputAs(Decision.class);
  }

  @PostMapping("wfp/continueasnew")
  public CleanUpLog continueAsNew()
          throws TimeoutException {

    cleanUpLog.clearLog();
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(ContinueAsNewWorkflow.class);
    logger.info("Workflow instance " + instanceId + " started");

    WorkflowInstanceStatus workflowInstanceStatus = daprWorkflowClient.waitForInstanceCompletion(instanceId, null, true);
    System.out.printf("workflow instance with ID: %s completed.", instanceId);
    return workflowInstanceStatus.readOutputAs(CleanUpLog.class);
  }

  @PostMapping("wfp/remote-endpoint")
  public Payload remoteEndpoint(@RequestBody Payload payload)
          throws TimeoutException {

    String instanceId = daprWorkflowClient.scheduleNewWorkflow(RemoteEndpointWorkflow.class, payload);
    logger.info("Workflow instance " + instanceId + " started");

    WorkflowInstanceStatus workflowInstanceStatus = daprWorkflowClient
            .waitForInstanceCompletion(instanceId, null, true);
    System.out.printf("workflow instance with ID: %s completed.", instanceId);
    return workflowInstanceStatus.readOutputAs(Payload.class);
  }

  @PostMapping("wfp/suspendresume")
  public String suspendResume(@RequestParam("orderId") String orderId) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(ExternalEventWorkflow.class);
    logger.info("Workflow instance " + instanceId + " started");
    ordersToApprove.put(orderId, instanceId);
    return instanceId;
  }

  @PostMapping("wfp/suspendresume/suspend")
  public String suspendResumeExecuteSuspend(@RequestParam("orderId") String orderId) {
    String instanceId = ordersToApprove.get(orderId);
    daprWorkflowClient.suspendWorkflow(instanceId, "testing suspend");
    WorkflowInstanceStatus instanceState = daprWorkflowClient.getInstanceState(instanceId, false);
    return instanceState.getRuntimeStatus().name();
  }

  @PostMapping("wfp/suspendresume/resume")
  public String suspendResumeExecuteResume(@RequestParam("orderId") String orderId) {
    String instanceId = ordersToApprove.get(orderId);
    daprWorkflowClient.resumeWorkflow(instanceId, "testing resume");
    WorkflowInstanceStatus instanceState = daprWorkflowClient.getInstanceState(instanceId, false);
    return instanceState.getRuntimeStatus().name();
  }


  @PostMapping("wfp/suspendresume/continue")
  public Decision suspendResumeContinue(@RequestParam("orderId") String orderId, @RequestParam("decision") Boolean decision)
          throws TimeoutException {
    String instanceId = ordersToApprove.get(orderId);
    logger.info("Workflow instance " + instanceId + " continue");
    daprWorkflowClient.raiseEvent(instanceId, "Approval", decision);
    WorkflowInstanceStatus workflowInstanceStatus = daprWorkflowClient
            .waitForInstanceCompletion(instanceId, null, true);
    return workflowInstanceStatus.readOutputAs(Decision.class);
  }

  @PostMapping("wfp/durationtimer")
  public String durationTimerWorkflow() {
    return daprWorkflowClient.scheduleNewWorkflow(DurationTimerWorkflow.class);
  }

  @PostMapping("wfp/zoneddatetimetimer")
  public String zonedDateTimeTimerWorkflow() {
    return daprWorkflowClient.scheduleNewWorkflow(ZonedDateTimeTimerWorkflow.class);
  }

}

