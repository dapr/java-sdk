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

import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.trace.Tracer;

import javax.annotation.Nullable;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class DurableRunner implements Runnable {
  private static final Logger logger = Logger.getLogger(DurableRunner.class.getPackage().getName());
  public final TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub sidecarClient;
  public final OrchestratorService.WorkItem workItem;
  @Nullable
  public final Tracer tracer;

  /**
   * Constructs a new instance of the DurableRunner.
   *
   * @param workItem      the work item to be executed
   * @param sidecarClient the sidecar client used to communicate with the durable task sidecar
   * @param tracer        the tracer used for tracing operations; can be null if tracing is not required
   */
  public DurableRunner(OrchestratorService.WorkItem workItem,
                       TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub sidecarClient,
                       @Nullable Tracer tracer) {
    this.workItem = workItem;
    this.sidecarClient = sidecarClient;
    this.tracer = tracer;
  }

  protected String getSidecarAddress() {
    return this.sidecarClient.getChannel().authority();
  }

  protected void logException(StatusRuntimeException e) {
    if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
      logger.log(Level.WARNING,
          "The sidecar at address {0} is unavailable while completing the activity task.",
          this.sidecarClient.getChannel().authority());
    } else if (e.getStatus().getCode() == Status.Code.CANCELLED) {
      logger.log(Level.WARNING,
          "Durable Task worker has disconnected from {0} while completing the activity task.",
          this.sidecarClient.getChannel().authority());
    } else {
      logger.log(Level.WARNING, "Unexpected failure completing the activity task at {0}.",
          this.sidecarClient.getChannel().authority());
    }
  }
}
