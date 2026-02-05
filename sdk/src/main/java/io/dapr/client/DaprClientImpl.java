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

import com.google.common.base.Strings;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.dapr.client.domain.ActorMetadata;
import io.dapr.client.domain.AppConnectionPropertiesHealthMetadata;
import io.dapr.client.domain.AppConnectionPropertiesMetadata;
import io.dapr.client.domain.AssistantMessage;
import io.dapr.client.domain.BulkPublishEntry;
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.BulkPublishResponseFailedEntry;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.ComponentMetadata;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.ConstantFailurePolicy;
import io.dapr.client.domain.ConversationInput;
import io.dapr.client.domain.ConversationInputAlpha2;
import io.dapr.client.domain.ConversationMessage;
import io.dapr.client.domain.ConversationMessageContent;
import io.dapr.client.domain.ConversationOutput;
import io.dapr.client.domain.ConversationRequest;
import io.dapr.client.domain.ConversationRequestAlpha2;
import io.dapr.client.domain.ConversationResponse;
import io.dapr.client.domain.ConversationResponseAlpha2;
import io.dapr.client.domain.ConversationResultAlpha2;
import io.dapr.client.domain.ConversationResultChoices;
import io.dapr.client.domain.ConversationResultCompletionUsage;
import io.dapr.client.domain.ConversationResultCompletionUsageDetails;
import io.dapr.client.domain.ConversationResultMessage;
import io.dapr.client.domain.ConversationResultPromptUsageDetails;
import io.dapr.client.domain.ConversationToolCalls;
import io.dapr.client.domain.ConversationToolCallsOfFunction;
import io.dapr.client.domain.ConversationTools;
import io.dapr.client.domain.ConversationToolsFunction;
import io.dapr.client.domain.DaprMetadata;
import io.dapr.client.domain.DecryptRequestAlpha1;
import io.dapr.client.domain.DeleteJobRequest;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.DropFailurePolicy;
import io.dapr.client.domain.EncryptRequestAlpha1;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.FailurePolicy;
import io.dapr.client.domain.FailurePolicyType;
import io.dapr.client.domain.GetBulkSecretRequest;
import io.dapr.client.domain.GetBulkStateRequest;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.GetJobRequest;
import io.dapr.client.domain.GetJobResponse;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.HttpEndpointMetadata;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.JobSchedule;
import io.dapr.client.domain.LockRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.RuleMetadata;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.ScheduleJobRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.client.domain.SubscriptionMetadata;
import io.dapr.client.domain.ToolMessage;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.client.domain.UnlockRequest;
import io.dapr.client.domain.UnlockResponseStatus;
import io.dapr.client.domain.UnsubscribeConfigurationRequest;
import io.dapr.client.domain.UnsubscribeConfigurationResponse;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.exceptions.DaprException;
import io.dapr.internal.exceptions.DaprHttpException;
import io.dapr.internal.grpc.DaprClientGrpcInterceptors;
import io.dapr.internal.resiliency.RetryPolicy;
import io.dapr.internal.resiliency.TimeoutPolicy;
import io.dapr.internal.subscription.EventSubscriberStreamObserver;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.DefaultContentTypeConverter;
import io.dapr.utils.TypeRef;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprAiProtos;
import io.dapr.v1.DaprBindingsProtos;
import io.dapr.v1.DaprConfigurationProtos;
import io.dapr.v1.DaprCryptoProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprJobsProtos;
import io.dapr.v1.DaprLockProtos;
import io.dapr.v1.DaprMetadataProtos;
import io.dapr.v1.DaprProtos;
import io.dapr.v1.DaprPubsubProtos;
import io.dapr.v1.DaprSecretProtos;
import io.dapr.v1.DaprStateProtos;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.context.ContextView;
import reactor.util.retry.Retry;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
   * The retry policy.
   */
  private final RetryPolicy retryPolicy;

  /**
   * The async gRPC stub.
   */
  private final DaprGrpc.DaprStub asyncStub;

  /**
   * The HTTP client to be used for healthz and HTTP service invocation only.
   *
   * @see io.dapr.client.DaprHttp
   */
  private final DaprHttp httpClient;

  private final DaprClientGrpcInterceptors grpcInterceptors;

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
    this.asyncStub = asyncStub;
    this.httpClient = httpClient;
    this.retryPolicy = retryPolicy;
    this.grpcInterceptors = new DaprClientGrpcInterceptors(daprApiToken, timeoutPolicy);
    this.logger = LoggerFactory.getLogger(DaprClientImpl.class);
  }

  private CommonProtos.StateOptions.StateConsistency getGrpcStateConsistency(StateOptions options) {
    switch (options.getConsistency()) {
      case EVENTUAL:
        return CommonProtos.StateOptions.StateConsistency.CONSISTENCY_EVENTUAL;
      case STRONG:
        return CommonProtos.StateOptions.StateConsistency.CONSISTENCY_STRONG;
      default:
        throw new IllegalArgumentException("Missing Consistency mapping to gRPC Consistency enum");
    }
  }

  private CommonProtos.StateOptions.StateConcurrency getGrpcStateConcurrency(StateOptions options) {
    switch (options.getConcurrency()) {
      case FIRST_WRITE:
        return CommonProtos.StateOptions.StateConcurrency.CONCURRENCY_FIRST_WRITE;
      case LAST_WRITE:
        return CommonProtos.StateOptions.StateConcurrency.CONCURRENCY_LAST_WRITE;
      default:
        throw new IllegalArgumentException("Missing StateConcurrency mapping to gRPC Concurrency enum");
    }
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
  public Mono<Void> publishEvent(PublishEventRequest request) {
    try {
      String pubsubName = request.getPubsubName();
      String topic = request.getTopic();
      Object data = request.getData();
      DaprPubsubProtos.PublishEventRequest.Builder envelopeBuilder = DaprPubsubProtos.PublishEventRequest.newBuilder()
          .setTopic(topic)
          .setPubsubName(pubsubName)
          .setData(ByteString.copyFrom(objectSerializer.serialize(data)));

      // Content-type can be overwritten on a per-request basis.
      // It allows CloudEvents to be handled differently, for example.
      String contentType = request.getContentType();
      if (contentType == null || contentType.isEmpty()) {
        contentType = objectSerializer.getContentType();
      }
      envelopeBuilder.setDataContentType(contentType);

      Map<String, String> metadata = request.getMetadata();
      if (metadata != null) {
        envelopeBuilder.putAllMetadata(metadata);
      }

      return Mono.deferContextual(
          context ->
              this.<Empty>createMono(
                  it -> intercept(context, asyncStub).publishEvent(envelopeBuilder.build(), it)
              )
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<BulkPublishResponse<T>> publishEvents(BulkPublishRequest<T> request) {
    try {
      String pubsubName = request.getPubsubName();
      String topic = request.getTopic();
      DaprPubsubProtos.BulkPublishRequest.Builder envelopeBuilder = DaprPubsubProtos.BulkPublishRequest.newBuilder();
      envelopeBuilder.setTopic(topic);
      envelopeBuilder.setPubsubName(pubsubName);

      if (Strings.isNullOrEmpty(pubsubName) || Strings.isNullOrEmpty(topic)) {
        throw new IllegalArgumentException("pubsubName and topic name cannot be null or empty");
      }

      for (BulkPublishEntry<?> entry : request.getEntries()) {
        Object event = entry.getEvent();
        byte[] data;
        String contentType = entry.getContentType();
        try {
          // Serialize event into bytes
          if (!Strings.isNullOrEmpty(contentType) && objectSerializer instanceof DefaultObjectSerializer) {
            // If content type is given by user and default object serializer is used
            data = DefaultContentTypeConverter.convertEventToBytesForGrpc(event, contentType);
          } else {
            // perform the serialization as per user given input of serializer
            // this is also the case when content type is empty

            data = objectSerializer.serialize(event);

            if (Strings.isNullOrEmpty(contentType)) {
              // Only override content type if not given in input by user
              contentType = objectSerializer.getContentType();
            }
          }
        } catch (IOException ex) {
          throw DaprException.propagate(ex);
        }

        DaprPubsubProtos.BulkPublishRequestEntry.Builder reqEntryBuilder = DaprPubsubProtos.BulkPublishRequestEntry
            .newBuilder()
            .setEntryId(entry.getEntryId())
            .setEvent(ByteString.copyFrom(data))
            .setContentType(contentType);
        Map<String, String> metadata = entry.getMetadata();
        if (metadata != null) {
          reqEntryBuilder.putAllMetadata(metadata);
        }
        envelopeBuilder.addEntries(reqEntryBuilder.build());
      }

      // Set metadata if available
      Map<String, String> metadata = request.getMetadata();
      if (metadata != null) {
        envelopeBuilder.putAllMetadata(metadata);
      }

      Map<String, BulkPublishEntry<T>> entryMap = new HashMap<>();
      for (BulkPublishEntry<T> entry : request.getEntries()) {
        entryMap.put(entry.getEntryId(), entry);
      }
      return Mono.deferContextual(
          context ->
              this.<DaprPubsubProtos.BulkPublishResponse>createMono(
                  it -> intercept(context, asyncStub).bulkPublishEventAlpha1(envelopeBuilder.build(), it)
              )
      ).map(
          it -> {
            List<BulkPublishResponseFailedEntry<T>> entries = new ArrayList<>();
            for (DaprPubsubProtos.BulkPublishResponseFailedEntry entry : it.getFailedEntriesList()) {
              BulkPublishResponseFailedEntry<T> domainEntry = new BulkPublishResponseFailedEntry<T>(
                  entryMap.get(entry.getEntryId()),
                  entry.getError());
              entries.add(domainEntry);
            }
            if (entries.size() > 0) {
              return new BulkPublishResponse<>(entries);
            }
            return new BulkPublishResponse<>();
          }
      );
    } catch (RuntimeException ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Subscription subscribeToEvents(
      String pubsubName, String topic, SubscriptionListener<T> listener, TypeRef<T> type) {
    DaprPubsubProtos.SubscribeTopicEventsRequestInitialAlpha1 initialRequest =
        DaprPubsubProtos.SubscribeTopicEventsRequestInitialAlpha1.newBuilder()
            .setTopic(topic)
            .setPubsubName(pubsubName)
            .build();
    DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 request =
        DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1.newBuilder()
            .setInitialRequest(initialRequest)
            .build();
    return buildSubscription(listener, type, request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Flux<T> subscribeToEvents(String pubsubName, String topic, TypeRef<T> type) {
    return subscribeToEvents(pubsubName, topic, type, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Flux<T> subscribeToEvents(String pubsubName, String topic, TypeRef<T> type, Map<String, String> metadata) {
    DaprPubsubProtos.SubscribeTopicEventsRequestInitialAlpha1.Builder initialRequestBuilder =
        DaprPubsubProtos.SubscribeTopicEventsRequestInitialAlpha1.newBuilder()
            .setTopic(topic)
            .setPubsubName(pubsubName);

    if (metadata != null && !metadata.isEmpty()) {
      initialRequestBuilder.putAllMetadata(metadata);
    }

    DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 request =
        DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1.newBuilder()
            .setInitialRequest(initialRequestBuilder.build())
            .build();

    return Flux.create(sink -> {
      DaprGrpc.DaprStub interceptedStub = this.grpcInterceptors.intercept(this.asyncStub);
      EventSubscriberStreamObserver<T> eventSubscriber = new EventSubscriberStreamObserver<>(
          interceptedStub,
          sink,
          type,
          this.objectSerializer
      );
      StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1> requestStream = eventSubscriber.start(request);

      // Cleanup when Flux is cancelled or completed
      sink.onDispose(() -> {
        try {
          requestStream.onCompleted();
        } catch (Exception e) {
          logger.debug("Completing the subscription stream resulted in an error: {}", e.getMessage());
        }
      });
    }, FluxSink.OverflowStrategy.BUFFER);
  }

  @Nonnull
  private <T> Subscription<T> buildSubscription(
      SubscriptionListener<T> listener,
      TypeRef<T> type,
      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 request) {
    var interceptedStub = this.grpcInterceptors.intercept(this.asyncStub);
    Subscription<T> subscription = new Subscription<>(interceptedStub, request, listener, response -> {
      if (response.getEventMessage() == null) {
        return null;
      }

      var message = response.getEventMessage();
      if ((message.getPubsubName() == null) || message.getPubsubName().isEmpty()) {
        return null;
      }

      try {
        CloudEvent<T> cloudEvent = new CloudEvent<>();
        T object = null;
        if (type != null) {
          object = DaprClientImpl.this.objectSerializer.deserialize(message.getData().toByteArray(), type);
        }
        cloudEvent.setData(object);
        cloudEvent.setDatacontenttype(message.getDataContentType());
        cloudEvent.setId(message.getId());
        cloudEvent.setTopic(message.getTopic());
        cloudEvent.setSpecversion(message.getSpecVersion());
        cloudEvent.setType(message.getType());
        cloudEvent.setPubsubName(message.getPubsubName());
        return cloudEvent;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    subscription.start();
    return subscription;
  }

  @Override
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

      final Map<String, String> headers = new HashMap<>();
      headers.putAll(httpExtension.getHeaders());
      if (metadata != null) {
        headers.putAll(metadata);
      }
      byte[] serializedRequestBody = objectSerializer.serialize(request);
      if (contentType != null && !contentType.isEmpty()) {
        headers.put(io.dapr.client.domain.Metadata.CONTENT_TYPE, contentType);
      } else {
        headers.put(io.dapr.client.domain.Metadata.CONTENT_TYPE, objectSerializer.getContentType());
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
          context -> this.<DaprBindingsProtos.InvokeBindingResponse>createMono(
              responseMetadata,
              it -> intercept(context, asyncStub, m -> responseMetadata.merge(m)).invokeBinding(envelope, it)
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
    try {
      final String stateStoreName = request.getStoreName();
      final String key = request.getKey();
      final StateOptions options = request.getStateOptions();
      final Map<String, String> metadata = request.getMetadata();

      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }
      DaprStateProtos.GetStateRequest.Builder builder = DaprStateProtos.GetStateRequest.newBuilder()
          .setStoreName(stateStoreName)
          .setKey(key);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      if (options != null && options.getConsistency() != null) {
        builder.setConsistency(getGrpcStateConsistency(options));
      }

      DaprStateProtos.GetStateRequest envelope = builder.build();

      return Mono.deferContextual(
          context ->
              this.<DaprStateProtos.GetStateResponse>createMono(
                  it -> intercept(context, asyncStub).getState(envelope, it)
              )
      ).map(
          it -> {
            try {
              return buildStateKeyValue(it, key, options, type);
            } catch (IOException ex) {
              throw DaprException.propagate(ex);
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
  public <T> Mono<List<State<T>>> getBulkState(GetBulkStateRequest request, TypeRef<T> type) {
    try {
      final String stateStoreName = request.getStoreName();
      final List<String> keys = request.getKeys();
      final int parallelism = request.getParallelism();
      final Map<String, String> metadata = request.getMetadata();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if (keys == null || keys.isEmpty()) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }

      if (parallelism < 0) {
        throw new IllegalArgumentException("Parallelism cannot be negative.");
      }
      DaprStateProtos.GetBulkStateRequest.Builder builder = DaprStateProtos.GetBulkStateRequest.newBuilder()
          .setStoreName(stateStoreName)
          .addAllKeys(keys)
          .setParallelism(parallelism);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprStateProtos.GetBulkStateRequest envelope = builder.build();

      return Mono.deferContextual(
          context -> this.<DaprStateProtos.GetBulkStateResponse>createMono(it -> intercept(context, asyncStub)
              .getBulkState(envelope, it)
          )
      ).map(
          it ->
              it
                  .getItemsList()
                  .stream()
                  .map(b -> {
                    try {
                      return buildStateKeyValue(b, type);
                    } catch (Exception e) {
                      throw DaprException.propagate(e);
                    }
                  })
                  .collect(Collectors.toList())
      );
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private <T> State<T> buildStateKeyValue(
      DaprStateProtos.BulkStateItem item,
      TypeRef<T> type) throws IOException {
    String key = item.getKey();
    String error = item.getError();
    if (!Strings.isNullOrEmpty(error)) {
      return new State<>(key, error);
    }

    String etag = item.getEtag();
    if (etag.equals("")) {
      etag = null;
    }

    T value = null;
    if (type != null) {
      ByteString payload = item.getData();
      byte[] data = payload == null ? null : payload.toByteArray();
      value = stateSerializer.deserialize(data, type);
    }

    return new State<>(key, value, etag, item.getMetadataMap(), null);
  }

  private <T> State<T> buildStateKeyValue(
      DaprStateProtos.GetStateResponse response,
      String requestedKey,
      StateOptions stateOptions,
      TypeRef<T> type) throws IOException {
    ByteString payload = response.getData();
    byte[] data = payload == null ? null : payload.toByteArray();
    T value = null;
    if (type != null) {
      value = stateSerializer.deserialize(data, type);
    }

    String etag = response.getEtag();
    if (etag.equals("")) {
      etag = null;
    }
    return new State<>(requestedKey, value, etag, response.getMetadataMap(), stateOptions);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> executeStateTransaction(ExecuteStateTransactionRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final List<TransactionalStateOperation<?>> operations = request.getOperations();
      final Map<String, String> metadata = request.getMetadata();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      DaprStateProtos.ExecuteStateTransactionRequest.Builder builder = DaprStateProtos.ExecuteStateTransactionRequest
          .newBuilder();
      builder.setStoreName(stateStoreName);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      for (TransactionalStateOperation<?> operation : operations) {
        DaprStateProtos.TransactionalStateOperation.Builder operationBuilder = DaprStateProtos
            .TransactionalStateOperation
            .newBuilder();
        operationBuilder.setOperationType(operation.getOperation().toString().toLowerCase());
        operationBuilder.setRequest(buildStateRequest(operation.getRequest()).build());
        builder.addOperations(operationBuilder.build());
      }
      DaprStateProtos.ExecuteStateTransactionRequest req = builder.build();

      return Mono.deferContextual(
          context -> this.<Empty>createMono(it -> intercept(context, asyncStub)
              .executeStateTransaction(req, it))
      ).then();
    } catch (Exception e) {
      return DaprException.wrapMono(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveBulkState(SaveStateRequest request) {
    try {
      final String stateStoreName = request.getStoreName();
      final List<State<?>> states = request.getStates();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      DaprStateProtos.SaveStateRequest.Builder builder = DaprStateProtos.SaveStateRequest.newBuilder();
      builder.setStoreName(stateStoreName);
      for (State<?> state : states) {
        builder.addStates(buildStateRequest(state).build());
      }
      DaprStateProtos.SaveStateRequest req = builder.build();

      return Mono.deferContextual(
          context -> this.<Empty>createMono(it -> intercept(context, asyncStub).saveState(req, it))
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private <T> CommonProtos.StateItem.Builder buildStateRequest(State<T> state) throws IOException {
    byte[] bytes = stateSerializer.serialize(state.getValue());

    CommonProtos.StateItem.Builder stateBuilder = CommonProtos.StateItem.newBuilder();
    if (state.getEtag() != null) {
      stateBuilder.setEtag(CommonProtos.Etag.newBuilder().setValue(state.getEtag()).build());
    }
    if (state.getMetadata() != null) {
      stateBuilder.putAllMetadata(state.getMetadata());
    }
    if (bytes != null) {
      stateBuilder.setValue(ByteString.copyFrom(bytes));
    }
    stateBuilder.setKey(state.getKey());
    CommonProtos.StateOptions.Builder optionBuilder = null;
    if (state.getOptions() != null) {
      StateOptions options = state.getOptions();
      optionBuilder = CommonProtos.StateOptions.newBuilder();
      if (options.getConcurrency() != null) {
        optionBuilder.setConcurrency(getGrpcStateConcurrency(options));
      }
      if (options.getConsistency() != null) {
        optionBuilder.setConsistency(getGrpcStateConsistency(options));
      }
    }
    if (optionBuilder != null) {
      stateBuilder.setOptions(optionBuilder.build());
    }
    return stateBuilder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(DeleteStateRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final String key = request.getKey();
      final StateOptions options = request.getStateOptions();
      final String etag = request.getEtag();
      final Map<String, String> metadata = request.getMetadata();

      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }

      CommonProtos.StateOptions.Builder optionBuilder = null;
      if (options != null) {
        optionBuilder = CommonProtos.StateOptions.newBuilder();
        if (options.getConcurrency() != null) {
          optionBuilder.setConcurrency(getGrpcStateConcurrency(options));
        }
        if (options.getConsistency() != null) {
          optionBuilder.setConsistency(getGrpcStateConsistency(options));
        }
      }
      DaprStateProtos.DeleteStateRequest.Builder builder = DaprStateProtos.DeleteStateRequest.newBuilder()
          .setStoreName(stateStoreName)
          .setKey(key);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      if (etag != null) {
        builder.setEtag(CommonProtos.Etag.newBuilder().setValue(etag).build());
      }

      if (optionBuilder != null) {
        builder.setOptions(optionBuilder.build());
      }

      DaprStateProtos.DeleteStateRequest req = builder.build();

      return Mono.deferContextual(
          context -> this.<Empty>createMono(it -> intercept(context, asyncStub).deleteState(req, it))
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(GetSecretRequest request) {
    String secretStoreName = request.getStoreName();
    String key = request.getKey();
    Map<String, String> metadata = request.getMetadata();
    try {
      if ((secretStoreName == null) || (secretStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret key cannot be null or empty.");
      }
    } catch (Exception e) {
      return DaprException.wrapMono(e);
    }

    DaprSecretProtos.GetSecretRequest.Builder requestBuilder = DaprSecretProtos.GetSecretRequest.newBuilder()
        .setStoreName(secretStoreName)
        .setKey(key);

    if (metadata != null) {
      requestBuilder.putAllMetadata(metadata);
    }
    DaprSecretProtos.GetSecretRequest req = requestBuilder.build();

    return Mono.deferContextual(
        context -> this.<DaprSecretProtos.GetSecretResponse>createMono(
            it -> intercept(context, asyncStub).getSecret(req, it))
    ).map(DaprSecretProtos.GetSecretResponse::getDataMap);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, Map<String, String>>> getBulkSecret(GetBulkSecretRequest request) {
    try {
      final String storeName = request.getStoreName();
      final Map<String, String> metadata = request.getMetadata();
      if ((storeName == null) || (storeName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret store name cannot be null or empty.");
      }

      DaprSecretProtos.GetBulkSecretRequest.Builder builder = DaprSecretProtos.GetBulkSecretRequest.newBuilder()
          .setStoreName(storeName);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprSecretProtos.GetBulkSecretRequest envelope = builder.build();

      return Mono.deferContextual(
          context ->
              this.<DaprSecretProtos.GetBulkSecretResponse>createMono(
                  it -> intercept(context, asyncStub).getBulkSecret(envelope, it)
              )
      ).map(it -> {
        Map<String, DaprSecretProtos.SecretResponse> secretsMap = it.getDataMap();
        if (secretsMap == null) {
          return Collections.emptyMap();
        }
        return secretsMap
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, s -> s.getValue().getSecretsMap()));
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Boolean> tryLock(LockRequest request) {
    try {
      final String stateStoreName = request.getStoreName();
      final String resourceId = request.getResourceId();
      final String lockOwner = request.getLockOwner();
      final Integer expiryInSeconds = request.getExpiryInSeconds();

      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if (resourceId == null || resourceId.isEmpty()) {
        throw new IllegalArgumentException("ResourceId cannot be null or empty.");
      }
      if (lockOwner == null || lockOwner.isEmpty()) {
        throw new IllegalArgumentException("LockOwner cannot be null or empty.");
      }
      if (expiryInSeconds < 0) {
        throw new IllegalArgumentException("ExpiryInSeconds cannot be negative.");
      }

      DaprLockProtos.TryLockRequest.Builder builder = DaprLockProtos.TryLockRequest.newBuilder()
              .setStoreName(stateStoreName)
              .setResourceId(resourceId)
              .setLockOwner(lockOwner)
              .setExpiryInSeconds(expiryInSeconds);

      DaprLockProtos.TryLockRequest tryLockRequest = builder.build();

      return Mono.deferContextual(
          context -> this.<DaprLockProtos.TryLockResponse>createMono(
                      it -> intercept(context, asyncStub).tryLockAlpha1(tryLockRequest, it)
              )
      ).flatMap(response -> {
        try {
          return Mono.just(response.getSuccess());
        } catch (Exception ex) {
          return DaprException.wrapMono(ex);
        }
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<UnlockResponseStatus> unlock(UnlockRequest request) {
    try {
      final String stateStoreName = request.getStoreName();
      final String resourceId = request.getResourceId();
      final String lockOwner = request.getLockOwner();

      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if (resourceId == null || resourceId.isEmpty()) {
        throw new IllegalArgumentException("ResourceId cannot be null or empty.");
      }
      if (lockOwner == null || lockOwner.isEmpty()) {
        throw new IllegalArgumentException("LockOwner cannot be null or empty.");
      }

      DaprLockProtos.UnlockRequest.Builder builder = DaprLockProtos.UnlockRequest.newBuilder()
              .setStoreName(stateStoreName)
              .setResourceId(resourceId)
              .setLockOwner(lockOwner);

      DaprLockProtos.UnlockRequest unlockRequest = builder.build();

      return Mono.deferContextual(
          context -> this.<DaprLockProtos.UnlockResponse>createMono(
                      it -> intercept(context, asyncStub).unlockAlpha1(unlockRequest, it)
              )
      ).flatMap(response -> {
        try {
          return Mono.just(UnlockResponseStatus.valueOf(response.getStatus().getNumber()));
        } catch (Exception ex) {
          return DaprException.wrapMono(ex);
        }
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<QueryStateResponse<T>> queryState(QueryStateRequest request, TypeRef<T> type) {
    try {
      if (request == null) {
        throw new IllegalArgumentException("Query state request cannot be null.");
      }
      final String storeName = request.getStoreName();
      final Map<String, String> metadata = request.getMetadata();
      if ((storeName == null) || (storeName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }

      String queryString;
      if (request.getQuery() != null) {
        queryString = JSON_REQUEST_MAPPER.writeValueAsString(request.getQuery());
      } else if (request.getQueryString() != null) {
        queryString = request.getQueryString();
      } else {
        throw new IllegalArgumentException("Both query and queryString fields are not set.");
      }

      DaprStateProtos.QueryStateRequest.Builder builder = DaprStateProtos.QueryStateRequest.newBuilder()
          .setStoreName(storeName)
          .setQuery(queryString);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprStateProtos.QueryStateRequest envelope = builder.build();

      return Mono.deferContextual(
          context -> this.<DaprStateProtos.QueryStateResponse>createMono(
              it -> intercept(context, asyncStub).queryStateAlpha1(envelope, it)
          )
      ).map(
          it -> {
            Map<String, String> resultMeta = it.getMetadataMap();
            String token = it.getToken();
            List<QueryStateItem<T>> res = it.getResultsList()
                .stream()
                .map(v -> {
                  try {
                    return buildQueryStateKeyValue(v, type);
                  } catch (Exception e) {
                    throw DaprException.propagate(e);
                  }
                })
                .collect(Collectors.toList());
            return new QueryStateResponse<>(res, token).setMetadata(metadata);
          });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private <T> QueryStateItem<T> buildQueryStateKeyValue(
      DaprStateProtos.QueryStateItem item,
      TypeRef<T> type) throws IOException {
    String key = item.getKey();
    String error = item.getError();
    if (!Strings.isNullOrEmpty(error)) {
      return new QueryStateItem<>(key, null, error);
    }
    ByteString payload = item.getData();
    byte[] data = payload == null ? null : payload.toByteArray();
    T value = null;
    if (type != null) {
      value = stateSerializer.deserialize(data, type);
    }

    String etag = item.getEtag();
    if (etag.equals("")) {
      etag = null;
    }
    return new QueryStateItem<>(key, value, etag);
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
        context -> this.<Empty>createMono(
            it -> intercept(context, asyncStub).shutdown(shutdownRequest, it))
    ).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, ConfigurationItem>> getConfiguration(GetConfigurationRequest request) {
    try {
      final String configurationStoreName = request.getStoreName();
      final Map<String, String> metadata = request.getMetadata();
      final List<String> keys = request.getKeys();
      if ((configurationStoreName == null) || (configurationStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Configuration Store Name cannot be null or empty.");
      }

      DaprConfigurationProtos.GetConfigurationRequest.Builder builder = DaprConfigurationProtos.GetConfigurationRequest
          .newBuilder()
          .setStoreName(configurationStoreName).addAllKeys(keys);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprConfigurationProtos.GetConfigurationRequest envelope = builder.build();
      return this.getConfiguration(envelope);

    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private Mono<Map<String, ConfigurationItem>> getConfiguration(DaprConfigurationProtos
                                                                    .GetConfigurationRequest envelope) {
    return Mono.deferContextual(
        context ->
            this.<DaprConfigurationProtos.GetConfigurationResponse>createMono(
                it -> intercept(context, asyncStub).getConfiguration(envelope, it)
            )
    ).map(
        it -> {
          Map<String, ConfigurationItem> configMap = new HashMap<>();
          Iterator<Map.Entry<String, CommonProtos.ConfigurationItem>> itr = it.getItems().entrySet().iterator();
          while (itr.hasNext()) {
            Map.Entry<String, CommonProtos.ConfigurationItem> entry = itr.next();
            configMap.put(entry.getKey(), buildConfigurationItem(entry.getValue(), entry.getKey()));
          }
          return Collections.unmodifiableMap(configMap);
        }
    );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<SubscribeConfigurationResponse> subscribeConfiguration(SubscribeConfigurationRequest request) {
    try {
      final String configurationStoreName = request.getStoreName();
      final List<String> keys = request.getKeys();
      final Map<String, String> metadata = request.getMetadata();

      if (configurationStoreName == null || (configurationStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Configuration Store Name can not be null or empty.");
      }

      // keys can and empty list for subscribe all scenario, so we do not need check for empty keys.
      DaprConfigurationProtos.SubscribeConfigurationRequest.Builder builder = DaprConfigurationProtos
          .SubscribeConfigurationRequest
          .newBuilder()
          .setStoreName(configurationStoreName)
          .addAllKeys(keys);

      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprConfigurationProtos.SubscribeConfigurationRequest envelope = builder.build();
      return this.<DaprConfigurationProtos.SubscribeConfigurationResponse>createFlux(
          it -> intercept(null, asyncStub).subscribeConfiguration(envelope, it)
      ).map(
          it -> {
            Map<String, ConfigurationItem> configMap = new HashMap<>();
            Iterator<Map.Entry<String, CommonProtos.ConfigurationItem>> itr = it.getItemsMap().entrySet().iterator();
            while (itr.hasNext()) {
              Map.Entry<String, CommonProtos.ConfigurationItem> entry = itr.next();
              configMap.put(entry.getKey(), buildConfigurationItem(entry.getValue(), entry.getKey()));
            }
            return new SubscribeConfigurationResponse(it.getId(), Collections.unmodifiableMap(configMap));
          }
      );
    } catch (Exception ex) {
      return DaprException.wrapFlux(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<UnsubscribeConfigurationResponse> unsubscribeConfiguration(UnsubscribeConfigurationRequest request) {
    try {
      final String configurationStoreName = request.getStoreName();
      final String id = request.getSubscriptionId();

      if (configurationStoreName == null || (configurationStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Configuration Store Name can not be null or empty.");
      }
      if (id.isEmpty()) {
        throw new IllegalArgumentException("Subscription id can not be null or empty.");
      }
      DaprConfigurationProtos.UnsubscribeConfigurationRequest.Builder builder =
          DaprConfigurationProtos.UnsubscribeConfigurationRequest.newBuilder()
              .setId(id)
              .setStoreName(configurationStoreName);

      DaprConfigurationProtos.UnsubscribeConfigurationRequest envelope = builder.build();

      return this.<DaprConfigurationProtos.UnsubscribeConfigurationResponse>createMono(
          it -> intercept(null, asyncStub).unsubscribeConfiguration(envelope, it)
      ).map(
          it -> new UnsubscribeConfigurationResponse(it.getOk(), it.getMessage())
      );
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  public Mono<Void> scheduleJob(ScheduleJobRequest scheduleJobRequest) {
    try {
      validateScheduleJobRequest(scheduleJobRequest);

      DaprJobsProtos.Job.Builder jobBuilder = DaprJobsProtos.Job.newBuilder();
      jobBuilder.setName(scheduleJobRequest.getName());

      DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
              .withZone(ZoneOffset.UTC);

      if (scheduleJobRequest.getData() != null) {
        jobBuilder.setData(Any.newBuilder()
            .setValue(ByteString.copyFrom(scheduleJobRequest.getData())).build());
      }

      if (scheduleJobRequest.getSchedule() != null) {
        jobBuilder.setSchedule(scheduleJobRequest.getSchedule().getExpression());
      }

      if (scheduleJobRequest.getTtl() != null) {
        jobBuilder.setTtl(iso8601Formatter.format(scheduleJobRequest.getTtl()));
      }

      if (scheduleJobRequest.getRepeats() != null) {
        jobBuilder.setRepeats(scheduleJobRequest.getRepeats());
      }

      if (scheduleJobRequest.getDueTime() != null) {
        jobBuilder.setDueTime(iso8601Formatter.format(scheduleJobRequest.getDueTime()));
      }

      if (scheduleJobRequest.getFailurePolicy() != null) {
        jobBuilder.setFailurePolicy(getJobFailurePolicy(scheduleJobRequest.getFailurePolicy()));
      }


      Mono<DaprJobsProtos.ScheduleJobResponse> scheduleJobResponseMono =
          Mono.deferContextual(context -> this.createMono(
                  it -> intercept(context, asyncStub)
                      .scheduleJobAlpha1(DaprJobsProtos.ScheduleJobRequest.newBuilder()
                                  .setOverwrite(scheduleJobRequest.getOverwrite())
                                  .setJob(jobBuilder.build()).build(), it)
              )
          );

      return scheduleJobResponseMono.then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  public Mono<GetJobResponse> getJob(GetJobRequest getJobRequest) {
    try {
      validateGetJobRequest(getJobRequest);

      Mono<DaprJobsProtos.GetJobResponse> getJobResponseMono =
          Mono.deferContextual(context -> this.createMono(
                  it -> intercept(context, asyncStub)
                      .getJobAlpha1(DaprJobsProtos.GetJobRequest.newBuilder()
                          .setName(getJobRequest.getName()).build(), it)
              )
          );

      return getJobResponseMono.map(response -> {
        DaprJobsProtos.Job job = response.getJob();
        GetJobResponse getJobResponse = null;

        if (job.hasSchedule() && job.hasDueTime()) {
          getJobResponse = new GetJobResponse(job.getName(), JobSchedule.fromString(job.getSchedule()));
          getJobResponse.setDueTime(Instant.parse(job.getDueTime()));
        } else if (job.hasSchedule()) {
          getJobResponse = new GetJobResponse(job.getName(), JobSchedule.fromString(job.getSchedule()));
        } else {
          getJobResponse = new GetJobResponse(job.getName(), Instant.parse(job.getDueTime()));
        }

        if (job.hasFailurePolicy()) {
          getJobResponse.setFailurePolicy(getJobFailurePolicy(job.getFailurePolicy()));
        }

        return getJobResponse
            .setTtl(job.hasTtl() ? Instant.parse(job.getTtl()) : null)
            .setData(job.hasData() ? job.getData().getValue().toByteArray() : null)
            .setRepeat(job.hasRepeats() ? job.getRepeats() : null);
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private FailurePolicy getJobFailurePolicy(CommonProtos.JobFailurePolicy jobFailurePolicy) {
    if (jobFailurePolicy.hasDrop()) {
      return new DropFailurePolicy();
    }

    CommonProtos.JobFailurePolicyConstant jobFailurePolicyConstant = jobFailurePolicy.getConstant();
    if (jobFailurePolicyConstant.hasInterval() && jobFailurePolicyConstant.hasMaxRetries()) {
      return new ConstantFailurePolicy(jobFailurePolicyConstant.getMaxRetries())
              .setDurationBetweenRetries(Duration.of(jobFailurePolicyConstant.getInterval().getNanos(),
                  ChronoUnit.NANOS));
    }

    if (jobFailurePolicyConstant.hasMaxRetries()) {
      return new ConstantFailurePolicy(jobFailurePolicyConstant.getMaxRetries());
    }

    return new ConstantFailurePolicy(
        Duration.of(jobFailurePolicyConstant.getInterval().getNanos(),
            ChronoUnit.NANOS));
  }

  private CommonProtos.JobFailurePolicy getJobFailurePolicy(FailurePolicy failurePolicy) {
    CommonProtos.JobFailurePolicy.Builder jobFailurePolicyBuilder = CommonProtos.JobFailurePolicy.newBuilder();

    if (failurePolicy.getFailurePolicyType() == FailurePolicyType.DROP) {
      jobFailurePolicyBuilder.setDrop(CommonProtos.JobFailurePolicyDrop.newBuilder().build());
      return jobFailurePolicyBuilder.build();
    }

    CommonProtos.JobFailurePolicyConstant.Builder constantPolicyBuilder =
        CommonProtos.JobFailurePolicyConstant.newBuilder();
    ConstantFailurePolicy jobConstantFailurePolicy = (ConstantFailurePolicy)failurePolicy;

    if (jobConstantFailurePolicy.getMaxRetries() != null) {
      constantPolicyBuilder.setMaxRetries(jobConstantFailurePolicy.getMaxRetries());
    }

    if (jobConstantFailurePolicy.getDurationBetweenRetries() != null) {
      constantPolicyBuilder.setInterval(com.google.protobuf.Duration.newBuilder()
          .setNanos(jobConstantFailurePolicy.getDurationBetweenRetries().getNano()).build());
    }

    jobFailurePolicyBuilder.setConstant(constantPolicyBuilder.build());

    return jobFailurePolicyBuilder.build();
  }

  /**
   * {@inheritDoc}
   */
  public Mono<Void> deleteJob(DeleteJobRequest deleteJobRequest) {
    try {
      validateDeleteJobRequest(deleteJobRequest);

      Mono<DaprJobsProtos.DeleteJobResponse> deleteJobResponseMono =
          Mono.deferContextual(context -> this.createMono(
                  it -> intercept(context, asyncStub)
                      .deleteJobAlpha1(DaprJobsProtos.DeleteJobRequest.newBuilder()
                          .setName(deleteJobRequest.getName()).build(), it)
              )
          );

      return deleteJobResponseMono.then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private void validateScheduleJobRequest(ScheduleJobRequest scheduleJobRequest) {
    if (scheduleJobRequest == null) {
      throw new IllegalArgumentException("scheduleJobRequest cannot be null");
    }

    if (scheduleJobRequest.getName() == null || scheduleJobRequest.getName().isEmpty()) {
      throw new IllegalArgumentException("Name in the request cannot be null or empty");
    }

    if (scheduleJobRequest.getSchedule() == null && scheduleJobRequest.getDueTime() == null) {
      throw new IllegalArgumentException("At least one of schedule or dueTime must be provided");
    }
  }

  private void validateGetJobRequest(GetJobRequest getJobRequest) {
    if (getJobRequest == null) {
      throw new IllegalArgumentException("getJobRequest cannot be null");
    }

    if (getJobRequest.getName() == null || getJobRequest.getName().isEmpty()) {
      throw new IllegalArgumentException("Name in the request cannot be null or empty");
    }
  }

  private void validateDeleteJobRequest(DeleteJobRequest deleteJobRequest) {
    if (deleteJobRequest == null) {
      throw new IllegalArgumentException("deleteJobRequest cannot be null");
    }

    if (deleteJobRequest.getName() == null || deleteJobRequest.getName().isEmpty()) {
      throw new IllegalArgumentException("Name in the request cannot be null or empty");
    }
  }

  /**
   * Build a new Configuration Item from provided parameter.
   *
   * @param configurationItem CommonProtos.ConfigurationItem
   * @return io.dapr.client.domain.ConfigurationItem
   */
  private ConfigurationItem buildConfigurationItem(
      CommonProtos.ConfigurationItem configurationItem, String key) {
    return new ConfigurationItem(
        key,
        configurationItem.getValue(),
        configurationItem.getVersion(),
        configurationItem.getMetadataMap());
  }

  /**
   * Populates GRPC client with interceptors for telemetry.
   *
   * @param context Reactor's context.
   * @param client  GRPC client for Dapr.
   * @return Client after adding interceptors.
   */
  private DaprGrpc.DaprStub intercept(ContextView context, DaprGrpc.DaprStub client) {
    return this.grpcInterceptors.intercept(client, context);
  }

  /**
   * Populates GRPC client with interceptors for telemetry.
   *
   * @param context Reactor's context.
   * @param client  GRPC client for Dapr.
   * @param metadataConsumer Consumer of gRPC metadata.
   * @return Client after adding interceptors.
   */
  private DaprGrpc.DaprStub intercept(
      ContextView context, DaprGrpc.DaprStub client, Consumer<Metadata> metadataConsumer) {
    return this.grpcInterceptors.intercept(client, context, metadataConsumer);
  }

  private <T> Mono<T> createMono(Consumer<StreamObserver<T>> consumer) {
    return this.createMono(null, consumer);
  }

  private <T> Mono<T> createMono(Metadata metadata, Consumer<StreamObserver<T>> consumer) {
    return retryPolicy.apply(
        Mono.create(sink -> DaprException.wrap(() -> consumer.accept(
            createStreamObserver(sink, metadata))).run()));
  }

  private <T> Flux<T> createFlux(Consumer<StreamObserver<T>> consumer) {
    return this.createFlux(null, consumer);
  }

  private <T> Flux<T> createFlux(Metadata metadata, Consumer<StreamObserver<T>> consumer) {
    return retryPolicy.apply(
        Flux.create(sink -> DaprException.wrap(() -> consumer.accept(createStreamObserver(sink, metadata))).run()));
  }

  private <T> StreamObserver<T> createStreamObserver(MonoSink<T> sink, Metadata grpcMetadata) {
    return new StreamObserver<T>() {
      @Override
      public void onNext(T value) {
        sink.success(value);
      }

      @Override
      public void onError(Throwable t) {
        sink.error(DaprException.propagate(DaprHttpException.fromGrpcExecutionException(grpcMetadata, t)));
      }

      @Override
      public void onCompleted() {
        sink.success();
      }
    };
  }

  private <T> StreamObserver<T> createStreamObserver(FluxSink<T> sink, final Metadata grpcMetadata) {
    return new StreamObserver<T>() {
      @Override
      public void onNext(T value) {
        sink.next(value);
      }

      @Override
      public void onError(Throwable t) {
        sink.error(DaprException.propagate(DaprHttpException.fromGrpcExecutionException(grpcMetadata, t)));
      }

      @Override
      public void onCompleted() {
        sink.complete();
      }
    };
  }

  @Override
  public Mono<DaprMetadata> getMetadata() {
    DaprMetadataProtos.GetMetadataRequest metadataRequest = DaprMetadataProtos.GetMetadataRequest.newBuilder().build();
    return Mono.deferContextual(
            context -> this.<DaprMetadataProtos.GetMetadataResponse>createMono(
                it -> intercept(context, asyncStub).getMetadata(metadataRequest, it)))
        .map(
            it -> {
              try {
                return buildDaprMetadata(it);
              } catch (IOException ex) {
                throw DaprException.propagate(ex);
              }
            });
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated(forRemoval = true)
  @Override
  public Mono<ConversationResponse> converse(ConversationRequest conversationRequest) {

    try {
      validateConversationRequest(conversationRequest);

      DaprAiProtos.ConversationRequest.Builder protosConversationRequestBuilder = DaprAiProtos.ConversationRequest
          .newBuilder().setTemperature(conversationRequest.getTemperature())
          .setScrubPII(conversationRequest.isScrubPii())
          .setName(conversationRequest.getName());

      if (conversationRequest.getContextId() != null) {
        protosConversationRequestBuilder.setContextID(conversationRequest.getContextId());
      }

      for (ConversationInput input : conversationRequest.getInputs()) {
        if (input.getContent() == null || input.getContent().isEmpty()) {
          throw new IllegalArgumentException("Conversation input content cannot be null or empty.");
        }

        DaprAiProtos.ConversationInput.Builder conversationInputOrBuilder = DaprAiProtos.ConversationInput.newBuilder()
            .setContent(input.getContent())
            .setScrubPII(input.isScrubPii());

        if (input.getRole() != null) {
          conversationInputOrBuilder.setRole(input.getRole().toString());
        }

        protosConversationRequestBuilder.addInputs(conversationInputOrBuilder.build());
      }

      Mono<DaprAiProtos.ConversationResponse> conversationResponseMono = Mono.deferContextual(
          context -> this.createMono(
              it -> intercept(context, asyncStub)
                  .converseAlpha1(protosConversationRequestBuilder.build(), it)
          )
      );

      return conversationResponseMono.map(conversationResponse -> {

        List<ConversationOutput> conversationOutputs = new ArrayList<>();
        for (DaprAiProtos.ConversationResult conversationResult : conversationResponse.getOutputsList()) {
          Map<String, byte[]> parameters = new HashMap<>();
          for (Map.Entry<String, Any> entrySet : conversationResult.getParametersMap().entrySet()) {
            parameters.put(entrySet.getKey(), entrySet.getValue().toByteArray());
          }

          ConversationOutput conversationOutput =
              new ConversationOutput(conversationResult.getResult(), parameters);
          conversationOutputs.add(conversationOutput);
        }

        return new ConversationResponse(conversationResponse.getContextID(), conversationOutputs);
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private void validateConversationRequest(ConversationRequest conversationRequest) {
    if ((conversationRequest.getName() == null) || (conversationRequest.getName().trim().isEmpty())) {
      throw new IllegalArgumentException("LLM name cannot be null or empty.");
    }

    if ((conversationRequest.getInputs() == null) || (conversationRequest
        .getInputs().isEmpty())) {
      throw new IllegalArgumentException("Conversation inputs cannot be null or empty.");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<ConversationResponseAlpha2> converseAlpha2(ConversationRequestAlpha2 conversationRequestAlpha2) {
    try {
      if ((conversationRequestAlpha2.getName() == null) || (conversationRequestAlpha2.getName().trim().isEmpty())) {
        throw new IllegalArgumentException("LLM name cannot be null or empty.");
      }

      if (conversationRequestAlpha2.getInputs() == null || conversationRequestAlpha2.getInputs().isEmpty()) {
        throw new IllegalArgumentException("Conversation Inputs cannot be null or empty.");
      }

      DaprAiProtos.ConversationRequestAlpha2 protoRequest = buildConversationRequestProto(conversationRequestAlpha2);

      Mono<DaprAiProtos.ConversationResponseAlpha2> conversationResponseMono = Mono.deferContextual(
          context -> this.createMono(
              it -> intercept(context, asyncStub).converseAlpha2(protoRequest, it)
          )
      );

      DaprAiProtos.ConversationResponseAlpha2 conversationResponse = conversationResponseMono.block();

      assert conversationResponse != null;

      List<ConversationResultAlpha2> results = buildConversationResults(conversationResponse.getOutputsList());
      return Mono.just(new ConversationResponseAlpha2(conversationResponse.getContextId(), results));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private DaprAiProtos.ConversationRequestAlpha2 buildConversationRequestProto(ConversationRequestAlpha2 request) {
    DaprAiProtos.ConversationRequestAlpha2.Builder builder = DaprAiProtos.ConversationRequestAlpha2
        .newBuilder()
        .setTemperature(request.getTemperature())
        .setScrubPii(request.isScrubPii())
        .setName(request.getName());

    if (request.getContextId() != null) {
      builder.setContextId(request.getContextId());
    }

    if (request.getToolChoice() != null) {
      builder.setToolChoice(request.getToolChoice());
    }


    if (request.getTools() != null) {
      for (ConversationTools tool : request.getTools()) {
        builder.addTools(buildConversationTools(tool));
      }
    }

    if (request.getMetadata() != null) {
      builder.putAllMetadata(request.getMetadata());
    }

    if (request.getParameters() != null) {
      Map<String, Any> parameters = request.getParameters()
          .entrySet().stream()
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> {
                try {
                  return Any.newBuilder().setValue(ByteString.copyFrom(objectSerializer.serialize(e.getValue())))
                      .build();
                } catch (IOException ex) {
                  throw new RuntimeException(ex);
                }
              })
          );
      builder.putAllParameters(parameters);
    }

    for (ConversationInputAlpha2 input : request.getInputs()) {
      DaprAiProtos.ConversationInputAlpha2.Builder inputBuilder = DaprAiProtos.ConversationInputAlpha2
              .newBuilder()
              .setScrubPii(input.isScrubPii());

      if (input.getMessages() != null) {
        for (ConversationMessage message : input.getMessages()) {
          DaprAiProtos.ConversationMessage protoMessage = buildConversationMessage(message);
          inputBuilder.addMessages(protoMessage);
        }
      }

      builder.addInputs(inputBuilder.build());
    }

    if (request.getResponseFormat() != null) {
      Map<String, Value> responseParams = request.getResponseFormat()
          .entrySet().stream()
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> {
                try {
                  return ProtobufValueHelper.toProtobufValue(e.getValue());
                } catch (IOException ex) {
                  throw new RuntimeException(ex);
                }
              }
          ));

      builder.setResponseFormat(Struct.newBuilder().putAllFields(responseParams).build());
    }

    if (request.getPromptCacheRetention() != null) {
      Duration javaDuration = request.getPromptCacheRetention();
      builder.setPromptCacheRetention(
          com.google.protobuf.Duration.newBuilder()
              .setSeconds(javaDuration.getSeconds())
              .setNanos(javaDuration.getNano())
              .build()
      );
    }
    
    return builder.build();
  }

  private DaprAiProtos.ConversationTools buildConversationTools(ConversationTools tool) {
    ConversationToolsFunction function = tool.getFunction();

    DaprAiProtos.ConversationToolsFunction.Builder protoFunction = DaprAiProtos.ConversationToolsFunction.newBuilder()
        .setName(function.getName());

    if (function.getDescription() != null) {
      protoFunction.setDescription(function.getDescription());
    }

    if (function.getParameters() != null) {
      Map<String, Value> functionParams = function.getParameters()
          .entrySet().stream()
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> {
                try {
                  return ProtobufValueHelper.toProtobufValue(e.getValue());
                } catch (IOException ex) {
                  throw new RuntimeException(ex);
                }
              }
          ));

      protoFunction.setParameters(Struct.newBuilder().putAllFields(functionParams).build());
    }

    return DaprAiProtos.ConversationTools.newBuilder().setFunction(protoFunction).build();
  }

  private DaprAiProtos.ConversationMessage buildConversationMessage(ConversationMessage message) {
    DaprAiProtos.ConversationMessage.Builder messageBuilder = DaprAiProtos.ConversationMessage.newBuilder();

    switch (message.getRole()) {
      case TOOL:
        DaprAiProtos.ConversationMessageOfTool.Builder toolMessage =
            DaprAiProtos.ConversationMessageOfTool.newBuilder();
        if (message.getName() != null) {
          toolMessage.setName(message.getName());
        }
        if (message.getContent() != null) {
          toolMessage.addAllContent(getConversationMessageContent(message));
        }
        if (((ToolMessage)message).getToolId() != null) {
          toolMessage.setToolId(((ToolMessage)message).getToolId());
        }
        messageBuilder.setOfTool(toolMessage);
        break;
      case USER:
        DaprAiProtos.ConversationMessageOfUser.Builder userMessage =
            DaprAiProtos.ConversationMessageOfUser.newBuilder();
        if (message.getName() != null) {
          userMessage.setName(message.getName());
        }
        if (message.getContent() != null) {
          userMessage.addAllContent(getConversationMessageContent(message));
        }
        messageBuilder.setOfUser(userMessage);
        break;
      case ASSISTANT:
        DaprAiProtos.ConversationMessageOfAssistant.Builder assistantMessage =
            DaprAiProtos.ConversationMessageOfAssistant.newBuilder();

        if (message.getName() != null) {
          assistantMessage.setName(message.getName());
        }
        if (message.getContent() != null) {
          assistantMessage.addAllContent(getConversationMessageContent(message));
        }
        if (((AssistantMessage)message).getToolCalls() != null) {
          assistantMessage.addAllToolCalls(getConversationToolCalls((AssistantMessage)message));
        }
        messageBuilder.setOfAssistant(assistantMessage);
        break;
      case DEVELOPER:
        DaprAiProtos.ConversationMessageOfDeveloper.Builder developerMessage =
            DaprAiProtos.ConversationMessageOfDeveloper.newBuilder();
        if (message.getName() != null) {
          developerMessage.setName(message.getName());
        }
        if (message.getContent() != null) {
          developerMessage.addAllContent(getConversationMessageContent(message));
        }
        messageBuilder.setOfDeveloper(developerMessage);
        break;
      case SYSTEM:
        DaprAiProtos.ConversationMessageOfSystem.Builder systemMessage =
            DaprAiProtos.ConversationMessageOfSystem.newBuilder();
        if (message.getName() != null) {
          systemMessage.setName(message.getName());
        }
        if (message.getContent() != null) {
          systemMessage.addAllContent(getConversationMessageContent(message));
        }
        messageBuilder.setOfSystem(systemMessage);
        break;
      default:
        throw new IllegalArgumentException("No role of type " + message.getRole() + " found");
    }

    return messageBuilder.build();
  }

  private List<ConversationResultAlpha2> buildConversationResults(
      List<DaprAiProtos.ConversationResultAlpha2> protoResults) {
    List<ConversationResultAlpha2> results = new ArrayList<>();

    for (DaprAiProtos.ConversationResultAlpha2 protoResult : protoResults) {
      List<ConversationResultChoices> choices = new ArrayList<>();

      for (DaprAiProtos.ConversationResultChoices protoChoice : protoResult.getChoicesList()) {
        ConversationResultMessage message = buildConversationResultMessage(protoChoice);
        choices.add(new ConversationResultChoices(protoChoice.getFinishReason(), protoChoice.getIndex(), message));
      }

      results.add(new ConversationResultAlpha2(
          choices,
          protoResult.getModel(),
          getConversationResultCompletionUsage(protoResult))
      );
    }
    
    return results;
  }

  private static ConversationResultCompletionUsage getConversationResultCompletionUsage(
      DaprAiProtos.ConversationResultAlpha2 protoResult) {
    var usage = new ConversationResultCompletionUsage(
        protoResult.getUsage().getCompletionTokens(),
        protoResult.getUsage().getPromptTokens(),
        protoResult.getUsage().getTotalTokens());

    usage.setCompletionTokenDetails(new ConversationResultCompletionUsageDetails(
        protoResult.getUsage().getCompletionTokensDetails().getAcceptedPredictionTokens(),
        protoResult.getUsage().getCompletionTokensDetails().getAudioTokens(),
        protoResult.getUsage().getCompletionTokensDetails().getReasoningTokens(),
        protoResult.getUsage().getCompletionTokensDetails().getRejectedPredictionTokens()));

    usage.setPromptTokenDetails(new ConversationResultPromptUsageDetails(
        protoResult.getUsage().getPromptTokensDetails().getAudioTokens(),
        protoResult.getUsage().getPromptTokensDetails().getCachedTokens()
    ));
    return usage;
  }

  private ConversationResultMessage buildConversationResultMessage(DaprAiProtos.ConversationResultChoices protoChoice) {
    if (!protoChoice.hasMessage()) {
      return null;
    }

    List<ConversationToolCalls> toolCalls = new ArrayList<>();

    for (DaprAiProtos.ConversationToolCalls protoToolCall : protoChoice.getMessage().getToolCallsList()) {
      ConversationToolCallsOfFunction function = null;
      if (protoToolCall.hasFunction()) {
        function = new ConversationToolCallsOfFunction(
            protoToolCall.getFunction().getName(),
            protoToolCall.getFunction().getArguments()
        );
      }

      ConversationToolCalls conversationToolCalls = new ConversationToolCalls(function);
      conversationToolCalls.setId(protoToolCall.getId());

      toolCalls.add(conversationToolCalls);
    }
    
    return new ConversationResultMessage(protoChoice.getMessage().getContent(), toolCalls
    );
  }

  private List<DaprAiProtos.ConversationMessageContent> getConversationMessageContent(
      ConversationMessage conversationMessage) {

    List<DaprAiProtos.ConversationMessageContent> conversationMessageContents = new ArrayList<>();
    for (ConversationMessageContent conversationMessageContent: conversationMessage.getContent()) {
      conversationMessageContents.add(DaprAiProtos.ConversationMessageContent.newBuilder()
          .setText(conversationMessageContent.getText())
          .build());
    }

    return conversationMessageContents;
  }

  private List<DaprAiProtos.ConversationToolCalls> getConversationToolCalls(
      AssistantMessage assistantMessage) {
    List<DaprAiProtos.ConversationToolCalls> conversationToolCalls = new ArrayList<>();
    for (ConversationToolCalls conversationToolCall: assistantMessage.getToolCalls()) {
      DaprAiProtos.ConversationToolCalls.Builder toolCallsBuilder = DaprAiProtos.ConversationToolCalls.newBuilder()
          .setFunction(DaprAiProtos.ConversationToolCallsOfFunction.newBuilder()
                  .setName(conversationToolCall.getFunction().getName())
                  .setArguments(conversationToolCall.getFunction().getArguments())
                  .build());
      if (conversationToolCall.getId() != null) {
        toolCallsBuilder.setId(conversationToolCall.getId());
      }

      conversationToolCalls.add(toolCallsBuilder.build());
    }

    return conversationToolCalls;
  }

  private DaprMetadata buildDaprMetadata(DaprMetadataProtos.GetMetadataResponse response) throws IOException {
    String id = response.getId();
    String runtimeVersion = response.getRuntimeVersion();
    List<String> enabledFeatures = response.getEnabledFeaturesList();
    List<ActorMetadata> actors = getActors(response);
    Map<String, String> attributes = response.getExtendedMetadataMap();
    List<ComponentMetadata> components = getComponents(response);
    List<HttpEndpointMetadata> httpEndpoints = getHttpEndpoints(response);
    List<SubscriptionMetadata> subscriptions = getSubscriptions(response);
    AppConnectionPropertiesMetadata appConnectionProperties = getAppConnectionProperties(response);

    return new DaprMetadata(id, runtimeVersion, enabledFeatures, actors, attributes, components, httpEndpoints,
      subscriptions, appConnectionProperties);
  }

  private List<ActorMetadata> getActors(DaprMetadataProtos.GetMetadataResponse response) {
    DaprMetadataProtos.ActorRuntime actorRuntime = response.getActorRuntime();
    List<DaprMetadataProtos.ActiveActorsCount> activeActorsList = actorRuntime.getActiveActorsList();

    List<ActorMetadata> actors = new ArrayList<>();
    for (DaprMetadataProtos.ActiveActorsCount aac : activeActorsList) {
      actors.add(new ActorMetadata(aac.getType(), aac.getCount()));
    }

    return actors;
  }

  private List<ComponentMetadata> getComponents(DaprMetadataProtos.GetMetadataResponse response) {
    List<DaprMetadataProtos.RegisteredComponents> registeredComponentsList = response.getRegisteredComponentsList();

    List<ComponentMetadata> components = new ArrayList<>();
    for (DaprMetadataProtos.RegisteredComponents rc : registeredComponentsList) {
      components.add(new ComponentMetadata(rc.getName(), rc.getType(), rc.getVersion(), rc.getCapabilitiesList()));
    }

    return components;
  }

  private List<SubscriptionMetadata> getSubscriptions(DaprMetadataProtos.GetMetadataResponse response) {
    List<DaprMetadataProtos.PubsubSubscription> subscriptionsList = response.getSubscriptionsList();

    List<SubscriptionMetadata> subscriptions = new ArrayList<>();
    for (DaprMetadataProtos.PubsubSubscription s : subscriptionsList) {
      List<DaprMetadataProtos.PubsubSubscriptionRule> rulesList = s.getRules().getRulesList();
      List<RuleMetadata> rules = new ArrayList<>();
      for (DaprMetadataProtos.PubsubSubscriptionRule r : rulesList) {
        rules.add(new RuleMetadata(r.getMatch(), r.getPath()));
      }
      subscriptions.add(new SubscriptionMetadata(s.getPubsubName(), s.getTopic(), s.getMetadataMap(), rules,
          s.getDeadLetterTopic()));
    }

    return subscriptions;
  }

  private List<HttpEndpointMetadata> getHttpEndpoints(DaprMetadataProtos.GetMetadataResponse response) {
    List<DaprMetadataProtos.MetadataHTTPEndpoint> httpEndpointsList = response.getHttpEndpointsList();

    List<HttpEndpointMetadata> httpEndpoints = new ArrayList<>();
    for (DaprMetadataProtos.MetadataHTTPEndpoint m : httpEndpointsList) {
      httpEndpoints.add(new HttpEndpointMetadata(m.getName()));
    }

    return httpEndpoints;
  }

  private AppConnectionPropertiesMetadata getAppConnectionProperties(DaprMetadataProtos.GetMetadataResponse response) {
    DaprMetadataProtos.AppConnectionProperties appConnectionProperties = response.getAppConnectionProperties();
    int port = appConnectionProperties.getPort();
    String protocol = appConnectionProperties.getProtocol();
    String channelAddress = appConnectionProperties.getChannelAddress();
    int maxConcurrency = appConnectionProperties.getMaxConcurrency();
    AppConnectionPropertiesHealthMetadata health = getAppConnectionPropertiesHealth(appConnectionProperties);

    return new AppConnectionPropertiesMetadata(port, protocol, channelAddress, maxConcurrency, health);
  }

  private AppConnectionPropertiesHealthMetadata getAppConnectionPropertiesHealth(
      DaprMetadataProtos.AppConnectionProperties appConnectionProperties) {
    if (!appConnectionProperties.hasHealth()) {
      return null;
    }

    DaprMetadataProtos.AppConnectionHealthProperties health = appConnectionProperties.getHealth();
    String healthCheckPath = health.getHealthCheckPath();
    String healthProbeInterval = health.getHealthProbeInterval();
    String healthProbeTimeout = health.getHealthProbeTimeout();
    int healthThreshold = health.getHealthThreshold();

    return new AppConnectionPropertiesHealthMetadata(healthCheckPath, healthProbeInterval, healthProbeTimeout,
        healthThreshold);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<byte[]> encrypt(EncryptRequestAlpha1 request) {
    try {
      if (request == null) {
        throw new IllegalArgumentException("EncryptRequestAlpha1 cannot be null.");
      }
      if (request.getComponentName() == null || request.getComponentName().trim().isEmpty()) {
        throw new IllegalArgumentException("Component name cannot be null or empty.");
      }
      if (request.getKeyName() == null || request.getKeyName().trim().isEmpty()) {
        throw new IllegalArgumentException("Key name cannot be null or empty.");
      }
      if (request.getKeyWrapAlgorithm() == null || request.getKeyWrapAlgorithm().trim().isEmpty()) {
        throw new IllegalArgumentException("Key wrap algorithm cannot be null or empty.");
      }
      if (request.getPlainTextStream() == null) {
        throw new IllegalArgumentException("Plaintext stream cannot be null.");
      }

      return Flux.create(sink -> {
        // Create response observer to receive encrypted data
        final StreamObserver<DaprCryptoProtos.EncryptResponse> responseObserver =
            new StreamObserver<DaprCryptoProtos.EncryptResponse>() {
              @Override
              public void onNext(DaprCryptoProtos.EncryptResponse response) {
                if (response.hasPayload()) {
                  byte[] data = response.getPayload().getData().toByteArray();
                  if (data.length > 0) {
                    sink.next(data);
                  }
                }
              }

              @Override
              public void onError(Throwable t) {
                sink.error(DaprException.propagate(new DaprException("ENCRYPT_ERROR",
                    "Error during encryption: " + t.getMessage(), t)));
              }

              @Override
              public void onCompleted() {
                sink.complete();
              }
            };

        // Build options for the first message
        DaprCryptoProtos.EncryptRequestOptions.Builder optionsBuilder = DaprCryptoProtos.EncryptRequestOptions
            .newBuilder()
            .setComponentName(request.getComponentName())
            .setKeyName(request.getKeyName())
            .setKeyWrapAlgorithm(request.getKeyWrapAlgorithm());

        if (request.getDataEncryptionCipher() != null && !request.getDataEncryptionCipher().isEmpty()) {
          optionsBuilder.setDataEncryptionCipher(request.getDataEncryptionCipher());
        }
        optionsBuilder.setOmitDecryptionKeyName(request.isOmitDecryptionKeyName());
        if (request.getDecryptionKeyName() != null && !request.getDecryptionKeyName().isEmpty()) {
          optionsBuilder.setDecryptionKeyName(request.getDecryptionKeyName());
        }

        final DaprCryptoProtos.EncryptRequestOptions options = optionsBuilder.build();
        final long[] sequenceNumber = {0};
        final boolean[] firstMessage = {true};

        // Get the request stream observer from gRPC
        final StreamObserver<DaprCryptoProtos.EncryptRequest> requestObserver =
            intercept(null, asyncStub).encryptAlpha1(responseObserver);

        // Subscribe to the plaintext stream and send chunks
        request.getPlainTextStream()
            .doOnNext(chunk -> {
              DaprCryptoProtos.EncryptRequest.Builder reqBuilder = DaprCryptoProtos.EncryptRequest.newBuilder()
                  .setPayload(CommonProtos.StreamPayload.newBuilder()
                      .setData(ByteString.copyFrom(chunk))
                      .setSeq(sequenceNumber[0]++)
                      .build());

              // Include options only in the first message
              if (firstMessage[0]) {
                reqBuilder.setOptions(options);
                firstMessage[0] = false;
              }

              requestObserver.onNext(reqBuilder.build());
            })
            .doOnError(error -> {
              requestObserver.onError(error);
              sink.error(DaprException.propagate(new DaprException("ENCRYPT_ERROR", 
                  "Error reading plaintext stream: " + error.getMessage(), error)));
            })
            .doOnComplete(() -> {
              requestObserver.onCompleted();
            })
            .subscribe();
      });
    } catch (Exception ex) {
      return DaprException.wrapFlux(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<byte[]> decrypt(DecryptRequestAlpha1 request) {
    try {
      if (request == null) {
        throw new IllegalArgumentException("DecryptRequestAlpha1 cannot be null.");
      }
      if (request.getComponentName() == null || request.getComponentName().trim().isEmpty()) {
        throw new IllegalArgumentException("Component name cannot be null or empty.");
      }
      if (request.getCipherTextStream() == null) {
        throw new IllegalArgumentException("Ciphertext stream cannot be null.");
      }

      return Flux.create(sink -> {
        // Create response observer to receive decrypted data
        final StreamObserver<DaprCryptoProtos.DecryptResponse> responseObserver =
            new StreamObserver<DaprCryptoProtos.DecryptResponse>() {
              @Override
              public void onNext(DaprCryptoProtos.DecryptResponse response) {
                if (response.hasPayload()) {
                  byte[] data = response.getPayload().getData().toByteArray();
                  if (data.length > 0) {
                    sink.next(data);
                  }
                }
              }

              @Override
              public void onError(Throwable t) {
                sink.error(DaprException.propagate(new DaprException("DECRYPT_ERROR",
                    "Error during decryption: " + t.getMessage(), t)));
              }

              @Override
              public void onCompleted() {
                sink.complete();
              }
            };

        // Build options for the first message
        DaprCryptoProtos.DecryptRequestOptions.Builder optionsBuilder = DaprCryptoProtos.DecryptRequestOptions
            .newBuilder()
            .setComponentName(request.getComponentName());

        if (request.getKeyName() != null && !request.getKeyName().isEmpty()) {
          optionsBuilder.setKeyName(request.getKeyName());
        }

        final DaprCryptoProtos.DecryptRequestOptions options = optionsBuilder.build();
        final long[] sequenceNumber = {0};
        final boolean[] firstMessage = {true};

        // Get the request stream observer from gRPC
        final StreamObserver<DaprCryptoProtos.DecryptRequest> requestObserver =
            intercept(null, asyncStub).decryptAlpha1(responseObserver);

        // Subscribe to the ciphertext stream and send chunks
        request.getCipherTextStream()
            .doOnNext(chunk -> {
              DaprCryptoProtos.DecryptRequest.Builder reqBuilder = DaprCryptoProtos.DecryptRequest.newBuilder()
                  .setPayload(CommonProtos.StreamPayload.newBuilder()
                      .setData(ByteString.copyFrom(chunk))
                      .setSeq(sequenceNumber[0]++)
                      .build());

              // Include options only in the first message
              if (firstMessage[0]) {
                reqBuilder.setOptions(options);
                firstMessage[0] = false;
              }

              requestObserver.onNext(reqBuilder.build());
            })
            .doOnError(error -> {
              requestObserver.onError(error);
              sink.error(DaprException.propagate(new DaprException("DECRYPT_ERROR", 
                  "Error reading ciphertext stream: " + error.getMessage(), error)));
            })
            .doOnComplete(() -> {
              requestObserver.onCompleted();
            })
            .subscribe();
      });
    } catch (Exception ex) {
      return DaprException.wrapFlux(ex);
    }
  }
}
