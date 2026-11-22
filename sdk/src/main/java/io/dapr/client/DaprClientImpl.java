/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.dapr.client.domain.BatchQueryVectorsRequestAlpha1;
import io.dapr.client.domain.BatchQueryVectorsResultAlpha1;
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.ConversationRequest;
import io.dapr.client.domain.ConversationRequestAlpha2;
import io.dapr.client.domain.ConversationResponse;
import io.dapr.client.domain.ConversationResponseAlpha2;
import io.dapr.client.domain.CreateVectorCollectionRequestAlpha1;
import io.dapr.client.domain.DaprMetadata;
import io.dapr.client.domain.DecryptRequestAlpha1;
import io.dapr.client.domain.DeleteJobRequest;
import io.dapr.client.domain.DeleteSearchDocumentsRequestAlpha1;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.DeleteVectorsRequestAlpha1;
import io.dapr.client.domain.EncryptRequestAlpha1;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.GetBulkSecretRequest;
import io.dapr.client.domain.GetBulkStateRequest;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.GetJobRequest;
import io.dapr.client.domain.GetJobResponse;
import io.dapr.client.domain.GetSearchDocumentsRequestAlpha1;
import io.dapr.client.domain.GetSearchIndexResponseAlpha1;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.GetVectorCollectionResponseAlpha1;
import io.dapr.client.domain.GetVectorsRequestAlpha1;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.IndexAck;
import io.dapr.client.domain.IndexSearchDocumentsRequestAlpha1;
import io.dapr.client.domain.IndexSearchDocumentsResponseAlpha1;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.LockRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.QueryVectorsRequestAlpha1;
import io.dapr.client.domain.QueryVectorsResponseAlpha1;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.ScheduleJobRequest;
import io.dapr.client.domain.SearchDocument;
import io.dapr.client.domain.SearchRequestAlpha1;
import io.dapr.client.domain.SearchResponseAlpha1;
import io.dapr.client.domain.SetBinaryFileRequestAlpha1;
import io.dapr.client.domain.State;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.client.domain.UnlockRequest;
import io.dapr.client.domain.UnlockResponseStatus;
import io.dapr.client.domain.UnsubscribeConfigurationRequest;
import io.dapr.client.domain.UnsubscribeConfigurationResponse;
import io.dapr.client.domain.UpsertVectorsRequestAlpha1;
import io.dapr.client.domain.UpsertVectorsResponseAlpha1;
import io.dapr.client.domain.VectorRecord;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.exceptions.DaprException;
import io.dapr.internal.exceptions.DaprHttpException;
import io.dapr.internal.grpc.DaprClientGrpcInterceptors;
import io.dapr.internal.resiliency.RetryPolicy;
import io.dapr.internal.resiliency.TimeoutPolicy;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprBindingsProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import static io.dapr.internal.exceptions.DaprHttpException.isSuccessfulHttpStatusCode;
import static io.dapr.internal.exceptions.DaprHttpException.isValidHttpStatusCode;
import static io.dapr.internal.exceptions.DaprHttpException.parseHttpStatusCode;

/**
 * Implementation of the Dapr client combining gRPC and HTTP (when applicable).
 *
 * @see io.dapr.v1.DaprGrpc
 * @see io.dapr.client.DaprClient
 */
public class DaprClientImpl extends AbstractDaprClient {

  private final Logger logger;

  /**
   * The GRPC managed channel to be used.
   */
  private final GrpcChannelFacade channel;

  /**
   * The HTTP client to be used for healthz and HTTP service invocation only.
   *
   * @see io.dapr.client.DaprHttp
   */
  private final DaprHttp httpClient;

  private final DaprClientGrpcInterceptors grpcInterceptors;

  private final DaprGrpcCalls grpcCalls;

  private final DaprConversationClient conversationClient;

  private final DaprMetadataClient metadataClient;

  private final DaprConfigurationClient configurationClient;

  private final DaprLockClient lockClient;

  private final DaprSecretsClient secretsClient;

  private final DaprCryptoClient cryptoClient;

  private final DaprJobsClient jobsClient;

  private final DaprBinaryFileClient binaryFileClient;

  private final DaprPubSubClient pubSubClient;

  private final DaprStateClient stateClient;

  private final DaprVectorClient vectorClient;

  private final DaprSearchClient searchClient;

  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param channel           Facade for the managed GRPC channel
   * @param asyncStub         async gRPC stub
   * @param objectSerializer  Serializer for transient request/response objects.
   * @param stateSerializer   Serializer for state objects.
   * @see DaprClientBuilder
   */
  DaprClientImpl(
      GrpcChannelFacade channel,
      DaprGrpc.DaprStub asyncStub,
      DaprHttp httpClient,
      DaprObjectSerializer objectSerializer,
      DaprObjectSerializer stateSerializer) {
    this(channel, asyncStub, httpClient, objectSerializer, stateSerializer, null, null);
  }

  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param channel           Facade for the managed GRPC channel
   * @param asyncStub         async gRPC stub
   * @param objectSerializer  Serializer for transient request/response objects.
   * @param stateSerializer   Serializer for state objects.
   * @param daprApiToken      Dapr API Token.
   * @see DaprClientBuilder
   */
  DaprClientImpl(
      GrpcChannelFacade channel,
      DaprGrpc.DaprStub asyncStub,
      DaprHttp httpClient,
      DaprObjectSerializer objectSerializer,
      DaprObjectSerializer stateSerializer,
      String daprApiToken) {
    this(channel, asyncStub, httpClient, objectSerializer, stateSerializer, null, daprApiToken);
  }

  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param channel           Facade for the managed GRPC channel
   * @param asyncStub         async gRPC stub
   * @param httpClient        client for http service invocation
   * @param objectSerializer  Serializer for transient request/response objects.
   * @param stateSerializer   Serializer for state objects.
   * @param resiliencyOptions Client-level override for resiliency options.
   * @param daprApiToken      Dapr API Token.
   * @see DaprClientBuilder
   */
  DaprClientImpl(
      GrpcChannelFacade channel,
      DaprGrpc.DaprStub asyncStub,
      DaprHttp httpClient,
      DaprObjectSerializer objectSerializer,
      DaprObjectSerializer stateSerializer,
      ResiliencyOptions resiliencyOptions,
      String daprApiToken) {
    this(
        channel,
        asyncStub,
        httpClient,
        objectSerializer,
        stateSerializer,
        new TimeoutPolicy(resiliencyOptions == null ? null : resiliencyOptions.getTimeout()),
        new RetryPolicy(resiliencyOptions == null ? null : resiliencyOptions.getMaxRetries()),
        daprApiToken);
  }

  /**
   * Instantiates a new DaprClient.
   *
   * @param channel           Facade for the managed GRPC channel
   * @param asyncStub         async gRPC stub
   * @param httpClient        client for http service invocation
   * @param objectSerializer  Serializer for transient request/response objects.
   * @param stateSerializer   Serializer for state objects.
   * @param timeoutPolicy     Client-level timeout policy.
   * @param retryPolicy       Client-level retry policy.
   * @param daprApiToken      Dapr API Token.
   * @see DaprClientBuilder
   */
  private DaprClientImpl(
      GrpcChannelFacade channel,
      DaprGrpc.DaprStub asyncStub,
      DaprHttp httpClient,
      DaprObjectSerializer objectSerializer,
      DaprObjectSerializer stateSerializer,
      TimeoutPolicy timeoutPolicy,
      RetryPolicy retryPolicy,
      String daprApiToken) {
    super(objectSerializer, stateSerializer);
    this.channel = channel;
    this.httpClient = httpClient;
    this.grpcInterceptors = new DaprClientGrpcInterceptors(daprApiToken, timeoutPolicy);
    this.grpcCalls = new DaprGrpcCalls(asyncStub, this.grpcInterceptors, retryPolicy);
    this.conversationClient = new DaprConversationClient(this.grpcCalls, objectSerializer);
    this.metadataClient = new DaprMetadataClient(this.grpcCalls);
    this.configurationClient = new DaprConfigurationClient(this.grpcCalls);
    this.lockClient = new DaprLockClient(this.grpcCalls);
    this.secretsClient = new DaprSecretsClient(this.grpcCalls);
    this.cryptoClient = new DaprCryptoClient(this.grpcCalls);
    this.jobsClient = new DaprJobsClient(this.grpcCalls);
    this.binaryFileClient = new DaprBinaryFileClient(this.grpcCalls);
    this.pubSubClient = new DaprPubSubClient(this.grpcCalls, objectSerializer);
    this.stateClient = new DaprStateClient(this.grpcCalls, stateSerializer);
    this.vectorClient = new DaprVectorClient(this.grpcCalls);
    this.searchClient = new DaprSearchClient(this.grpcCalls);
    this.logger = LoggerFactory.getLogger(DaprClientImpl.class);
  }

  /**
   * {@inheritDoc}
   */
  public <T extends AbstractStub<T>> T newGrpcStub(String appId, Function<Channel, T> stubBuilder) {
    // Adds Dapr interceptors to populate gRPC metadata automatically.
    return this.grpcInterceptors.intercept(appId, stubBuilder.apply(this.channel.getGrpcChannel()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> waitForSidecar(int timeoutInMilliseconds) {
    String[] pathSegments = new String[] { DaprHttp.API_VERSION, "healthz", "outbound"};

    // Do the Dapr Http endpoint check to have parity with Dotnet
    Mono<DaprHttp.Response> responseMono = this.httpClient.invokeApi(DaprHttp.HttpMethods.GET.name(), pathSegments,
        null, "", null, null);

    return responseMono
        // No method to "retry forever every 500ms", so we make it practically forever.
        // 9223372036854775807 * 500 ms = 1.46235604 x 10^11 years
        // If anyone needs to wait for the sidecar for longer than that, sorry.
        .retryWhen(
            Retry
                .fixedDelay(Long.MAX_VALUE, Duration.ofMillis(500))
                .doBeforeRetry(s -> {
                  this.logger.info("Retrying sidecar health check ...");
                }))
        .timeout(Duration.ofMillis(timeoutInMilliseconds))
        .onErrorResume(DaprException.class, e ->
            Mono.error(new RuntimeException(e)))
        .switchIfEmpty(DaprException.wrapMono(new RuntimeException("Health check timed out")))
        .then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Boolean> healthCheck() {
    String[] pathSegments = new String[] { DaprHttp.API_VERSION, "healthz" };

    // Do the Dapr Http endpoint check to have parity with Dotnet
    return this.httpClient.invokeApi(DaprHttp.HttpMethods.GET.name(), pathSegments, null, "", null, null)
        .map(response -> true)
        .onErrorResume(e -> {
          this.logger.debug("Sidecar health check failed", e);
          return Mono.just(false);
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(PublishEventRequest request) {
    return this.pubSubClient.publishEvent(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<BulkPublishResponse<T>> publishEvents(BulkPublishRequest<T> request) {
    return this.pubSubClient.publishEvents(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Subscription subscribeToEvents(
      String pubsubName, String topic, SubscriptionListener<T> listener, TypeRef<T> type) {
    return this.pubSubClient.subscribeToEvents(pubsubName, topic, listener, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Subscription subscribeToEvents(
      String pubsubName,
      String topic,
      String deadLetterTopic,
      SubscriptionListener<T> listener,
      TypeRef<T> type) {
    return this.pubSubClient.subscribeToEvents(pubsubName, topic, deadLetterTopic, listener, type);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  @Override
  public <T> Flux<T> subscribeToEvents(String pubsubName, String topic, TypeRef<T> type) {
    return this.pubSubClient.subscribeToEvents(pubsubName, topic, type);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  @Override
  public <T> Flux<T> subscribeToEvents(String pubsubName, String topic, TypeRef<T> type, Map<String, String> metadata) {
    return this.pubSubClient.subscribeToEvents(pubsubName, topic, type, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Flux<T> subscribeToTopic(String pubsubName, String topic, TypeRef<T> type) {
    return this.pubSubClient.subscribeToTopic(pubsubName, topic, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Flux<T> subscribeToTopic(String pubsubName, String topic, TypeRef<T> type, Map<String, String> metadata) {
    return this.pubSubClient.subscribeToTopic(pubsubName, topic, type, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Flux<T> subscribeToTopic(String pubsubName, String topic, String deadLetterTopic, TypeRef<T> type) {
    return this.pubSubClient.subscribeToTopic(pubsubName, topic, deadLetterTopic, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Flux<T> subscribeToTopic(
      String pubsubName,
      String topic,
      String deadLetterTopic,
      TypeRef<T> type,
      Map<String, String> metadata) {
    return this.pubSubClient.subscribeToTopic(pubsubName, topic, deadLetterTopic, type, metadata);
  }

  @Override
  @Deprecated
  public <T> Mono<T> invokeMethod(InvokeMethodRequest invokeMethodRequest, TypeRef<T> type) {
    try {
      final String appId = invokeMethodRequest.getAppId();
      final String method = invokeMethodRequest.getMethod();
      final Object request = invokeMethodRequest.getBody();
      final HttpExtension httpExtension = invokeMethodRequest.getHttpExtension();
      final String contentType = invokeMethodRequest.getContentType();
      final Map<String, String> metadata = invokeMethodRequest.getMetadata();

      if (httpExtension == null) {
        throw new IllegalArgumentException("HttpExtension cannot be null. Use HttpExtension.NONE instead.");
      }
      // If the httpExtension is not null, then the method will not be null based on checks in constructor
      final String httpMethod = httpExtension.getMethod().toString();
      if (appId == null || appId.trim().isEmpty()) {
        throw new IllegalArgumentException("App Id cannot be null or empty.");
      }
      if (method == null || method.trim().isEmpty()) {
        throw new IllegalArgumentException("Method name cannot be null or empty.");
      }


      String[] methodSegments = method.split("/");

      List<String> pathSegments = new ArrayList<>(Arrays.asList(DaprHttp.API_VERSION, "invoke", appId, "method"));
      pathSegments.addAll(Arrays.asList(methodSegments));

      final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      headers.putAll(httpExtension.getHeaders());
      if (metadata != null) {
        headers.putAll(metadata);
      }
      byte[] serializedRequestBody = objectSerializer.serialize(request);
      if (contentType != null && !contentType.isEmpty()) {
        headers.put(io.dapr.client.domain.Metadata.CONTENT_TYPE, contentType);
      } else {
        headers.putIfAbsent(io.dapr.client.domain.Metadata.CONTENT_TYPE, objectSerializer.getContentType());
      }
      Mono<DaprHttp.Response> response = Mono.deferContextual(
          context -> this.httpClient.invokeApi(httpMethod, pathSegments.toArray(new String[0]),
              httpExtension.getQueryParams(), serializedRequestBody, headers, context)
      );
      return response.flatMap(r -> getMonoForHttpResponse(type, r));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  @Override
  public DaprInvokeHttpClient invokeHttpClient(String appId) {
    if (appId == null || appId.trim().isEmpty()) {
      throw new IllegalArgumentException("App Id cannot be null or empty.");
    }
    URI invokeBase = this.httpClient.getBaseUri()
        .resolve("/" + DaprHttp.API_VERSION + "/invoke/" + appId + "/method/");
    return new DaprInvokeHttpClient(
        this.httpClient.getHttpClient(),
        invokeBase,
        this.httpClient.getDaprApiToken(),
        this.httpClient.getReadTimeout());
  }

  private <T> Mono<T> getMonoForHttpResponse(TypeRef<T> type, DaprHttp.Response r) {
    try {
      if (type == null) {
        return Mono.empty();
      }

      T object = objectSerializer.deserialize(r.getBody(), type);
      if (object == null) {
        return Mono.empty();
      }

      return Mono.just(object);
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(InvokeBindingRequest request, TypeRef<T> type) {
    try {
      final String name = request.getName();
      final String operation = request.getOperation();
      final Object data = request.getData();
      final Map<String, String> metadata = request.getMetadata();
      if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Binding name cannot be null or empty.");
      }

      if (operation == null || operation.trim().isEmpty()) {
        throw new IllegalArgumentException("Binding operation cannot be null or empty.");
      }

      byte[] byteData = objectSerializer.serialize(data);
      DaprBindingsProtos.InvokeBindingRequest.Builder builder = DaprBindingsProtos.InvokeBindingRequest.newBuilder()
          .setName(name).setOperation(operation);
      if (byteData != null) {
        builder.setData(ByteString.copyFrom(byteData));
      }
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      DaprBindingsProtos.InvokeBindingRequest envelope = builder.build();

      Metadata responseMetadata = new Metadata();
      return Mono.deferContextual(
          context -> grpcCalls.<DaprBindingsProtos.InvokeBindingResponse>createMono(
              responseMetadata,
              it -> grpcCalls.intercept(context, m -> responseMetadata.merge(m)).invokeBinding(envelope, it)
          )
      ).flatMap(
          it -> {
            int httpStatusCode =
                parseHttpStatusCode(it.getMetadataMap().getOrDefault("statusCode", ""));
            if (isValidHttpStatusCode(httpStatusCode) && !isSuccessfulHttpStatusCode(httpStatusCode)) {
              // Exception condition in a successful request.
              // This is useful to send an exception due to an error from the HTTP binding component.
              throw DaprException.propagate(new DaprHttpException(httpStatusCode, it.getData().toByteArray()));
            }

            try {
              if (type == null) {
                return Mono.empty();
              }
              return Mono.justOrEmpty(objectSerializer.deserialize(it.getData().toByteArray(), type));
            } catch (IOException e) {
              throw DaprException.propagate(e);
            }
          }
      );
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(GetStateRequest request, TypeRef<T> type) {
    return this.stateClient.getState(request, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<List<State<T>>> getBulkState(GetBulkStateRequest request, TypeRef<T> type) {
    return this.stateClient.getBulkState(request, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> executeStateTransaction(ExecuteStateTransactionRequest request) {
    return this.stateClient.executeStateTransaction(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveBulkState(SaveStateRequest request) {
    return this.stateClient.saveBulkState(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(DeleteStateRequest request) {
    return this.stateClient.deleteState(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(GetSecretRequest request) {
    return this.secretsClient.getSecret(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, Map<String, String>>> getBulkSecret(GetBulkSecretRequest request) {
    return this.secretsClient.getBulkSecret(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Boolean> tryLock(LockRequest request) {
    return this.lockClient.tryLock(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<UnlockResponseStatus> unlock(UnlockRequest request) {
    return this.lockClient.unlock(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<QueryStateResponse<T>> queryState(QueryStateRequest request, TypeRef<T> type) {
    return this.stateClient.queryState(request, type);
  }

  /**
   * Closes the ManagedChannel for GRPC.
   *
   * @throws IOException on exception.
   * @see io.grpc.ManagedChannel#shutdown()
   */
  @Override
  public void close() throws Exception {
    DaprException.wrap(() -> {
      if (channel != null) {
        channel.close();
      }
      if (httpClient != null) {
        httpClient.close();
      }
      return true;
    }).call();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> shutdown() {
    DaprProtos.ShutdownRequest shutdownRequest = DaprProtos.ShutdownRequest.newBuilder().build();
    return Mono.deferContextual(
        context -> grpcCalls.<Empty>createMono(
            it -> grpcCalls.intercept(context).shutdown(shutdownRequest, it))
    ).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, ConfigurationItem>> getConfiguration(GetConfigurationRequest request) {
    return this.configurationClient.getConfiguration(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<SubscribeConfigurationResponse> subscribeConfiguration(SubscribeConfigurationRequest request) {
    return this.configurationClient.subscribeConfiguration(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<UnsubscribeConfigurationResponse> unsubscribeConfiguration(UnsubscribeConfigurationRequest request) {
    return this.configurationClient.unsubscribeConfiguration(request);
  }

  /**
   * {@inheritDoc}
   */
  public Mono<Void> scheduleJob(ScheduleJobRequest scheduleJobRequest) {
    return this.jobsClient.scheduleJob(scheduleJobRequest);
  }

  /**
   * {@inheritDoc}
   */
  public Mono<GetJobResponse> getJob(GetJobRequest getJobRequest) {
    return this.jobsClient.getJob(getJobRequest);
  }

  /**
   * {@inheritDoc}
   */
  public Mono<Void> deleteJob(DeleteJobRequest deleteJobRequest) {
    return this.jobsClient.deleteJob(deleteJobRequest);
  }

  @Override
  public Mono<DaprMetadata> getMetadata() {
    return this.metadataClient.getMetadata();
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated(forRemoval = true)
  @Override
  public Mono<ConversationResponse> converse(ConversationRequest conversationRequest) {
    return this.conversationClient.converse(conversationRequest);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<ConversationResponseAlpha2> converseAlpha2(ConversationRequestAlpha2 conversationRequestAlpha2) {
    return this.conversationClient.converseAlpha2(conversationRequestAlpha2);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<byte[]> encrypt(EncryptRequestAlpha1 request) {
    return this.cryptoClient.encrypt(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<byte[]> decrypt(DecryptRequestAlpha1 request) {
    return this.cryptoClient.decrypt(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteBinaryFile(String storeName, String fileName) {
    return this.binaryFileClient.deleteBinaryFile(storeName, fileName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<byte[]> getBinaryFile(String storeName, String fileName) {
    return this.binaryFileClient.getBinaryFile(storeName, fileName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> getBinaryFileBytes(String storeName, String fileName) {
    return this.binaryFileClient.getBinaryFileBytes(storeName, fileName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> setBinaryFile(String storeName, String fileName, byte[] content, boolean overwrite) {
    return this.binaryFileClient.setBinaryFile(storeName, fileName, content, overwrite);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> setBinaryFile(SetBinaryFileRequestAlpha1 request) {
    return this.binaryFileClient.setBinaryFile(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> createVectorCollection(CreateVectorCollectionRequestAlpha1 request) {
    return vectorClient.createVectorCollection(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<GetVectorCollectionResponseAlpha1> getVectorCollection(String storeName, String collection) {
    return vectorClient.getVectorCollection(storeName, collection, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<GetVectorCollectionResponseAlpha1> getVectorCollection(
      String storeName, String collection, Map<String, String> metadata) {
    return vectorClient.getVectorCollection(storeName, collection, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<String>> listVectorCollections(String storeName) {
    return vectorClient.listVectorCollections(storeName, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<String>> listVectorCollections(String storeName, Map<String, String> metadata) {
    return vectorClient.listVectorCollections(storeName, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteVectorCollection(String storeName, String collection) {
    return vectorClient.deleteVectorCollection(storeName, collection, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteVectorCollection(String storeName, String collection, Map<String, String> metadata) {
    return vectorClient.deleteVectorCollection(storeName, collection, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<UpsertVectorsResponseAlpha1> upsertVectors(UpsertVectorsRequestAlpha1 request) {
    return vectorClient.upsertVectors(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<VectorRecord>> getVectors(GetVectorsRequestAlpha1 request) {
    return vectorClient.getVectors(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<IndexAck> deleteVectors(DeleteVectorsRequestAlpha1 request) {
    return vectorClient.deleteVectors(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<QueryVectorsResponseAlpha1> queryVectors(QueryVectorsRequestAlpha1 request) {
    return vectorClient.queryVectors(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<BatchQueryVectorsResultAlpha1>> batchQueryVectors(BatchQueryVectorsRequestAlpha1 request) {
    return vectorClient.batchQueryVectors(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> createSearchIndex(String storeName, String index) {
    return searchClient.createSearchIndex(storeName, index, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> createSearchIndex(String storeName, String index, Map<String, String> metadata) {
    return searchClient.createSearchIndex(storeName, index, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<GetSearchIndexResponseAlpha1> getSearchIndex(String storeName, String index) {
    return searchClient.getSearchIndex(storeName, index, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<GetSearchIndexResponseAlpha1> getSearchIndex(
      String storeName, String index, Map<String, String> metadata) {
    return searchClient.getSearchIndex(storeName, index, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<String>> listSearchIndexes(String storeName) {
    return searchClient.listSearchIndexes(storeName, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<String>> listSearchIndexes(String storeName, Map<String, String> metadata) {
    return searchClient.listSearchIndexes(storeName, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteSearchIndex(String storeName, String index) {
    return searchClient.deleteSearchIndex(storeName, index, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteSearchIndex(String storeName, String index, Map<String, String> metadata) {
    return searchClient.deleteSearchIndex(storeName, index, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<IndexSearchDocumentsResponseAlpha1> indexSearchDocuments(IndexSearchDocumentsRequestAlpha1 request) {
    return searchClient.indexSearchDocuments(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<SearchDocument>> getSearchDocuments(GetSearchDocumentsRequestAlpha1 request) {
    return searchClient.getSearchDocuments(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<IndexAck> deleteSearchDocuments(DeleteSearchDocumentsRequestAlpha1 request) {
    return searchClient.deleteSearchDocuments(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<SearchResponseAlpha1> search(SearchRequestAlpha1 request) {
    return searchClient.search(request);
  }
}
