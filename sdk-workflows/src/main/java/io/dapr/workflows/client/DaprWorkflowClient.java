/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.workflows.client;

import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.DurableTaskGrpcClientBuilder;
import io.dapr.utils.NetworkUtils;
import io.dapr.workflows.runtime.Workflow;
import io.grpc.ManagedChannel;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

public class DaprWorkflowClient implements AutoCloseable {

  private final DurableTaskClient innerClient;
  private final ManagedChannel grpcChannel;

  /**
   * Public constructor for DaprWorkflowClient. This layer constructs the GRPC Channel.
   */
  public DaprWorkflowClient() {
    this(NetworkUtils.buildGrpcManagedChannel());
  }

  /**
   * Private Constructor that passes a created DurableTaskClient and the new GRPC channel.
   *
   * @param grpcChannel ManagedChannel for GRPC channel.
   */
  private DaprWorkflowClient(ManagedChannel grpcChannel) {
    this(createDurableTaskClient(grpcChannel), grpcChannel);
  }

  /**
   * Private Constructor for DaprWorkflowClient.
   *
   * @param innerClient DurableTaskGrpcClient with GRPC Channel set up.
   * @param grpcChannel ManagedChannel for instance variable setting.
   *
   */
  private DaprWorkflowClient(DurableTaskClient innerClient, ManagedChannel grpcChannel) {
    this.innerClient = innerClient;
    this.grpcChannel = grpcChannel;
  }

  /**
   * Static method to create the DurableTaskClient.
   *
   * @param grpcChannel ManagedChannel for GRPC.
   * @return a new instance of a DurableTaskClient with a GRPC channel.
   */
  private static DurableTaskClient createDurableTaskClient(ManagedChannel grpcChannel) {
    return new DurableTaskGrpcClientBuilder()
        .grpcChannel(grpcChannel)
        .build();
  }

  /**
   * Schedules a new workflow using DurableTask client.
   *
   * @param <T> any Workflow type
   * @param clazz Class extending Workflow to start an instance of.
   * @return A Mono Plan of type String with the randomly-generated instance ID for new Workflow instance.
   */
  public <T extends Workflow> Mono<String> scheduleNewWorkflow(Class<T> clazz) {
    return Mono.create(it -> {
      String response = this.innerClient.scheduleNewOrchestrationInstance(clazz.getCanonicalName());
      it.success(response);
    });
  }

  /**
   * Schedules a new workflow using DurableTask client.
   *
   * @param <T> any Workflow type
   * @param clazz Class extending Workflow to start an instance of.
   * @param input the input to pass to the scheduled orchestration instance. Must be serializable.
   * @return A Mono Plan of type String with the randomly-generated instance ID for new Workflow instance.
   */
  public <T extends Workflow> Mono<String> scheduleNewWorkflow(Class<T> clazz, Object input) {
    return Mono.create(it -> {
      String response = this.innerClient.scheduleNewOrchestrationInstance(clazz.getCanonicalName(), input);
      it.success(response);
    });
  }

  /**
   * Schedules a new workflow using DurableTask client.
   *
   * @param <T> any Workflow type
   * @param clazz Class extending Workflow to start an instance of.
   * @param input the input to pass to the scheduled orchestration instance. Must be serializable.
   * @param instanceId the unique ID of the orchestration instance to schedule
   * @return A Mono Plan of type String with the <code>instanceId</code> parameter value.
   */
  public <T extends Workflow> Mono<String> scheduleNewWorkflow(Class<T> clazz, Object input, String instanceId) {
    return Mono.create(it -> {
      String response = this.innerClient.scheduleNewOrchestrationInstance(clazz.getCanonicalName(), input, instanceId);
      it.success(response);
    });
  }

  /**
   * Terminates the workflow associated with the provided instance id.
   *
   * @param workflowInstanceId Workflow instance id to terminate.
   * @param output the optional output to set for the terminated orchestration instance.
   * @return A Mono Plan of type Void.
   */
  public Mono<Void> terminateWorkflow(String workflowInstanceId, @Nullable Object output) {
    return Mono.create(it -> {
      this.innerClient.terminate(workflowInstanceId, output);
      it.success();
    }).then();
  }

  /**
   * Closes the inner DurableTask client and shutdown the GRPC channel.
   *
   */
  public void close() throws InterruptedException {
    if (this.innerClient != null) {
      this.innerClient.close();
    }
    if (this.grpcChannel != null && !this.grpcChannel.isShutdown()) {
      this.grpcChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
