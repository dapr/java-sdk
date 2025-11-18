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

import io.dapr.config.Properties;
import io.dapr.durabletask.DurableTaskClient;
import io.dapr.durabletask.DurableTaskGrpcClientBuilder;
import io.dapr.durabletask.NewOrchestrationInstanceOptions;
import io.dapr.durabletask.OrchestrationMetadata;
import io.dapr.durabletask.PurgeResult;
import io.dapr.utils.NetworkUtils;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.internal.ApiTokenClientInterceptor;
import io.dapr.workflows.internal.TraceParentClientInterceptor;
import io.dapr.workflows.runtime.DefaultWorkflowInstanceStatus;
import io.dapr.workflows.runtime.DefaultWorkflowState;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

import javax.annotation.Nullable;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Defines client operations for managing Dapr Workflow instances.
 */
public class DaprWorkflowClient implements AutoCloseable {

  private ClientInterceptor workflowApiTokenInterceptor;
  private DurableTaskClient innerClient;
  private ManagedChannel grpcChannel;
  private ObservationRegistry observationRegistry;
  private Tracer tracer;
  private Meter meter;

  /**
   * Public constructor for DaprWorkflowClient. This layer constructs the GRPC Channel.
   */
  public DaprWorkflowClient() {
    this(new Properties());
  }

  /**
   * Public constructor for DaprWorkflowClient. This layer constructs the GRPC Channel.
   *
   * @param properties Properties for the GRPC Channel.
   */
  public DaprWorkflowClient(Properties properties) {
    this(NetworkUtils.buildGrpcManagedChannel(properties, new ApiTokenClientInterceptor(properties)));
  }

  /**
   * Public constructor for DaprWorkflowClient. This layer constructs the GRPC Channel.
   *
   * @param properties Properties for the GRPC Channel.
   * @param observationRegistry micrometer observation registry.
   * @param tracer otel tracer for traces.
   * @param meter otel meter for metrics.
   */
  public DaprWorkflowClient(Properties properties, ObservationRegistry observationRegistry,
                            Tracer tracer, Meter meter) {
    this(NetworkUtils.buildGrpcManagedChannel(properties,
        new ApiTokenClientInterceptor(properties),
        new TraceParentClientInterceptor(tracer)));
    this.observationRegistry = observationRegistry;
    this.tracer = tracer;
    this.meter = meter;

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
   */
  private DaprWorkflowClient(DurableTaskClient innerClient, ManagedChannel grpcChannel) {
    this.innerClient = innerClient;
    this.grpcChannel = grpcChannel;
  }

  /**
   * Schedules a new workflow using DurableTask client.
   *
   * @param <T>   any Workflow type
   * @param clazz Class extending Workflow to start an instance of.
   * @return the randomly-generated instance ID for new Workflow instance.
   */
  public <T extends Workflow> String scheduleNewWorkflow(Class<T> clazz) {
    if (tracer == null) {
      return this.innerClient.scheduleNewOrchestrationInstance(clazz.getCanonicalName());
    }
    Span span = tracer.spanBuilder("dapr.workflow.scheduleNewWorkflow")
        .setAttribute("workflow", clazz.getName())
        .startSpan();
    try {
      return this.innerClient.scheduleNewOrchestrationInstance(clazz.getCanonicalName());
    } finally {
      span.end();
    }
  }

  /**
   * Schedules a new workflow using DurableTask client.
   *
   * @param <T>   any Workflow type
   * @param clazz Class extending Workflow to start an instance of.
   * @param input the input to pass to the scheduled orchestration instance. Must be serializable.
   * @return the randomly-generated instance ID for new Workflow instance.
   */
  public <T extends Workflow> String scheduleNewWorkflow(Class<T> clazz, Object input) {
    if (tracer == null) {
      return this.innerClient.scheduleNewOrchestrationInstance(clazz.getCanonicalName(), input);
    }
    Span span = tracer.spanBuilder("dapr.workflow.scheduleNewWorkflow")
        .setAttribute("workflow", clazz.getName())
        .setAttribute("input", input.toString())
        .startSpan();
    try {
      return this.innerClient.scheduleNewOrchestrationInstance(clazz.getCanonicalName(), input);
    } finally {
      span.end();
    }
  }

  /**
   * Schedules a new workflow using DurableTask client.
   *
   * @param <T>        any Workflow type
   * @param clazz      Class extending Workflow to start an instance of.
   * @param input      the input to pass to the scheduled orchestration instance. Must be serializable.
   * @param instanceId the unique ID of the orchestration instance to schedule
   * @return the <code>instanceId</code> parameter value.
   */
  public <T extends Workflow> String scheduleNewWorkflow(Class<T> clazz, Object input, String instanceId) {
    if (tracer == null) {
      return this.innerClient.scheduleNewOrchestrationInstance(clazz.getCanonicalName(), input, instanceId);
    }
    Span span = tracer.spanBuilder("dapr.workflow.scheduleNewWorkflow")
        .setAttribute("workflow", clazz.getName())
        .setAttribute("input", input.toString())
        .setAttribute("instanceId", instanceId)
        .startSpan();
    try {
      return this.innerClient.scheduleNewOrchestrationInstance(clazz.getCanonicalName(), input, instanceId);
    } finally {
      span.end();
    }
  }

  /**
   * Schedules a new workflow with a specified set of options for execution.
   *
   * @param <T>     any Workflow type
   * @param clazz   Class extending Workflow to start an instance of.
   * @param options the options for the new workflow, including input, instance ID, etc.
   * @return the <code>instanceId</code> parameter value.
   */
  public <T extends Workflow> String scheduleNewWorkflow(Class<T> clazz, NewWorkflowOptions options) {
    if (tracer == null) {
      return this.innerClient.scheduleNewOrchestrationInstance(clazz.getCanonicalName(), options);
    }

    Span span = tracer.spanBuilder("dapr.workflow.scheduleNewWorkflow")
        .setAttribute("workflow", clazz.getName())
        .setAttribute("options", options.toString())
        .startSpan();
    try {
      NewOrchestrationInstanceOptions orchestrationInstanceOptions = fromNewWorkflowOptions(options);

      return this.innerClient.scheduleNewOrchestrationInstance(clazz.getCanonicalName(),
          orchestrationInstanceOptions);
    } finally {
      span.end();
    }
  }

  /**
   * Suspend the workflow associated with the provided instance id.
   *
   * @param workflowInstanceId Workflow instance id to suspend.
   * @param reason             reason for suspending the workflow instance.
   */
  public void suspendWorkflow(String workflowInstanceId, @Nullable String reason) {
    if (tracer == null) {
      this.innerClient.suspendInstance(workflowInstanceId, reason);
    }
    Span span = tracer.spanBuilder("dapr.workflow.suspendWorkflow")
        .setAttribute("instanceId", workflowInstanceId)
        .setAttribute("reason", reason)
        .startSpan();
    try {
      this.innerClient.suspendInstance(workflowInstanceId, reason);
    } finally {
      span.end();
    }
  }

  /**
   * Resume the workflow associated with the provided instance id.
   *
   * @param workflowInstanceId Workflow instance id to resume.
   * @param reason             reason for resuming the workflow instance.
   */
  public void resumeWorkflow(String workflowInstanceId, @Nullable String reason) {
    if (tracer == null) {
      this.innerClient.suspendInstance(workflowInstanceId, reason);
    }
    Span span = tracer.spanBuilder("dapr.workflow.resumeWorkflow")
        .setAttribute("instanceId", workflowInstanceId)
        .setAttribute("reason", reason)
        .startSpan();
    try {
      this.innerClient.resumeInstance(workflowInstanceId, reason);
    } finally {
      span.end();
    }
  }

  /**
   * Terminates the workflow associated with the provided instance id.
   *
   * @param workflowInstanceId Workflow instance id to terminate.
   * @param output             the optional output to set for the terminated orchestration instance.
   */
  public void terminateWorkflow(String workflowInstanceId, @Nullable Object output) {
    if (tracer == null) {
      this.innerClient.terminate(workflowInstanceId, output);
    }
    Span span = tracer.spanBuilder("dapr.workflow.terminateWorkflow")
        .setAttribute("instanceId", workflowInstanceId)
        .setAttribute("output", output.toString())
        .startSpan();
    try {
      this.innerClient.terminate(workflowInstanceId, output);
    } finally {
      span.end();
    }
  }

  /**
   * Fetches workflow instance metadata from the configured durable store.
   *
   * @param instanceId          the unique ID of the workflow instance to fetch
   * @param getInputsAndOutputs <code>true</code> to fetch the workflow instance's
   *                            inputs, outputs, and custom status, or <code>false</code> to omit them
   * @return a metadata record that describes the workflow instance and it execution status, or a default instance
   * @deprecated Use {@link #getWorkflowState(String, boolean)} instead.
   */
  @Nullable
  @Deprecated(forRemoval = true)
  public WorkflowInstanceStatus getInstanceState(String instanceId, boolean getInputsAndOutputs) {
    if (tracer == null) {
      OrchestrationMetadata metadata = this.innerClient.getInstanceMetadata(instanceId, getInputsAndOutputs);
      return metadata == null ? null : new DefaultWorkflowInstanceStatus(metadata);
    }
    Span span = tracer.spanBuilder("dapr.workflow.getInstanceState")
        .setAttribute("instanceId", instanceId)
        .setAttribute("fetchInputOutput", getInputsAndOutputs)
        .startSpan();
    try {
      OrchestrationMetadata metadata = this.innerClient.getInstanceMetadata(instanceId, getInputsAndOutputs);
      return metadata == null ? null : new DefaultWorkflowInstanceStatus(metadata);
    } finally {
      span.end();
    }
  }

  /**
   * Fetches workflow instance metadata from the configured durable store.
   *
   * @param instanceId          the unique ID of the workflow instance to fetch
   * @param getInputsAndOutputs <code>true</code> to fetch the workflow instance's
   *                            inputs, outputs, and custom status, or <code>false</code> to omit them
   * @return a metadata record that describes the workflow instance and it execution status, or a default instance
   */
  @Nullable
  public WorkflowState getWorkflowState(String instanceId, boolean getInputsAndOutputs) {
    if (tracer == null) {
      OrchestrationMetadata metadata = this.innerClient.getInstanceMetadata(instanceId, getInputsAndOutputs);
      return metadata == null ? null : new DefaultWorkflowState(metadata);
    }
    Span span = tracer.spanBuilder("dapr.workflow.getWorkflowState")
        .setAttribute("instanceId", instanceId)
        .setAttribute("fetchInputOutput", getInputsAndOutputs)
        .startSpan();
    try {
      OrchestrationMetadata metadata = this.innerClient.getInstanceMetadata(instanceId, getInputsAndOutputs);
      return metadata == null ? null : new DefaultWorkflowState(metadata);
    } finally {
      span.end();
    }
  }

  /**
   * Waits for an workflow to start running and returns an
   * {@link WorkflowInstanceStatus} object that contains metadata about the started
   * instance and optionally its input, output, and custom status payloads.
   *
   * <p>A "started" workflow instance is any instance not in the Pending state.
   *
   * <p>If an workflow instance is already running when this method is called,
   * the method will return immediately.
   *
   * @param instanceId          the unique ID of the workflow instance to wait for
   * @param timeout             the amount of time to wait for the workflow instance to start
   * @param getInputsAndOutputs true to fetch the workflow instance's
   *                            inputs, outputs, and custom status, or false to omit them
   * @return the workflow instance metadata or null if no such instance is found
   * @throws TimeoutException when the workflow instance is not started within the specified amount of time
   * @deprecated Use {@link #waitForWorkflowStart(String, Duration, boolean)} instead.
   */
  @Deprecated(forRemoval = true)
  @Nullable
  public WorkflowInstanceStatus waitForInstanceStart(String instanceId, Duration timeout, boolean getInputsAndOutputs)
      throws TimeoutException {

    if (tracer == null) {
      OrchestrationMetadata metadata = this.innerClient.waitForInstanceStart(instanceId, timeout, getInputsAndOutputs);
      return metadata == null ? null : new DefaultWorkflowInstanceStatus(metadata);
    }
    Span span = tracer.spanBuilder("dapr.workflow.waitForInstanceStart")
        .setAttribute("instanceId", instanceId)
        .setAttribute("fetchInputOutput", getInputsAndOutputs)
        .startSpan();
    try {
      OrchestrationMetadata metadata = this.innerClient.waitForInstanceStart(instanceId, timeout, getInputsAndOutputs);
      return metadata == null ? null : new DefaultWorkflowInstanceStatus(metadata);
    } finally {
      span.end();
    }
  }


  /**
   * Waits for a workflow to start running and returns an
   * {@link WorkflowState} object that contains metadata about the started
   * instance and optionally its input, output, and custom status payloads.
   *
   * <p>A "started" workflow instance is any instance not in the Pending state.
   *
   * <p>If an workflow instance is already running when this method is called,
   * the method will return immediately.
   *
   * @param instanceId          the unique ID of the workflow instance to wait for
   * @param timeout             the amount of time to wait for the workflow instance to start
   * @param getInputsAndOutputs true to fetch the workflow instance's
   *                            inputs, outputs, and custom status, or false to omit them
   * @return the workflow instance metadata or null if no such instance is found
   * @throws TimeoutException when the workflow instance is not started within the specified amount of time
   */
  @Nullable
  public WorkflowState waitForWorkflowStart(String instanceId, Duration timeout, boolean getInputsAndOutputs)
      throws TimeoutException {

    if (tracer == null) {
      OrchestrationMetadata metadata = this.innerClient.waitForInstanceStart(instanceId, timeout, getInputsAndOutputs);
      return metadata == null ? null : new DefaultWorkflowState(metadata);
    }
    Span span = tracer.spanBuilder("dapr.workflow.waitForWorkflowStart")
        .setAttribute("instanceId", instanceId)
        .setAttribute("fetchInputOutput", getInputsAndOutputs)
        .startSpan();
    try {
      OrchestrationMetadata metadata = this.innerClient.waitForInstanceStart(instanceId, timeout, getInputsAndOutputs);
      return metadata == null ? null : new DefaultWorkflowState(metadata);
    } finally {
      span.end();
    }
  }

  /**
   * Waits for an workflow to complete and returns an {@link WorkflowInstanceStatus} object that contains
   * metadata about the completed instance.
   *
   * <p>A "completed" workflow instance is any instance in one of the terminal states. For example, the
   * Completed, Failed, or Terminated states.
   *
   * <p>Workflows are long-running and could take hours, days, or months before completing.
   * Workflows can also be eternal, in which case they'll never complete unless terminated.
   * In such cases, this call may block indefinitely, so care must be taken to ensure appropriate timeouts are used.
   * If an workflow instance is already complete when this method is called, the method will return immediately.
   *
   * @param instanceId          the unique ID of the workflow instance to wait for
   * @param timeout             the amount of time to wait for the workflow instance to complete
   * @param getInputsAndOutputs true to fetch the workflow instance's inputs, outputs, and custom
   *                            status, or false to omit them
   * @return the workflow instance metadata or null if no such instance is found
   * @throws TimeoutException when the workflow instance is not completed within the specified amount of time
   * @deprecated Use {@link #waitForWorkflowCompletion(String, Duration, boolean)} instead.
   */
  @Nullable
  @Deprecated(forRemoval = true)
  public WorkflowInstanceStatus waitForInstanceCompletion(String instanceId, Duration timeout,
                                                          boolean getInputsAndOutputs) throws TimeoutException {

    if (tracer == null) {
      OrchestrationMetadata metadata = this.innerClient.waitForInstanceCompletion(instanceId, timeout,
          getInputsAndOutputs);
      return metadata == null ? null : new DefaultWorkflowInstanceStatus(metadata);
    }
    Span span = tracer.spanBuilder("dapr.workflow.waitForWorkflowStart")
        .setAttribute("instanceId", instanceId)
        .setAttribute("fetchInputOutput", getInputsAndOutputs)
        .startSpan();
    try {
      OrchestrationMetadata metadata = this.innerClient.waitForInstanceCompletion(instanceId, timeout,
          getInputsAndOutputs);
      return metadata == null ? null : new DefaultWorkflowInstanceStatus(metadata);
    } finally {
      span.end();
    }
  }


  /**
   * Waits for an workflow to complete and returns an {@link WorkflowState} object that contains
   * metadata about the completed instance.
   *
   * <p>A "completed" workflow instance is any instance in one of the terminal states. For example, the
   * Completed, Failed, or Terminated states.
   *
   * <p>Workflows are long-running and could take hours, days, or months before completing.
   * Workflows can also be eternal, in which case they'll never complete unless terminated.
   * In such cases, this call may block indefinitely, so care must be taken to ensure appropriate timeouts are used.
   * If an workflow instance is already complete when this method is called, the method will return immediately.
   *
   * @param instanceId          the unique ID of the workflow instance to wait for
   * @param timeout             the amount of time to wait for the workflow instance to complete
   * @param getInputsAndOutputs true to fetch the workflow instance's inputs, outputs, and custom
   *                            status, or false to omit them
   * @return the workflow instance metadata or null if no such instance is found
   * @throws TimeoutException when the workflow instance is not completed within the specified amount of time
   */
  @Nullable
  public WorkflowState waitForWorkflowCompletion(String instanceId, Duration timeout,
                                                          boolean getInputsAndOutputs) throws TimeoutException {

    if (tracer == null) {
      OrchestrationMetadata metadata = this.innerClient.waitForInstanceCompletion(instanceId, timeout,
          getInputsAndOutputs);
      return metadata == null ? null : new DefaultWorkflowState(metadata);
    }
    Span span = tracer.spanBuilder("dapr.workflow.waitForWorkflowCompletion")
        .setAttribute("instanceId", instanceId)
        .setAttribute("fetchInputOutput", getInputsAndOutputs)
        .startSpan();
    try {
      OrchestrationMetadata metadata = this.innerClient.waitForInstanceCompletion(instanceId, timeout,
          getInputsAndOutputs);
      return metadata == null ? null : new DefaultWorkflowState(metadata);
    } finally {
      span.end();
    }
  }

  /**
   * Sends an event notification message to awaiting workflow instance.
   *
   * @param workflowInstanceId The ID of the workflow instance that will handle the event.
   * @param eventName          The name of the event. Event names are case-insensitive.
   * @param eventPayload       The serializable data payload to include with the event.
   */
  public void raiseEvent(String workflowInstanceId, String eventName, Object eventPayload) {
    if (tracer == null) {
      this.innerClient.raiseEvent(workflowInstanceId, eventName, eventPayload);
    }

    Span span = tracer.spanBuilder("dapr.workflow.raiseEvent")
        .setAttribute("eventName", eventName)
        .setAttribute("payload", eventPayload.toString())
        .startSpan();
    try {
      this.innerClient.raiseEvent(workflowInstanceId, eventName, eventPayload);
    } finally {
      span.end();
    }
  }

  /**
   * Purges workflow instance state from the workflow state store.
   *
   * @param workflowInstanceId The unique ID of the workflow instance to purge.
   * @return Return true if the workflow state was found and purged successfully otherwise false.
   * @deprecated Use {@link #purgeWorkflow(String)} instead.
   */
  @Deprecated(forRemoval = true)
  public boolean purgeInstance(String workflowInstanceId) {
    if (tracer == null) {
      PurgeResult result = this.innerClient.purgeInstance(workflowInstanceId);

      if (result != null) {
        return result.getDeletedInstanceCount() > 0;
      }
    }
    Span span = tracer.spanBuilder("dapr.workflow.purgeInstance")
        .setAttribute("instanceId", workflowInstanceId)
        .startSpan();
    try {
      PurgeResult result = this.innerClient.purgeInstance(workflowInstanceId);

      if (result != null) {
        return result.getDeletedInstanceCount() > 0;
      }

      return false;
    } finally {
      span.end();
    }
  }

  /**
   * Purges workflow instance state from the workflow state store.
   *
   * @param workflowInstanceId The unique ID of the workflow instance to purge.
   * @return Return true if the workflow state was found and purged successfully otherwise false.
   */
  public boolean purgeWorkflow(String workflowInstanceId) {
    if (tracer == null) {
      PurgeResult result = this.innerClient.purgeInstance(workflowInstanceId);

      if (result != null) {
        return result.getDeletedInstanceCount() > 0;
      }

      return false;
    }
    Span span = tracer.spanBuilder("dapr.workflow.waitForWorkflowCompletion")
        .setAttribute("instanceId", instanceId)
        .setAttribute("fetchInputOutput", getInputsAndOutputs)
        .startSpan();
    try {
      PurgeResult result = this.innerClient.purgeInstance(workflowInstanceId);

      if (result != null) {
        return result.getDeletedInstanceCount() > 0;
      }

      return false;
    } finally {
      span.end();
    }
  }

  /**
   * Closes the inner DurableTask client and shutdown the GRPC channel.
   */
  public void close() throws InterruptedException {
    try {
      if (this.innerClient != null) {
        this.innerClient.close();
        this.innerClient = null;
      }
    } finally {
      if (this.grpcChannel != null && !this.grpcChannel.isShutdown()) {
        this.grpcChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        this.grpcChannel = null;
      }
    }
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

  private static NewOrchestrationInstanceOptions fromNewWorkflowOptions(NewWorkflowOptions options) {
    NewOrchestrationInstanceOptions instanceOptions = new NewOrchestrationInstanceOptions();

    if (options.getVersion() != null) {
      instanceOptions.setVersion(options.getVersion());
    }

    if (options.getInstanceId() != null) {
      instanceOptions.setInstanceId(options.getInstanceId());
    }

    if (options.getInput() != null) {
      instanceOptions.setInput(options.getInput());
    }

    if (options.getStartTime() != null) {
      instanceOptions.setStartTime(options.getStartTime());
    }

    return instanceOptions;
  }

}
