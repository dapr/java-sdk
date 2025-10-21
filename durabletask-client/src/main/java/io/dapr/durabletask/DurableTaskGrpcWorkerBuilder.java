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

import io.grpc.Channel;

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

/**
 * Builder object for constructing customized {@link DurableTaskGrpcWorker} instances.
 *
 */
public final class DurableTaskGrpcWorkerBuilder {
  final HashMap<String, TaskOrchestrationFactory> orchestrationFactories = new HashMap<>();
  final HashMap<String, TaskActivityFactory> activityFactories = new HashMap<>();
  int port;
  Channel channel;
  DataConverter dataConverter;
  Duration maximumTimerInterval;
  ExecutorService executorService;
  String appId; // App ID for cross-app routing

  /**
   * Adds an orchestration factory to be used by the constructed {@link DurableTaskGrpcWorker}.
   *
   * @param factory an orchestration factory to be used by the constructed {@link DurableTaskGrpcWorker}
   * @return this builder object
   */
  public DurableTaskGrpcWorkerBuilder addOrchestration(TaskOrchestrationFactory factory) {
    String key = factory.getName();
    if (key == null || key.length() == 0) {
      throw new IllegalArgumentException("A non-empty task orchestration name is required.");
    }

    if (this.orchestrationFactories.containsKey(key)) {
      throw new IllegalArgumentException(
          String.format("A task orchestration factory named %s is already registered.", key));
    }

    this.orchestrationFactories.put(key, factory);
    return this;
  }

  /**
   * Adds an activity factory to be used by the constructed {@link DurableTaskGrpcWorker}.
   *
   * @param factory an activity factory to be used by the constructed {@link DurableTaskGrpcWorker}
   * @return this builder object
   */
  public DurableTaskGrpcWorkerBuilder addActivity(TaskActivityFactory factory) {
    // TODO: Input validation
    String key = factory.getName();
    if (key == null || key.length() == 0) {
      throw new IllegalArgumentException("A non-empty task activity name is required.");
    }

    if (this.activityFactories.containsKey(key)) {
      throw new IllegalArgumentException(
          String.format("A task activity factory named %s is already registered.", key));
    }

    this.activityFactories.put(key, factory);
    return this;
  }

  /**
   * Sets the gRPC channel to use for communicating with the sidecar process.
   *
   * <p>This builder method allows you to provide your own gRPC channel for communicating with the Durable Task sidecar
   * endpoint. Channels provided using this method won't be closed when the worker is closed.
   * Rather, the caller remains responsible for shutting down the channel after disposing the worker.</p>
   *
   * <p>If not specified, a gRPC channel will be created automatically for each constructed
   * {@link DurableTaskGrpcWorker}.</p>
   *
   * @param channel the gRPC channel to use
   * @return this builder object
   */
  public DurableTaskGrpcWorkerBuilder grpcChannel(Channel channel) {
    this.channel = channel;
    return this;
  }

  /**
   * Sets the gRPC endpoint port to connect to. If not specified, the default Durable Task port number will be used.
   *
   * @param port the gRPC endpoint port to connect to
   * @return this builder object
   */
  public DurableTaskGrpcWorkerBuilder port(int port) {
    this.port = port;
    return this;
  }

  /**
   * Sets the {@link DataConverter} to use for converting serializable data payloads.
   *
   * @param dataConverter the {@link DataConverter} to use for converting serializable data payloads
   * @return this builder object
   */
  public DurableTaskGrpcWorkerBuilder dataConverter(DataConverter dataConverter) {
    this.dataConverter = dataConverter;
    return this;
  }

  /**
   * Sets the maximum timer interval. If not specified, the default maximum timer interval duration will be used.
   * The default maximum timer interval duration is 3 days.
   *
   * @param maximumTimerInterval the maximum timer interval
   * @return this builder object
   */
  public DurableTaskGrpcWorkerBuilder maximumTimerInterval(Duration maximumTimerInterval) {
    this.maximumTimerInterval = maximumTimerInterval;
    return this;
  }

  /**
   * Sets the executor service that will be used to execute threads.
   *
   * @param executorService {@link ExecutorService}.
   * @return this builder object.
   */
  public DurableTaskGrpcWorkerBuilder withExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
    return this;
  }

  /**
   * Sets the app ID for cross-app workflow routing.
   *
   * <p>This app ID is used to identify this worker in cross-app routing scenarios.
   * It should match the app ID configured in the Dapr sidecar.</p>
   *
   * @param appId the app ID for this worker
   * @return this builder object
   */
  public DurableTaskGrpcWorkerBuilder appId(String appId) {
    this.appId = appId;
    return this;
  }

  /**
   * Initializes a new {@link DurableTaskGrpcWorker} object with the settings specified in the current builder object.
   *
   * @return a new {@link DurableTaskGrpcWorker} object
   */
  public DurableTaskGrpcWorker build() {
    return new DurableTaskGrpcWorker(this);
  }
}
