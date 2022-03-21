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
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.GetBulkSecretRequest;
import io.dapr.client.domain.GetBulkStateRequest;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.internal.opencensus.GrpcWrapper;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.NetworkUtils;
import io.dapr.utils.TypeRef;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.context.Context;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * An adapter for the GRPC Client.
 *
 * @see io.dapr.v1.DaprGrpc
 * @see io.dapr.client.DaprClient
 */
public class DaprClientGrpc extends AbstractDaprClient {

  /**
   * The GRPC managed channel to be used.
   */
  private Closeable channel;

  /**
   * The async gRPC stub.
   */
  private DaprGrpc.DaprStub asyncStub;

  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param closeableChannel A closeable for a Managed GRPC channel
   * @param asyncStub        async gRPC stub
   * @param objectSerializer Serializer for transient request/response objects.
   * @param stateSerializer  Serializer for state objects.
   * @see DaprClientBuilder
   */
  DaprClientGrpc(
      Closeable closeableChannel,
      DaprGrpc.DaprStub asyncStub,
      DaprObjectSerializer objectSerializer,
      DaprObjectSerializer stateSerializer) {
    super(objectSerializer, stateSerializer);
    this.channel = closeableChannel;
    this.asyncStub = intercept(asyncStub);
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
  @Override
  public Mono<Void> waitForSidecar(int timeoutInMilliseconds) {
    return Mono.fromRunnable(() -> {
      try {
        NetworkUtils.waitForSocket(Properties.SIDECAR_IP.get(), Properties.GRPC_PORT.get(), timeoutInMilliseconds);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
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
      DaprProtos.PublishEventRequest.Builder envelopeBuilder = DaprProtos.PublishEventRequest.newBuilder()
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

      return Mono.subscriberContext().flatMap(
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
  public <T> Mono<T> invokeMethod(InvokeMethodRequest invokeMethodRequest, TypeRef<T> type) {
    try {
      String appId = invokeMethodRequest.getAppId();
      String method = invokeMethodRequest.getMethod();
      Object body = invokeMethodRequest.getBody();
      HttpExtension httpExtension = invokeMethodRequest.getHttpExtension();
      DaprProtos.InvokeServiceRequest envelope = buildInvokeServiceRequest(
          httpExtension,
          appId,
          method,
          body);
      // Regarding missing metadata in method invocation for gRPC:
      // gRPC to gRPC does not handle metadata in Dapr runtime proto.
      // gRPC to HTTP does not map correctly in Dapr runtime as per https://github.com/dapr/dapr/issues/2342

      return Mono.subscriberContext().flatMap(
              context -> this.<CommonProtos.InvokeResponse>createMono(
                  it -> intercept(context, asyncStub).invokeService(envelope, it)
              )
          ).flatMap(
              it -> {
                try {
                  return Mono.justOrEmpty(objectSerializer.deserialize(it.getData().getValue().toByteArray(), type));
                } catch (IOException e)  {
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
      DaprProtos.InvokeBindingRequest.Builder builder = DaprProtos.InvokeBindingRequest.newBuilder()
          .setName(name).setOperation(operation);
      if (byteData != null) {
        builder.setData(ByteString.copyFrom(byteData));
      }
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      DaprProtos.InvokeBindingRequest envelope = builder.build();

      return Mono.subscriberContext().flatMap(
              context -> this.<DaprProtos.InvokeBindingResponse>createMono(
                  it -> intercept(context, asyncStub).invokeBinding(envelope, it)
              )
          ).flatMap(
              it -> {
                try {
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
      DaprProtos.GetStateRequest.Builder builder = DaprProtos.GetStateRequest.newBuilder()
          .setStoreName(stateStoreName)
          .setKey(key);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      if (options != null && options.getConsistency() != null) {
        builder.setConsistency(getGrpcStateConsistency(options));
      }

      DaprProtos.GetStateRequest envelope = builder.build();

      return Mono.subscriberContext().flatMap(
          context ->
              this.<DaprProtos.GetStateResponse>createMono(
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
      DaprProtos.GetBulkStateRequest.Builder builder = DaprProtos.GetBulkStateRequest.newBuilder()
          .setStoreName(stateStoreName)
          .addAllKeys(keys)
          .setParallelism(parallelism);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprProtos.GetBulkStateRequest envelope = builder.build();

      return Mono.subscriberContext().flatMap(
              context -> this.<DaprProtos.GetBulkStateResponse>createMono(it -> intercept(context, asyncStub)
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
      DaprProtos.BulkStateItem item,
      TypeRef<T> type) throws IOException {
    String key = item.getKey();
    String error = item.getError();
    if (!Strings.isNullOrEmpty(error)) {
      return new State<>(key, error);
    }

    ByteString payload = item.getData();
    byte[] data = payload == null ? null : payload.toByteArray();
    T value = stateSerializer.deserialize(data, type);
    String etag = item.getEtag();
    if (etag.equals("")) {
      etag = null;
    }
    return new State<>(key, value, etag, item.getMetadataMap(), null);
  }

  private <T> State<T> buildStateKeyValue(
      DaprProtos.GetStateResponse response,
      String requestedKey,
      StateOptions stateOptions,
      TypeRef<T> type) throws IOException {
    ByteString payload = response.getData();
    byte[] data = payload == null ? null : payload.toByteArray();
    T value = stateSerializer.deserialize(data, type);
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
      DaprProtos.ExecuteStateTransactionRequest.Builder builder = DaprProtos.ExecuteStateTransactionRequest
          .newBuilder();
      builder.setStoreName(stateStoreName);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      for (TransactionalStateOperation<?> operation : operations) {
        DaprProtos.TransactionalStateOperation.Builder operationBuilder = DaprProtos.TransactionalStateOperation
            .newBuilder();
        operationBuilder.setOperationType(operation.getOperation().toString().toLowerCase());
        operationBuilder.setRequest(buildStateRequest(operation.getRequest()).build());
        builder.addOperations(operationBuilder.build());
      }
      DaprProtos.ExecuteStateTransactionRequest req = builder.build();

      return Mono.subscriberContext().flatMap(
          context -> this.<Empty>createMono(it -> intercept(context, asyncStub).executeStateTransaction(req, it))
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
      DaprProtos.SaveStateRequest.Builder builder = DaprProtos.SaveStateRequest.newBuilder();
      builder.setStoreName(stateStoreName);
      for (State<?> state : states) {
        builder.addStates(buildStateRequest(state).build());
      }
      DaprProtos.SaveStateRequest req = builder.build();

      return Mono.subscriberContext().flatMap(
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
      DaprProtos.DeleteStateRequest.Builder builder = DaprProtos.DeleteStateRequest.newBuilder()
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

      DaprProtos.DeleteStateRequest req = builder.build();

      return Mono.subscriberContext().flatMap(
          context -> this.<Empty>createMono(it -> intercept(context, asyncStub).deleteState(req, it))
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * Builds the object io.dapr.{@link DaprProtos.InvokeServiceRequest} to be send based on the parameters.
   *
   * @param httpExtension Object for HttpExtension
   * @param appId         The application id to be invoked
   * @param method        The application method to be invoked
   * @param body          The body of the request to be send as part of the invocation
   * @param <K>           The Type of the Body
   * @return The object to be sent as part of the invocation.
   * @throws IOException If there's an issue serializing the request.
   */
  private <K> DaprProtos.InvokeServiceRequest buildInvokeServiceRequest(
      HttpExtension httpExtension,
      String appId,
      String method,
      K body) throws IOException {
    if (httpExtension == null) {
      throw new IllegalArgumentException("HttpExtension cannot be null. Use HttpExtension.NONE instead.");
    }
    CommonProtos.InvokeRequest.Builder requestBuilder = CommonProtos.InvokeRequest.newBuilder();
    requestBuilder.setMethod(method);
    if (body != null) {
      byte[] byteRequest = objectSerializer.serialize(body);
      Any data = Any.newBuilder().setValue(ByteString.copyFrom(byteRequest)).build();
      requestBuilder.setData(data);
    } else {
      requestBuilder.setData(Any.newBuilder().build());
    }
    CommonProtos.HTTPExtension.Builder httpExtensionBuilder = CommonProtos.HTTPExtension.newBuilder();

    httpExtensionBuilder.setVerb(CommonProtos.HTTPExtension.Verb.valueOf(httpExtension.getMethod().toString()))
        .setQuerystring(httpExtension.encodeQueryString());
    requestBuilder.setHttpExtension(httpExtensionBuilder.build());

    requestBuilder.setContentType(objectSerializer.getContentType());

    DaprProtos.InvokeServiceRequest.Builder envelopeBuilder = DaprProtos.InvokeServiceRequest.newBuilder()
        .setId(appId)
        .setMessage(requestBuilder.build());
    return envelopeBuilder.build();
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

    DaprProtos.GetSecretRequest.Builder requestBuilder = DaprProtos.GetSecretRequest.newBuilder()
              .setStoreName(secretStoreName)
              .setKey(key);

    if (metadata != null) {
      requestBuilder.putAllMetadata(metadata);
    }
    DaprProtos.GetSecretRequest req = requestBuilder.build();

    return Mono.subscriberContext().flatMap(
        context -> this.<DaprProtos.GetSecretResponse>createMono(it -> intercept(context, asyncStub).getSecret(req, it))
    ).map(DaprProtos.GetSecretResponse::getDataMap);
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

      DaprProtos.GetBulkSecretRequest.Builder builder = DaprProtos.GetBulkSecretRequest.newBuilder()
          .setStoreName(storeName);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprProtos.GetBulkSecretRequest envelope = builder.build();

      return Mono.subscriberContext().flatMap(
          context ->
            this.<DaprProtos.GetBulkSecretResponse>createMono(
                it -> intercept(context, asyncStub).getBulkSecret(envelope, it)
            )
      ).map(it -> {
        Map<String, DaprProtos.SecretResponse> secretsMap = it.getDataMap();
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

      DaprProtos.QueryStateRequest.Builder builder = DaprProtos.QueryStateRequest.newBuilder()
          .setStoreName(storeName)
          .setQuery(queryString);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprProtos.QueryStateRequest envelope = builder.build();

      return Mono.subscriberContext().flatMap(
          context -> this.<DaprProtos.QueryStateResponse>createMono(
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
                    throw  DaprException.propagate(e);
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
      DaprProtos.QueryStateItem item,
      TypeRef<T> type) throws IOException {
    String key = item.getKey();
    String error = item.getError();
    if (!Strings.isNullOrEmpty(error)) {
      return new QueryStateItem<>(key, null, error);
    }
    ByteString payload = item.getData();
    byte[] data = payload == null ? null : payload.toByteArray();
    T value = stateSerializer.deserialize(data, type);
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
    if (channel != null) {
      DaprException.wrap(() -> {
        channel.close();
        return true;
      }).call();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> shutdown() {
    return Mono.subscriberContext().flatMap(
        context -> this.<Empty>createMono(
            it -> intercept(context, asyncStub).shutdown(Empty.getDefaultInstance(), it))
    ).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<ConfigurationItem>> getConfiguration(GetConfigurationRequest request) {
    try {
      final String configurationStoreName = request.getStoreName();
      final Map<String, String> metadata = request.getMetadata();
      final List<String> keys = request.getKeys();
      if ((configurationStoreName == null) || (configurationStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Configuration Store Name cannot be null or empty.");
      }
      if (keys.isEmpty()) {
        throw new IllegalArgumentException("Keys can not be empty or null");
      }
      DaprProtos.GetConfigurationRequest.Builder builder = DaprProtos.GetConfigurationRequest.newBuilder()
          .setStoreName(configurationStoreName).addAllKeys(keys);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprProtos.GetConfigurationRequest envelope = builder.build();
      return this.getConfigurationAlpha1(envelope);

    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private Mono<List<ConfigurationItem>> getConfigurationAlpha1(DaprProtos.GetConfigurationRequest envelope) {
    return Mono.subscriberContext().flatMap(
        context ->
            this.<DaprProtos.GetConfigurationResponse>createMono(
                it -> intercept(context, asyncStub).getConfigurationAlpha1(envelope, it)
            )
    ).map(
        it ->
            it.getItemsList().stream()
                .map(this::buildConfigurationItem).collect(Collectors.toList())
    );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<List<ConfigurationItem>> subscribeToConfiguration(SubscribeConfigurationRequest request) {
    try {
      final String configurationStoreName = request.getStoreName();
      final List<String> keys = request.getKeys();
      final Map<String, String> metadata = request.getMetadata();

      if (configurationStoreName == null || (configurationStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Configuration Store Name can not be null or empty.");
      }
      if (keys.isEmpty()) {
        throw new IllegalArgumentException("Keys can not be null or empty.");
      }
      DaprProtos.SubscribeConfigurationRequest.Builder builder = DaprProtos.SubscribeConfigurationRequest.newBuilder()
          .setStoreName(configurationStoreName)
          .addAllKeys(keys);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprProtos.SubscribeConfigurationRequest envelope = builder.build();
      return this.<DaprProtos.SubscribeConfigurationResponse>createFlux(
          it -> intercept(asyncStub).subscribeConfigurationAlpha1(envelope, it)
      ).map(
          it ->
              it.getItemsList().stream()
                  .map(this::buildConfigurationItem).collect(Collectors.toList())
      );
    } catch (Exception ex) {
      return DaprException.wrapFlux(ex);
    }
  }

  /**
   * Build a new Configuration Item from provided parameter.
   *
   * @param configurationItem CommonProtos.ConfigurationItem
   * @return io.dapr.client.domain.ConfigurationItem
   */
  private ConfigurationItem buildConfigurationItem(
      CommonProtos.ConfigurationItem configurationItem) {
    return new ConfigurationItem(
        configurationItem.getKey(),
        configurationItem.getValue(),
        configurationItem.getVersion(),
        configurationItem.getMetadataMap()
    );
  }

  /**
   * Populates GRPC client with interceptors.
   *
   * @param client GRPC client for Dapr.
   * @return Client after adding interceptors.
   */
  private static DaprGrpc.DaprStub intercept(DaprGrpc.DaprStub client) {
    ClientInterceptor interceptor = new ClientInterceptor() {
      @Override
      public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
          MethodDescriptor<ReqT, RespT> methodDescriptor,
          CallOptions callOptions,
          Channel channel) {
        ClientCall<ReqT, RespT> clientCall = channel.newCall(methodDescriptor, callOptions);
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(clientCall) {
          @Override
          public void start(final Listener<RespT> responseListener, final Metadata metadata) {
            String daprApiToken = Properties.API_TOKEN.get();
            if (daprApiToken != null) {
              metadata.put(Metadata.Key.of(Headers.DAPR_API_TOKEN, Metadata.ASCII_STRING_MARSHALLER), daprApiToken);
            }

            super.start(responseListener, metadata);
          }
        };
      }
    };
    return client.withInterceptors(interceptor);
  }

  /**
   * Populates GRPC client with interceptors for telemetry.
   *
   * @param context Reactor's context.
   * @param client  GRPC client for Dapr.
   * @return Client after adding interceptors.
   */
  private static DaprGrpc.DaprStub intercept(Context context, DaprGrpc.DaprStub client) {
    return GrpcWrapper.intercept(context, client);
  }

  private <T> Mono<T> createMono(Consumer<StreamObserver<T>> consumer) {
    return Mono.create(sink -> DaprException.wrap(() -> consumer.accept(createStreamObserver(sink))).run());
  }

  private <T> Flux<T> createFlux(Consumer<StreamObserver<T>> consumer) {
    return Flux.create(sink -> DaprException.wrap(() -> consumer.accept(createStreamObserver(sink))).run());
  }

  private <T> StreamObserver<T> createStreamObserver(MonoSink<T> sink) {
    return new StreamObserver<T>() {
      @Override
      public void onNext(T value) {
        sink.success(value);
      }

      @Override
      public void onError(Throwable t) {
        sink.error(DaprException.propagate(new ExecutionException(t)));
      }

      @Override
      public void onCompleted() {
        sink.success();
      }
    };
  }

  private <T> StreamObserver<T> createStreamObserver(FluxSink<T> sink) {
    return new StreamObserver<T>() {
      @Override
      public void onNext(T value) {
        sink.next(value);
      }

      @Override
      public void onError(Throwable t) {
        sink.error(DaprException.propagate(new ExecutionException(t)));
      }

      @Override
      public void onCompleted() {
        sink.complete();
      }
    };
  }
}
