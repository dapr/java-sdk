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

package io.dapr.durabletask.runner;

import com.google.protobuf.StringValue;
import io.dapr.durabletask.TaskOrchestrationExecutor;
import io.dapr.durabletask.TaskOrchestratorResult;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.trace.Tracer;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

public class OrchestratorRunner extends DurableRunner {
  private static final Logger logger = Logger.getLogger(OrchestratorRunner.class.getPackage().getName());

  private final OrchestratorService.OrchestratorRequest orchestratorRequest;
  private final TaskOrchestrationExecutor taskOrchestrationExecutor;

  /**
   * Constructs a new instance of the OrchestratorRunner class.
   *
   * @param workItem                  The work item containing details about the orchestrator task to be executed.
   * @param taskOrchestrationExecutor The executor responsible for running task orchestration logic.
   * @param sidecarClient             The gRPC stub for communication with the Task Hub sidecar service.
   * @param tracer                    An optional tracer used for distributed tracing, can be null.
   */
  public OrchestratorRunner(
      OrchestratorService.WorkItem workItem,
      TaskOrchestrationExecutor taskOrchestrationExecutor,
      TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub sidecarClient,
      @Nullable Tracer tracer) {

    super(workItem, sidecarClient, tracer);
    this.orchestratorRequest = workItem.getOrchestratorRequest();
    this.taskOrchestrationExecutor = taskOrchestrationExecutor;
  }

  @Override
  public void run() {
    TaskOrchestratorResult taskOrchestratorResult = taskOrchestrationExecutor.execute(
        orchestratorRequest.getPastEventsList(),
        orchestratorRequest.getNewEventsList());

    var versionBuilder = OrchestratorService.OrchestrationVersion.newBuilder();

    if (StringUtils.isNotEmpty(taskOrchestratorResult.getVersion())) {
      versionBuilder.setName(taskOrchestratorResult.getVersion());
    }

    if (taskOrchestratorResult.getPatches() != null) {
      versionBuilder.addAllPatches(taskOrchestratorResult.getPatches());
    }

    OrchestratorService.OrchestratorResponse response = OrchestratorService.OrchestratorResponse.newBuilder()
        .setInstanceId(orchestratorRequest.getInstanceId())
        .addAllActions(taskOrchestratorResult.getActions())
        .setCustomStatus(StringValue.of(taskOrchestratorResult.getCustomStatus()))
        .setCompletionToken(workItem.getCompletionToken())
        .setVersion(versionBuilder)
        .build();

    try {
      this.sidecarClient.completeOrchestratorTask(response);
      logger.log(Level.FINEST,
          "Completed orchestrator request for instance: {0}",
          orchestratorRequest.getInstanceId());
    } catch (StatusRuntimeException e) {
      this.logException(e);
    }
  }
}
