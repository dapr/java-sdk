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

package io.dapr.durabletask;

import com.google.protobuf.StringValue;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService.TaskFailureDetails;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.dapr.durabletask.orchestration.TaskOrchestrationFactories;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Task hub worker that connects to a sidecar process over gRPC to execute
 * orchestrator and activity events.
 */
public final class DurableTaskGrpcWorker implements AutoCloseable {

  private static final int DEFAULT_PORT = 4001;
  private static final Logger logger = Logger.getLogger(DurableTaskGrpcWorker.class.getPackage().getName());
  private static final Duration DEFAULT_MAXIMUM_TIMER_INTERVAL = Duration.ofDays(3);

  private final TaskOrchestrationFactories orchestrationFactories;

  private final HashMap<String, TaskActivityFactory> activityFactories = new HashMap<>();

  private final ManagedChannel managedSidecarChannel;
  private final DataConverter dataConverter;
  private final Duration maximumTimerInterval;
  private final ExecutorService workerPool;
  private final String appId; // App ID for cross-app routing

  private final TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub sidecarClient;
  private final boolean isExecutorServiceManaged;
  private volatile boolean isNormalShutdown = false;
  private Thread workerThread;

  DurableTaskGrpcWorker(DurableTaskGrpcWorkerBuilder builder) {
    this.orchestrationFactories = builder.orchestrationFactories;
    this.activityFactories.putAll(builder.activityFactories);
    this.appId = builder.appId;

    Channel sidecarGrpcChannel;
    if (builder.channel != null) {
      // The caller is responsible for managing the channel lifetime
      this.managedSidecarChannel = null;
      sidecarGrpcChannel = builder.channel;
    } else {
      // Construct our own channel using localhost + a port number
      int port = DEFAULT_PORT;
      if (builder.port > 0) {
        port = builder.port;
      }

      // Need to keep track of this channel so we can dispose it on close()
      this.managedSidecarChannel = ManagedChannelBuilder
          .forAddress("localhost", port)
          .usePlaintext()
          .build();
      sidecarGrpcChannel = this.managedSidecarChannel;
    }

    this.sidecarClient = TaskHubSidecarServiceGrpc.newBlockingStub(sidecarGrpcChannel);
    this.dataConverter = builder.dataConverter != null ? builder.dataConverter : new JacksonDataConverter();
    this.maximumTimerInterval = builder.maximumTimerInterval != null ? builder.maximumTimerInterval
        : DEFAULT_MAXIMUM_TIMER_INTERVAL;
    this.workerPool = builder.executorService != null ? builder.executorService : Executors.newCachedThreadPool();
    this.isExecutorServiceManaged = builder.executorService == null;
  }

  /**
   * Establishes a gRPC connection to the sidecar and starts processing work-items
   * in the background.
   *
   * <p>This method retries continuously to establish a connection to the sidecar. If
   * a connection fails,
   * a warning log message will be written and a new connection attempt will be
   * made. This process
   * continues until either a connection succeeds or the process receives an
   * interrupt signal. </p>
   */
  public void start() {
    this.workerThread = new Thread(this::startAndBlock);
    this.workerThread.start();
  }

  /**
   * Closes the internally managed gRPC channel and executor service, if one
   * exists.
   *
   * <p>Only the internally managed GRPC Channel and Executor services are closed. If
   * any of them are supplied,
   * it is the responsibility of the supplier to take care of them.</p>
   *
   */
  public void close() {
    if (this.workerThread != null) {
      this.workerThread.interrupt();
    }
    this.isNormalShutdown = true;
    this.shutDownWorkerPool();
    this.closeSideCarChannel();
  }

  /**
   * Establishes a gRPC connection to the sidecar and starts processing work-items
   * on the current thread.
   * This method call blocks indefinitely, or until the current thread is
   * interrupted.
   *
   * <p>Use can alternatively use the {@link #start} method to run orchestration
   * processing in a background thread.</p>
   *
   * <p>This method retries continuously to establish a connection to the sidecar. If
   * a connection fails,
   * a warning log message will be written and a new connection attempt will be
   * made. This process
   * continues until either a connection succeeds or the process receives an
   * interrupt signal.</p>
   */
  public void startAndBlock() {
    logger.log(Level.INFO, "Durable Task worker is connecting to sidecar at {0}.", this.getSidecarAddress());

    TaskOrchestrationExecutor taskOrchestrationExecutor = new TaskOrchestrationExecutor(
        this.orchestrationFactories,
        this.dataConverter,
        this.maximumTimerInterval,
        logger,
        this.appId);
    TaskActivityExecutor taskActivityExecutor = new TaskActivityExecutor(
        this.activityFactories,
        this.dataConverter,
        logger);

    while (true) {
      try {
        OrchestratorService.GetWorkItemsRequest getWorkItemsRequest = OrchestratorService.GetWorkItemsRequest
            .newBuilder().build();
        Iterator<OrchestratorService.WorkItem> workItemStream = this.sidecarClient.getWorkItems(getWorkItemsRequest);
        while (workItemStream.hasNext()) {
          OrchestratorService.WorkItem workItem = workItemStream.next();
          OrchestratorService.WorkItem.RequestCase requestType = workItem.getRequestCase();
          if (requestType == OrchestratorService.WorkItem.RequestCase.ORCHESTRATORREQUEST) {
            OrchestratorService.OrchestratorRequest orchestratorRequest = workItem.getOrchestratorRequest();

            logger.log(Level.FINEST,
                String.format("Processing orchestrator request for instance: {0}",
                    orchestratorRequest.getInstanceId()));

            // TODO: Error handling
            this.workerPool.submit(() -> {
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
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                  logger.log(Level.WARNING,
                      "The sidecar at address {0} is unavailable while completing the orchestrator task.",
                      this.getSidecarAddress());
                } else if (e.getStatus().getCode() == Status.Code.CANCELLED) {
                  logger.log(Level.WARNING,
                      "Durable Task worker has disconnected from {0} while completing the orchestrator task.",
                      this.getSidecarAddress());
                } else {
                  logger.log(Level.WARNING,
                      "Unexpected failure completing the orchestrator task at {0}.",
                      this.getSidecarAddress());
                }
              }
            });
          } else if (requestType == OrchestratorService.WorkItem.RequestCase.ACTIVITYREQUEST) {
            OrchestratorService.ActivityRequest activityRequest = workItem.getActivityRequest();
            logger.log(Level.FINEST,
                String.format("Processing activity request: %s for instance: %s}",
                    activityRequest.getName(),
                    activityRequest.getOrchestrationInstance().getInstanceId()));

            // TODO: Error handling
            this.workerPool.submit(() -> {
              String output = null;
              TaskFailureDetails failureDetails = null;
              try {
                output = taskActivityExecutor.execute(
                    activityRequest.getName(),
                    activityRequest.getInput().getValue(),
                    activityRequest.getTaskExecutionId(),
                    activityRequest.getTaskId());
              } catch (Throwable e) {
                failureDetails = TaskFailureDetails.newBuilder()
                    .setErrorType(e.getClass().getName())
                    .setErrorMessage(e.getMessage())
                    .setStackTrace(StringValue.of(FailureDetails.getFullStackTrace(e)))
                    .build();
              }

              OrchestratorService.ActivityResponse.Builder responseBuilder = OrchestratorService.ActivityResponse
                  .newBuilder()
                  .setInstanceId(activityRequest.getOrchestrationInstance().getInstanceId())
                  .setTaskId(activityRequest.getTaskId())
                  .setCompletionToken(workItem.getCompletionToken());

              if (output != null) {
                responseBuilder.setResult(StringValue.of(output));
              }

              if (failureDetails != null) {
                responseBuilder.setFailureDetails(failureDetails);
              }

              try {
                this.sidecarClient.completeActivityTask(responseBuilder.build());
              } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                  logger.log(Level.WARNING,
                      "The sidecar at address {0} is unavailable while completing the activity task.",
                      this.getSidecarAddress());
                } else if (e.getStatus().getCode() == Status.Code.CANCELLED) {
                  logger.log(Level.WARNING,
                      "Durable Task worker has disconnected from {0} while completing the activity task.",
                      this.getSidecarAddress());
                } else {
                  logger.log(Level.WARNING, "Unexpected failure completing the activity task at {0}.",
                      this.getSidecarAddress());
                }
              }
            });
          } else if (requestType == OrchestratorService.WorkItem.RequestCase.HEALTHPING) {
            // No-op
          } else {
            logger.log(Level.WARNING,
                "Received and dropped an unknown '{0}' work-item from the sidecar.",
                requestType);
          }
        }
      } catch (StatusRuntimeException e) {
        if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
          logger.log(Level.INFO, "The sidecar at address {0} is unavailable. Will continue retrying.",
              this.getSidecarAddress());
        } else if (e.getStatus().getCode() == Status.Code.CANCELLED) {
          logger.log(Level.INFO, "Durable Task worker has disconnected from {0}.", this.getSidecarAddress());
        } else {
          logger.log(Level.WARNING,
              String.format("Unexpected failure connecting to %s", this.getSidecarAddress()), e);
        }

        // Retry after 5 seconds
        try {
          Thread.sleep(5000);
        } catch (InterruptedException ex) {
          break;
        }
      }
    }
  }

  /**
   * Stops the current worker's listen loop, preventing any new orchestrator or
   * activity events from being processed.
   */
  public void stop() {
    this.close();
  }

  private void closeSideCarChannel() {
    if (this.managedSidecarChannel != null) {
      try {
        this.managedSidecarChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        // Best effort. Also note that AutoClose documentation recommends NOT having
        // close() methods throw InterruptedException:
        // https://docs.oracle.com/javase/7/docs/api/java/lang/AutoCloseable.html
      }
    }
  }

  private void shutDownWorkerPool() {
    if (this.isExecutorServiceManaged) {
      if (!this.isNormalShutdown) {
        logger.log(Level.WARNING,
            "ExecutorService shutdown initiated unexpectedly. No new tasks will be accepted");
      }

      this.workerPool.shutdown();
      try {
        if (!this.workerPool.awaitTermination(60, TimeUnit.SECONDS)) {
          this.workerPool.shutdownNow();
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private String getSidecarAddress() {
    return this.sidecarClient.getChannel().authority();
  }
}
