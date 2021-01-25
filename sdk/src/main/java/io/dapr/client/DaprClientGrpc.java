/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import com.google.common.base.Strings;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.GetBulkSecretRequest;
import io.dapr.client.domain.GetBulkStateRequest;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.Response;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
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
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor;
import io.grpc.stub.StreamObserver;
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
import reactor.core.publisher.MonoSink;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
   * The async gRPC stub.
   */
  private DaprGrpc.DaprStub asyncStub;


  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param closeableChannel A closeable for a Managed GRPC channel
   * @param asyncStub     async gRPC stub
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
    this.asyncStub = populateWithInterceptors(asyncStub);
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
  public Mono<Response<Void>> publishEvent(PublishEventRequest request) {
    try {
      String pubsubName = request.getPubsubName();
      String topic = request.getTopic();
      Object data = request.getData();
      Context context = request.getContext();
      DaprProtos.PublishEventRequest.Builder envelopeBuilder = DaprProtos.PublishEventRequest.newBuilder()
          .setTopic(topic)
          .setPubsubName(pubsubName)
          .setData(ByteString.copyFrom(objectSerializer.serialize(data)));
      envelopeBuilder.setDataContentType(objectSerializer.getContentType());
      Map<String, String> metadata = request.getMetadata();
      if (metadata != null) {
        envelopeBuilder.putAllMetadata(metadata);
        String contentType = metadata.get(io.dapr.client.domain.Metadata.CONTENT_TYPE);
        if (contentType != null) {
          envelopeBuilder.setDataContentType(contentType);
        }
      }

      return this.<Empty>createMono(
              context,
              it -> asyncStub.publishEvent(envelopeBuilder.build(), it)
      ).thenReturn(new Response<>(context, null));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Response<T>> invokeMethod(InvokeMethodRequest invokeMethodRequest, TypeRef<T> type) {
    try {
      String appId = invokeMethodRequest.getAppId();
      String method = invokeMethodRequest.getMethod();
      Object body = invokeMethodRequest.getBody();
      HttpExtension httpExtension = invokeMethodRequest.getHttpExtension();
      Context context = invokeMethodRequest.getContext();
      DaprProtos.InvokeServiceRequest envelope = buildInvokeServiceRequest(
          httpExtension,
          appId,
          method,
          body);
      // Regarding missing metadata in method invocation for gRPC:
      // gRPC to gRPC does not handle metadata in Dapr runtime proto.
      // gRPC to HTTP does not map correctly in Dapr runtime as per https://github.com/dapr/dapr/issues/2342

      return this.<CommonProtos.InvokeResponse>createMono(
              context,
              it -> asyncStub.invokeService(envelope, it)
      ).flatMap(
              it -> {
                try {
                  return Mono.justOrEmpty(objectSerializer.deserialize(it.getData().getValue().toByteArray(), type));
                } catch (IOException e) {
                  throw DaprException.propagate(e);
                }
              }
      ).map(r -> new Response<>(context, r));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
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

      return this.<DaprProtos.InvokeBindingResponse>createMono(
              context,
              it -> asyncStub.invokeBinding(envelope, it)
      ).flatMap(
              it -> {
                try {
                  return Mono.justOrEmpty(objectSerializer.deserialize(it.getData().toByteArray(), type));
                } catch (IOException e) {
                  throw DaprException.propagate(e);
                }
              }
      ).map(r -> new Response<>(context, r));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Response<State<T>>> getState(GetStateRequest request, TypeRef<T> type) {
    try {
      final String stateStoreName = request.getStoreName();
      final String key = request.getKey();
      final StateOptions options = request.getStateOptions();
      final Map<String, String> metadata = request.getMetadata();
      final Context context = request.getContext();

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

      return this.<DaprProtos.GetStateResponse>createMono(
              context,
              it -> asyncStub.getState(envelope, it)
      ).map(
              it -> {
                try {
                  return buildStateKeyValue(it, key, options, type);
                } catch (IOException ex) {
                  throw DaprException.propagate(ex);
                }
              }
      ).map(s -> new Response<>(context, s));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Response<List<State<T>>>> getBulkState(GetBulkStateRequest request, TypeRef<T> type) {
    try {
      final String stateStoreName = request.getStoreName();
      final List<String> keys = request.getKeys();
      final int parallelism = request.getParallelism();
      final Map<String, String> metadata = request.getMetadata();
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
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprProtos.GetBulkStateRequest envelope = builder.build();

      return this.<DaprProtos.GetBulkStateResponse>createMono(
              context,
              it -> asyncStub.getBulkState(envelope, it)
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
      ).map(s -> new Response<>(context, s));
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
    return new State<>(value, key, etag, item.getMetadataMap(), null);
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
    return new State<>(value, requestedKey, etag, response.getMetadataMap(), stateOptions);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Response<Void>> executeStateTransaction(ExecuteStateTransactionRequest request) {
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

      return this.<Empty>createMono(
              context,
              it -> asyncStub.executeStateTransaction(req, it)
      ).thenReturn(new Response<>(context, null));
    } catch (Exception e) {
      return DaprException.wrapMono(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Response<Void>> saveBulkState(SaveStateRequest request) {
    try {
      final String stateStoreName = request.getStoreName();
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

      return this.<Empty>createMono(
              context,
              it -> asyncStub.saveState(req, it)
      ).thenReturn(new Response<>(context, null));
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
  public Mono<Response<Void>> deleteState(DeleteStateRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final String key = request.getKey();
      final StateOptions options = request.getStateOptions();
      final String etag = request.getEtag();
      final Map<String, String> metadata = request.getMetadata();
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

      return this.<Empty>createMono(
              context,
              it -> asyncStub.deleteState(req, it)
      ).thenReturn(new Response<>(context, null));
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
    String secretStoreName = request.getStoreName();
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
      return DaprException.wrapMono(e);
    }

    DaprProtos.GetSecretRequest.Builder requestBuilder = DaprProtos.GetSecretRequest.newBuilder()
          .setStoreName(secretStoreName)
          .setKey(key);

    if (metadata != null) {
      requestBuilder.putAllMetadata(metadata);
    }
    DaprProtos.GetSecretRequest req = requestBuilder.build();

    return this.<DaprProtos.GetSecretResponse>createMono(
            context,
            it -> asyncStub.getSecret(req, it)
    ).map(it -> new Response<>(context, it.getDataMap()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Response<Map<String, Map<String, String>>>> getBulkSecret(GetBulkSecretRequest request) {
    try {
      final String storeName = request.getStoreName();
      final Map<String, String> metadata = request.getMetadata();
      final Context context = request.getContext();
      if ((storeName == null) || (storeName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret store name cannot be null or empty.");
      }

      DaprProtos.GetBulkSecretRequest.Builder builder = DaprProtos.GetBulkSecretRequest.newBuilder()
          .setStoreName(storeName);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprProtos.GetBulkSecretRequest envelope = builder.build();

      return this.<DaprProtos.GetBulkSecretResponse>createMono(context, it -> asyncStub.getBulkSecret(envelope, it))
          .map(it -> {
            Map<String, DaprProtos.SecretResponse> secretsMap = it.getDataMap();
            if (secretsMap == null) {
              return Collections.EMPTY_MAP;
            }
            return secretsMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(s -> s.getKey(), s -> s.getValue().getSecretsMap()));
          })
          .map(s -> new Response<>(context, s));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * Closes the ManagedChannel for GRPC.
   * @see io.grpc.ManagedChannel#shutdown()
   * @throws IOException on exception.
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
   * Populates GRPC client with interceptors.
   *
   * @param client GRPC client for Dapr.
   * @return Client after adding interceptors.
   */
  private static DaprGrpc.DaprStub populateWithInterceptors(DaprGrpc.DaprStub client) {
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
      throw new DaprException(e);
    }
  }

  private static Runnable wrap(Context context, Runnable runnable) {
    if (context == null) {
      return DaprException.wrap(runnable);
    }

    return DaprException.wrap(context.wrap(runnable));
  }

  private <T> Mono<T> createMono(Context context, Consumer<StreamObserver<T>> consumer) {
    return Mono.create(
            sink -> wrap(context, () -> consumer.accept(createStreamObserver(sink))).run()
    );
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
}
