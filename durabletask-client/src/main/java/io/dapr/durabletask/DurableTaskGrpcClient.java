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
import com.google.protobuf.Timestamp;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.TlsChannelCredentials;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Durable Task client implementation that uses gRPC to connect to a remote "sidecar" process.
 */
public final class DurableTaskGrpcClient extends DurableTaskClient {
  private static final int DEFAULT_PORT = 4001;
  private static final Logger logger = Logger.getLogger(DurableTaskGrpcClient.class.getPackage().getName());
  private static final String GRPC_TLS_CA_PATH = "DAPR_GRPC_TLS_CA_PATH";
  private static final String GRPC_TLS_CERT_PATH = "DAPR_GRPC_TLS_CERT_PATH";
  private static final String GRPC_TLS_KEY_PATH = "DAPR_GRPC_TLS_KEY_PATH";
  private static final String GRPC_TLS_INSECURE = "DAPR_GRPC_TLS_INSECURE";

  private final DataConverter dataConverter;
  private final ManagedChannel managedSidecarChannel;
  private final TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub sidecarClient;
  private final Tracer tracer;

  DurableTaskGrpcClient(DurableTaskGrpcClientBuilder builder) {
    this.dataConverter = builder.dataConverter != null ? builder.dataConverter : new JacksonDataConverter();

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

      String endpoint = "localhost:" + port;
      ManagedChannelBuilder<?> channelBuilder;

      // Get TLS configuration from builder or environment variables
      String tlsCaPath = builder.tlsCaPath != null ? builder.tlsCaPath : System.getenv(GRPC_TLS_CA_PATH);
      String tlsCertPath = builder.tlsCertPath != null ? builder.tlsCertPath : System.getenv(GRPC_TLS_CERT_PATH);
      String tlsKeyPath = builder.tlsKeyPath != null ? builder.tlsKeyPath : System.getenv(GRPC_TLS_KEY_PATH);
      boolean insecure = builder.insecure || Boolean.parseBoolean(System.getenv(GRPC_TLS_INSECURE));

      if (insecure) {
        // Insecure mode - uses TLS but doesn't verify certificates
        try {
          channelBuilder = NettyChannelBuilder.forTarget(endpoint)
              .sslContext(GrpcSslContexts.forClient()
                  .trustManager(InsecureTrustManagerFactory.INSTANCE)
                  .build());
        } catch (Exception e) {
          throw new RuntimeException("Failed to create insecure TLS credentials", e);
        }
      } else if (tlsCertPath != null && tlsKeyPath != null) {
        // mTLS case - using client cert and key, with optional CA cert for server authentication
        try (
            InputStream clientCertInputStream = new FileInputStream(tlsCertPath);
            InputStream clientKeyInputStream = new FileInputStream(tlsKeyPath);
            InputStream caCertInputStream = tlsCaPath != null ? new FileInputStream(tlsCaPath) : null
        ) {
          TlsChannelCredentials.Builder tlsBuilder = TlsChannelCredentials.newBuilder()
              .keyManager(clientCertInputStream, clientKeyInputStream);  // For client authentication
          if (caCertInputStream != null) {
            tlsBuilder.trustManager(caCertInputStream);  // For server authentication
          }
          ChannelCredentials credentials = tlsBuilder.build();
          channelBuilder = Grpc.newChannelBuilder(endpoint, credentials);
        } catch (IOException e) {
          throw new RuntimeException("Failed to create mTLS credentials"
              + (tlsCaPath != null ? " with CA cert" : ""), e);
        }
      } else if (tlsCaPath != null) {
        // Simple TLS case - using CA cert only for server authentication
        try (InputStream caCertInputStream = new FileInputStream(tlsCaPath)) {
          ChannelCredentials credentials = TlsChannelCredentials.newBuilder()
              .trustManager(caCertInputStream)
              .build();
          channelBuilder = Grpc.newChannelBuilder(endpoint, credentials);
        } catch (IOException e) {
          throw new RuntimeException("Failed to create TLS credentials with CA cert", e);
        }
      } else {
        // No TLS config provided, use plaintext
        channelBuilder = ManagedChannelBuilder.forTarget(endpoint).usePlaintext();
      }

      // Need to keep track of this channel so we can dispose it on close()
      this.managedSidecarChannel = channelBuilder.build();
      sidecarGrpcChannel = this.managedSidecarChannel;
    }

    if (builder.tracer != null) {
      this.tracer = builder.tracer;
    } else {
      //this.tracer = OpenTelemetry.noop().getTracer("DurableTaskGrpcClient");
      this.tracer = GlobalOpenTelemetry.getTracer("dapr-workflow");
    }

    this.sidecarClient = TaskHubSidecarServiceGrpc.newBlockingStub(sidecarGrpcChannel);
  }

  /**
   * Closes the internally managed gRPC channel, if one exists.
   *
   * <p>This method is a no-op if this client object was created using a builder with a gRPC channel object explicitly
   * configured.</p>
   */
  @Override
  public void close() {
    if (this.managedSidecarChannel != null) {
      try {
        this.managedSidecarChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        // Best effort. Also note that AutoClose documentation recommends NOT having
        // close() methods throw InterruptedException:
        // https://docs.oracle.com/javase/7/docs/api/java/lang/AutoCloseable.html
      }
    }
  }

  @Override
  public String scheduleNewOrchestrationInstance(
      String orchestratorName,
      NewOrchestrationInstanceOptions options) {
    if (orchestratorName == null || orchestratorName.length() == 0) {
      throw new IllegalArgumentException("A non-empty orchestrator name must be specified.");
    }

    Helpers.throwIfArgumentNull(options, "options");

    OrchestratorService.CreateInstanceRequest.Builder builder = OrchestratorService.CreateInstanceRequest.newBuilder();
    builder.setName(orchestratorName);

    String instanceId = options.getInstanceId();
    if (instanceId == null) {
      instanceId = UUID.randomUUID().toString();
    }
    builder.setInstanceId(instanceId);

    String version = options.getVersion();
    if (version != null) {
      builder.setVersion(StringValue.of(version));
    }

    Object input = options.getInput();
    if (input != null) {
      String serializedInput = this.dataConverter.serialize(input);
      builder.setInput(StringValue.of(serializedInput));
    }

    Instant startTime = options.getStartTime();
    if (startTime != null) {
      Timestamp ts = DataConverter.getTimestampFromInstant(startTime);
      builder.setScheduledStartTimestamp(ts);
    }

    AtomicReference<OrchestratorService.CreateInstanceResponse> response = new AtomicReference<>();

    OrchestratorService.CreateInstanceRequest request = builder.build();
    response.set(this.sidecarClient.startInstance(request));

    return response.get().getInstanceId();
  }

  @Override
  public void raiseEvent(String instanceId, String eventName, Object eventPayload) {
    Helpers.throwIfArgumentNull(instanceId, "instanceId");
    Helpers.throwIfArgumentNull(eventName, "eventName");

    OrchestratorService.RaiseEventRequest.Builder builder = OrchestratorService.RaiseEventRequest.newBuilder()
        .setInstanceId(instanceId)
        .setName(eventName);
    if (eventPayload != null) {
      String serializedPayload = this.dataConverter.serialize(eventPayload);
      builder.setInput(StringValue.of(serializedPayload));
    }

    OrchestratorService.RaiseEventRequest request = builder.build();
    this.sidecarClient.raiseEvent(request);

  }

  @Override
  public OrchestrationMetadata getInstanceMetadata(String instanceId, boolean getInputsAndOutputs) {
    OrchestratorService.GetInstanceRequest request = OrchestratorService.GetInstanceRequest.newBuilder()
        .setInstanceId(instanceId)
        .setGetInputsAndOutputs(getInputsAndOutputs)
        .build();
    OrchestratorService.GetInstanceResponse response = this.sidecarClient.getInstance(request);
    return new OrchestrationMetadata(response, this.dataConverter, request.getGetInputsAndOutputs());
  }

  @Override
  public OrchestrationMetadata waitForInstanceStart(String instanceId, Duration timeout, boolean getInputsAndOutputs)
      throws TimeoutException {
    OrchestratorService.GetInstanceRequest request = OrchestratorService.GetInstanceRequest.newBuilder()
        .setInstanceId(instanceId)
        .setGetInputsAndOutputs(getInputsAndOutputs)
        .build();

    if (timeout == null || timeout.isNegative() || timeout.isZero()) {
      timeout = Duration.ofMinutes(10);
    }

    TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub grpcClient = this.sidecarClient.withDeadlineAfter(
        timeout.toMillis(),
        TimeUnit.MILLISECONDS);

    OrchestratorService.GetInstanceResponse response;
    try {
      response = grpcClient.waitForInstanceStart(request);
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
        throw new TimeoutException("Start orchestration timeout reached.");
      }
      throw e;
    }
    return new OrchestrationMetadata(response, this.dataConverter, request.getGetInputsAndOutputs());
  }

  @Override
  public OrchestrationMetadata waitForInstanceCompletion(String instanceId, Duration timeout,
                                                         boolean getInputsAndOutputs) throws TimeoutException {
    OrchestratorService.GetInstanceRequest request = OrchestratorService.GetInstanceRequest.newBuilder()
        .setInstanceId(instanceId)
        .setGetInputsAndOutputs(getInputsAndOutputs)
        .build();

    if (timeout == null || timeout.isNegative() || timeout.isZero()) {
      timeout = Duration.ofMinutes(10);
    }

    TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub grpcClient = this.sidecarClient.withDeadlineAfter(
        timeout.toMillis(),
        TimeUnit.MILLISECONDS);

    OrchestratorService.GetInstanceResponse response;
    try {
      response = grpcClient.waitForInstanceCompletion(request);
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
        throw new TimeoutException("Orchestration instance completion timeout reached.");
      }
      throw e;
    }
    return new OrchestrationMetadata(response, this.dataConverter, request.getGetInputsAndOutputs());
  }

  @Override
  public void terminate(String instanceId, @Nullable Object output) {
    Helpers.throwIfArgumentNull(instanceId, "instanceId");
    String serializeOutput = this.dataConverter.serialize(output);
    this.logger.fine(() -> String.format(
        "Terminating instance %s and setting output to: %s",
        instanceId,
        serializeOutput != null ? serializeOutput : "(null)"));
    OrchestratorService.TerminateRequest.Builder builder = OrchestratorService.TerminateRequest.newBuilder()
        .setInstanceId(instanceId);
    if (serializeOutput != null) {
      builder.setOutput(StringValue.of(serializeOutput));
    }
    this.sidecarClient.terminateInstance(builder.build());
  }

  @Override
  public OrchestrationStatusQueryResult queryInstances(OrchestrationStatusQuery query) {
    OrchestratorService.InstanceQuery.Builder instanceQueryBuilder = OrchestratorService.InstanceQuery.newBuilder();
    Optional.ofNullable(query.getCreatedTimeFrom()).ifPresent(createdTimeFrom ->
        instanceQueryBuilder.setCreatedTimeFrom(DataConverter.getTimestampFromInstant(createdTimeFrom)));
    Optional.ofNullable(query.getCreatedTimeTo()).ifPresent(createdTimeTo ->
        instanceQueryBuilder.setCreatedTimeTo(DataConverter.getTimestampFromInstant(createdTimeTo)));
    Optional.ofNullable(query.getContinuationToken()).ifPresent(token ->
        instanceQueryBuilder.setContinuationToken(StringValue.of(token)));
    Optional.ofNullable(query.getInstanceIdPrefix()).ifPresent(prefix ->
        instanceQueryBuilder.setInstanceIdPrefix(StringValue.of(prefix)));
    instanceQueryBuilder.setFetchInputsAndOutputs(query.isFetchInputsAndOutputs());
    instanceQueryBuilder.setMaxInstanceCount(query.getMaxInstanceCount());
    query.getRuntimeStatusList().forEach(runtimeStatus ->
        Optional.ofNullable(runtimeStatus).ifPresent(status ->
            instanceQueryBuilder.addRuntimeStatus(OrchestrationRuntimeStatus.toProtobuf(status))));
    query.getTaskHubNames().forEach(taskHubName -> Optional.ofNullable(taskHubName).ifPresent(name ->
        instanceQueryBuilder.addTaskHubNames(StringValue.of(name))));
    OrchestratorService.QueryInstancesResponse queryInstancesResponse = this.sidecarClient
        .queryInstances(OrchestratorService.QueryInstancesRequest.newBuilder().setQuery(instanceQueryBuilder).build());
    return toQueryResult(queryInstancesResponse, query.isFetchInputsAndOutputs());
  }

  private OrchestrationStatusQueryResult toQueryResult(
      OrchestratorService.QueryInstancesResponse queryInstancesResponse, boolean fetchInputsAndOutputs) {
    List<OrchestrationMetadata> metadataList = new ArrayList<>();
    queryInstancesResponse.getOrchestrationStateList().forEach(state -> {
      metadataList.add(new OrchestrationMetadata(state, this.dataConverter, fetchInputsAndOutputs));
    });
    return new OrchestrationStatusQueryResult(metadataList, queryInstancesResponse.getContinuationToken().getValue());
  }

  @Override
  public void createTaskHub(boolean recreateIfExists) {
    this.sidecarClient.createTaskHub(OrchestratorService.CreateTaskHubRequest.newBuilder()
        .setRecreateIfExists(recreateIfExists).build());
  }

  @Override
  public void deleteTaskHub() {
    this.sidecarClient.deleteTaskHub(OrchestratorService.DeleteTaskHubRequest.newBuilder().build());
  }

  @Override
  public PurgeResult purgeInstance(String instanceId) {
    OrchestratorService.PurgeInstancesRequest request = OrchestratorService.PurgeInstancesRequest.newBuilder()
        .setInstanceId(instanceId)
        .build();

    OrchestratorService.PurgeInstancesResponse response = this.sidecarClient.purgeInstances(request);
    return toPurgeResult(response);
  }

  @Override
  public PurgeResult purgeInstances(PurgeInstanceCriteria purgeInstanceCriteria) throws TimeoutException {
    OrchestratorService.PurgeInstanceFilter.Builder builder = OrchestratorService.PurgeInstanceFilter.newBuilder();
    builder.setCreatedTimeFrom(DataConverter.getTimestampFromInstant(purgeInstanceCriteria.getCreatedTimeFrom()));
    Optional.ofNullable(purgeInstanceCriteria.getCreatedTimeTo()).ifPresent(createdTimeTo ->
        builder.setCreatedTimeTo(DataConverter.getTimestampFromInstant(createdTimeTo)));
    purgeInstanceCriteria.getRuntimeStatusList().forEach(runtimeStatus ->
        Optional.ofNullable(runtimeStatus).ifPresent(status ->
            builder.addRuntimeStatus(OrchestrationRuntimeStatus.toProtobuf(status))));

    Duration timeout = purgeInstanceCriteria.getTimeout();
    if (timeout == null || timeout.isNegative() || timeout.isZero()) {
      timeout = Duration.ofMinutes(4);
    }

    TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub grpcClient = this.sidecarClient.withDeadlineAfter(
        timeout.toMillis(),
        TimeUnit.MILLISECONDS);

    OrchestratorService.PurgeInstancesResponse response;
    try {
      response = grpcClient.purgeInstances(OrchestratorService.PurgeInstancesRequest.newBuilder()
          .setPurgeInstanceFilter(builder).build());
      return toPurgeResult(response);
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
        String timeOutException = String.format("Purge instances timeout duration of %s reached.", timeout);
        throw new TimeoutException(timeOutException);
      }
      throw e;
    }
  }

  @Override
  public void suspendInstance(String instanceId, @Nullable String reason) {
    OrchestratorService.SuspendRequest.Builder suspendRequestBuilder = OrchestratorService.SuspendRequest.newBuilder();
    suspendRequestBuilder.setInstanceId(instanceId);
    if (reason != null) {
      suspendRequestBuilder.setReason(StringValue.of(reason));
    }
    this.sidecarClient.suspendInstance(suspendRequestBuilder.build());
  }

  @Override
  public void resumeInstance(String instanceId, @Nullable String reason) {
    OrchestratorService.ResumeRequest.Builder resumeRequestBuilder = OrchestratorService.ResumeRequest.newBuilder();
    resumeRequestBuilder.setInstanceId(instanceId);
    if (reason != null) {
      resumeRequestBuilder.setReason(StringValue.of(reason));
    }
    this.sidecarClient.resumeInstance(resumeRequestBuilder.build());
  }

  @Override
  public String restartInstance(String instanceId, boolean restartWithNewInstanceId) {
    OrchestrationMetadata metadata = this.getInstanceMetadata(instanceId, true);
    if (!metadata.isInstanceFound()) {
      throw new IllegalArgumentException(new StringBuilder()
          .append("An orchestration with instanceId ")
          .append(instanceId)
          .append(" was not found.").toString());
    }

    if (restartWithNewInstanceId) {
      return this.scheduleNewOrchestrationInstance(metadata.getName(),
          this.dataConverter.deserialize(metadata.getSerializedInput(), Object.class));
    } else {
      return this.scheduleNewOrchestrationInstance(metadata.getName(),
          this.dataConverter.deserialize(metadata.getSerializedInput(), Object.class), metadata.getInstanceId());
    }
  }

  private PurgeResult toPurgeResult(OrchestratorService.PurgeInstancesResponse response) {
    return new PurgeResult(response.getDeletedInstanceCount());
  }
}
