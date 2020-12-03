/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.GetStatesRequest;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeServiceRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.Response;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.config.Properties;
import io.dapr.serializer.DaprObjectSerializer;
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
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor;
import io.opencensus.implcore.trace.propagation.PropagationComponentImpl;
import io.opencensus.implcore.trace.propagation.TraceContextFormat;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.propagation.BinaryFormat;
import io.opencensus.trace.propagation.SpanContextParseException;
import io.opencensus.trace.propagation.TextFormat;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * An adapter for the GRPC Client.
 *
 * @see io.dapr.v1.DaprGrpc
 * @see io.dapr.client.DaprClient
 */
public class DaprClientGrpc extends AbstractDaprClient {

  private static final TextMapPropagator.Setter<Map<String, String>> MAP_SETTER =
      (mapper, key, value) -> {
        if (mapper != null) {
          mapper.put(key, value);
        }
      };

  /**
   * Binary formatter to generate grpc-trace-bin.
   */
  private static final BinaryFormat OPENCENSUS_BINARY_FORMAT = new PropagationComponentImpl().getBinaryFormat();

  /**
   * The GRPC managed channel to be used.
   */
  private Closeable channel;

  /**
   * The GRPC client to be used.
   *
   * @see io.dapr.v1.DaprGrpc.DaprFutureStub
   */
  private DaprGrpc.DaprFutureStub client;

  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param closeableChannel A closeable for a Managed GRPC channel
   * @param futureClient     GRPC client
   * @param objectSerializer Serializer for transient request/response objects.
   * @param stateSerializer  Serializer for state objects.
   * @see DaprClientBuilder
   */
  DaprClientGrpc(
      Closeable closeableChannel,
      DaprGrpc.DaprFutureStub futureClient,
      DaprObjectSerializer objectSerializer,
      DaprObjectSerializer stateSerializer) {
    super(objectSerializer, stateSerializer);
    this.channel = closeableChannel;
    this.client = populateWithInterceptors(futureClient);
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
  public Mono<Response<Void>> publishEvent(PublishEventRequest request) {
    try {
      String pubsubName = request.getPubsubName();
      String topic = request.getTopic();
      Object data = request.getData();
      // TODO(artursouza): handle metadata.
      // Map<String, String> metadata = request.getMetadata();
      Context context = request.getContext();
      DaprProtos.PublishEventRequest envelope = DaprProtos.PublishEventRequest.newBuilder()
          .setTopic(topic)
          .setPubsubName(pubsubName)
          .setData(ByteString.copyFrom(objectSerializer.serialize(data))).build();

      return Mono.fromCallable(wrap(context, () -> {
        ListenableFuture<Empty> futureEmpty = client.publishEvent(envelope);
        futureEmpty.get();
        return null;
      }));
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Response<T>> invokeService(InvokeServiceRequest invokeServiceRequest, TypeRef<T> type) {
    try {
      String appId = invokeServiceRequest.getAppId();
      String method = invokeServiceRequest.getMethod();
      Object request = invokeServiceRequest.getBody();
      HttpExtension httpExtension = invokeServiceRequest.getHttpExtension();
      Context context = invokeServiceRequest.getContext();
      DaprProtos.InvokeServiceRequest envelope = buildInvokeServiceRequest(
          httpExtension,
          appId,
          method,
          request);
      return Mono.fromCallable(wrap(context, () -> {
        ListenableFuture<CommonProtos.InvokeResponse> futureResponse =
            client.invokeService(envelope);

        return objectSerializer.deserialize(futureResponse.get().getData().getValue().toByteArray(), type);
      })).map(r -> new Response<>(context, r));
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Response<T>> invokeBinding(InvokeBindingRequest request, TypeRef<T> type) {
    try {
      final String name = request.getName();
      final String operation = request.getOperation();
      final Object data = request.getData();
      final Map<String, String> metadata = request.getMetadata();
      final Context context = request.getContext();
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
      return Mono.fromCallable(wrap(context, () -> {
        ListenableFuture<DaprProtos.InvokeBindingResponse> futureResponse = client.invokeBinding(envelope);
        return objectSerializer.deserialize(futureResponse.get().getData().toByteArray(), type);
      })).map(r -> new Response<>(context, r));
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Response<State<T>>> getState(GetStateRequest request, TypeRef<T> type) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final String key = request.getKey();
      final StateOptions options = request.getStateOptions();
      // TODO(artursouza): handle etag once available in proto.
      // String etag = request.getEtag();
      final Context context = request.getContext();

      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }
      DaprProtos.GetStateRequest.Builder builder = DaprProtos.GetStateRequest.newBuilder()
          .setStoreName(stateStoreName)
          .setKey(key)
          .putAllMetadata(request.getMetadata());
      if (options != null && options.getConsistency() != null) {
        builder.setConsistency(getGrpcStateConsistency(options));
      }

      DaprProtos.GetStateRequest envelope = builder.build();
      return Mono.fromCallable(wrap(context, () -> {
        ListenableFuture<DaprProtos.GetStateResponse> futureResponse = client.getState(envelope);
        DaprProtos.GetStateResponse response = null;
        try {
          response = futureResponse.get();
        } catch (NullPointerException npe) {
          return null;
        }
        return buildStateKeyValue(response, key, options, type);
      })).map(s -> new Response<>(context, s));
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Response<List<State<T>>>> getStates(GetStatesRequest request, TypeRef<T> type) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final List<String> keys = request.getKeys();
      final int parallelism = request.getParallelism();
      final Context context = request.getContext();
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
      if (request.getMetadata() != null) {
        builder.putAllMetadata(request.getMetadata());
      }

      DaprProtos.GetBulkStateRequest envelope = builder.build();
      return Mono.fromCallable(wrap(context, () -> {
        ListenableFuture<DaprProtos.GetBulkStateResponse> futureResponse = client.getBulkState(envelope);
        DaprProtos.GetBulkStateResponse response = null;
        try {
          response = futureResponse.get();
        } catch (NullPointerException npe) {
          return null;
        }

        return response
            .getItemsList()
            .stream()
            .map(b -> {
              try {
                return buildStateKeyValue(b, type);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
            .collect(Collectors.toList());
      })).map(s -> new Response<>(context, s));
    } catch (Exception ex) {
      return Mono.error(ex);
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
    return new State<>(value, key, etag);
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
    return new State<>(value, requestedKey, etag, stateOptions);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Response<Void>> executeTransaction(ExecuteStateTransactionRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final List<TransactionalStateOperation<?>> operations = request.getOperations();
      final Map<String, String> metadata = request.getMetadata();
      final Context context = request.getContext();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      DaprProtos.ExecuteStateTransactionRequest.Builder builder = DaprProtos.ExecuteStateTransactionRequest
          .newBuilder();
      builder.setStoreName(stateStoreName);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      for (TransactionalStateOperation<?> operation: operations) {
        DaprProtos.TransactionalStateOperation.Builder operationBuilder = DaprProtos.TransactionalStateOperation
            .newBuilder();
        operationBuilder.setOperationType(operation.getOperation().toString().toLowerCase());
        operationBuilder.setRequest(buildStateRequest(operation.getRequest()).build());
        builder.addOperations(operationBuilder.build());
      }
      DaprProtos.ExecuteStateTransactionRequest req = builder.build();

      return Mono.fromCallable(wrap(context, () -> client.executeStateTransaction(req))).flatMap(f -> {
        try {
          f.get();
        } catch (Exception e) {
          return Mono.error(e);
        }
        return Mono.empty();
      }).thenReturn(new Response<>(context, null));
    } catch (IOException e) {
      return Mono.error(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Response<Void>> saveStates(SaveStateRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final List<State<?>> states = request.getStates();
      final Context context = request.getContext();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      DaprProtos.SaveStateRequest.Builder builder = DaprProtos.SaveStateRequest.newBuilder();
      builder.setStoreName(stateStoreName);
      for (State<?> state : states) {
        builder.addStates(buildStateRequest(state).build());
      }
      DaprProtos.SaveStateRequest req = builder.build();

      return Mono.fromCallable(wrap(context, () -> client.saveState(req))).flatMap(f -> {
        try {
          f.get();
        } catch (Exception ex) {
          return Mono.error(ex);
        }
        return Mono.empty();
      }).thenReturn(new Response<>(context, null));
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  private <T> CommonProtos.StateItem.Builder buildStateRequest(State<T> state) throws IOException {
    byte[] bytes = stateSerializer.serialize(state.getValue());

    CommonProtos.StateItem.Builder stateBuilder = CommonProtos.StateItem.newBuilder();
    if (state.getEtag() != null) {
      stateBuilder.setEtag(state.getEtag());
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
  public Mono<Response<Void>> deleteState(DeleteStateRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final String key = request.getKey();
      final StateOptions options = request.getStateOptions();
      final String etag = request.getEtag();
      final Context context = request.getContext();

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
          .setKey(key)
          .putAllMetadata(request.getMetadata());
      if (etag != null) {
        builder.setEtag(etag);
      }

      if (optionBuilder != null) {
        builder.setOptions(optionBuilder.build());
      }

      DaprProtos.DeleteStateRequest req = builder.build();
      return Mono.fromCallable(wrap(context, () -> client.deleteState(req))).flatMap(f -> {
        try {
          f.get();
        } catch (Exception ex) {
          return Mono.error(ex);
        }
        return Mono.empty();
      }).thenReturn(new Response<>(context, null));
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * Builds the object io.dapr.{@link DaprProtos.InvokeServiceRequest} to be send based on the parameters.
   *
   * @param httpExtension Object for HttpExtension
   * @param appId         The application id to be invoked
   * @param method        The application method to be invoked
   * @param request       The body of the request to be send as part of the invocation
   * @param <K>           The Type of the Body
   * @return The object to be sent as part of the invocation.
   * @throws IOException If there's an issue serializing the request.
   */
  private <K> DaprProtos.InvokeServiceRequest buildInvokeServiceRequest(
      HttpExtension httpExtension,
      String appId,
      String method,
      K request) throws IOException {
    if (httpExtension == null) {
      throw new IllegalArgumentException("HttpExtension cannot be null. Use HttpExtension.NONE instead.");
    }
    CommonProtos.InvokeRequest.Builder requestBuilder = CommonProtos.InvokeRequest.newBuilder();
    requestBuilder.setMethod(method);
    if (request != null) {
      byte[] byteRequest = objectSerializer.serialize(request);
      Any data = Any.newBuilder().setValue(ByteString.copyFrom(byteRequest)).build();
      requestBuilder.setData(data);
    } else {
      requestBuilder.setData(Any.newBuilder().build());
    }
    CommonProtos.HTTPExtension.Builder httpExtensionBuilder = CommonProtos.HTTPExtension.newBuilder();
    httpExtensionBuilder.setVerb(CommonProtos.HTTPExtension.Verb.valueOf(httpExtension.getMethod().toString()))
        .putAllQuerystring(httpExtension.getQueryString());
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
  public Mono<Response<Map<String, String>>> getSecret(GetSecretRequest request) {
    String secretStoreName = request.getSecretStoreName();
    String key = request.getKey();
    Map<String, String> metadata = request.getMetadata();
    Context context = request.getContext();
    try {
      if ((secretStoreName == null) || (secretStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret key cannot be null or empty.");
      }
    } catch (Exception e) {
      return Mono.error(e);
    }

    DaprProtos.GetSecretRequest.Builder requestBuilder = DaprProtos.GetSecretRequest.newBuilder()
          .setStoreName(secretStoreName)
          .setKey(key);

    if (metadata != null) {
      requestBuilder.putAllMetadata(metadata);
    }
    return Mono.fromCallable(wrap(context, () -> {
      DaprProtos.GetSecretRequest req = requestBuilder.build();
      ListenableFuture<DaprProtos.GetSecretResponse> future = client.getSecret(req);
      return future.get();
    })).map(future -> new Response<>(context, future.getDataMap()));
  }

  /**
   * Closes the ManagedChannel for GRPC.
   * @see io.grpc.ManagedChannel#shutdown()
   * @throws IOException on exception.
   */
  @Override
  public void close() throws IOException {
    if (channel != null) {
      channel.close();
    }
  }

  /**
   * Populates GRPC client with interceptors.
   *
   * @param client GRPC client for Dapr.
   * @return Client after adding interceptors.
   */
  private static DaprGrpc.DaprFutureStub populateWithInterceptors(DaprGrpc.DaprFutureStub client) {
    ClientInterceptor interceptor = new ClientInterceptor() {
      @Override
      public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
          MethodDescriptor<ReqT, RespT> methodDescriptor,
          CallOptions callOptions,
          Channel channel) {
        ClientCall<ReqT, RespT> clientCall = channel.newCall(methodDescriptor, callOptions);
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(clientCall) {
          @Override
          public void start(final Listener<RespT> responseListener, final Metadata headers) {
            // Dapr only supports "grpc-trace-bin" for GRPC and OpenTelemetry SDK does not support that yet:
            // https://github.com/open-telemetry/opentelemetry-specification/issues/639
            // This should be the only use of OpenCensus SDK: populate "grpc-trace-bin".
            Context context = Context.current();
            SpanContext opencensusSpanContext = extractOpenCensusSpanContext(context);
            if (opencensusSpanContext != null) {
              byte[] grpcTraceBin = OPENCENSUS_BINARY_FORMAT.toByteArray(opencensusSpanContext);
              headers.put(Key.of(Headers.GRPC_TRACE_BIN, Metadata.BINARY_BYTE_MARSHALLER), grpcTraceBin);
            }

            String daprApiToken = Properties.API_TOKEN.get();
            if (daprApiToken != null) {
              headers.put(Key.of(Headers.DAPR_API_TOKEN, Metadata.ASCII_STRING_MARSHALLER), daprApiToken);
            }

            super.start(responseListener, headers);
          }
        };
      }
    };
    return client.withInterceptors(interceptor);
  }

  /**
   * Extracts the context from OpenTelemetry and creates a SpanContext for OpenCensus.
   *
   * @param openTelemetryContext Context from OpenTelemetry.
   * @return SpanContext for OpenCensus.
   */
  private static SpanContext extractOpenCensusSpanContext(Context openTelemetryContext) {
    Map<String, String> map = new HashMap<>();
    OpenTelemetry.getGlobalPropagators().getTextMapPropagator().inject(
        openTelemetryContext, map, MAP_SETTER);

    if (!map.containsKey("traceparent")) {
      // Trying to extract context without this key will throw an "expected" exception, so we avoid it here.
      return null;
    }

    try {
      return new TraceContextFormat()
          .extract(map, new TextFormat.Getter<Map<String, String>>() {
            @Nullable
            @Override
            public String get(Map<String, String> map, String key) {
              return map.get(key);
            }
          });
    } catch (SpanContextParseException e) {
      throw new RuntimeException(e);
    }
  }

  private static <V> Callable<V> wrap(Context context, Callable<V> callable) {
    if (context == null) {
      return callable;
    }

    return context.wrap(callable);
  }
}
