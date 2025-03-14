package io.dapr.ai.client;

import com.google.protobuf.Any;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.internal.exceptions.DaprHttpException;
import io.dapr.internal.grpc.interceptors.DaprTimeoutInterceptor;
import io.dapr.internal.grpc.interceptors.DaprTracingInterceptor;
import io.dapr.internal.resiliency.RetryPolicy;
import io.dapr.internal.resiliency.TimeoutPolicy;
import io.dapr.utils.NetworkUtils;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DaprConversationClient implements AutoCloseable, DaprAiClient {

  /**
   * Stub that has the method to call the conversation apis.
   */
  private final DaprGrpc.DaprStub asyncStub;

  /**
   * The retry policy.
   */
  private final RetryPolicy retryPolicy;

  /**
   * The timeout policy.
   */
  private final TimeoutPolicy timeoutPolicy;

  /**
   * Constructor to create conversation client.
   */
  public DaprConversationClient() {
    this(DaprGrpc.newStub(NetworkUtils.buildGrpcManagedChannel(new Properties())), null);
  }

  /**
   * Constructor.
   *
   * @param properties with client configuration options.
   * @param resiliencyOptions retry options.
   */
  public DaprConversationClient(
      Properties properties,
      ResiliencyOptions resiliencyOptions) {
    this(DaprGrpc.newStub(NetworkUtils.buildGrpcManagedChannel(properties)), resiliencyOptions);
  }

  /**
   * ConversationClient constructor.
   *
   * @param resiliencyOptions timeout and retry policies.
   */
  protected DaprConversationClient(
      DaprGrpc.DaprStub asyncStub,
      ResiliencyOptions resiliencyOptions) {
    this.asyncStub = asyncStub;
    this.retryPolicy = new RetryPolicy(resiliencyOptions == null ? null : resiliencyOptions.getMaxRetries());
    this.timeoutPolicy = new TimeoutPolicy(resiliencyOptions == null ? null : resiliencyOptions.getTimeout());
  }

  @Override
  public Mono<DaprConversationResponse> converse(
      String conversationComponentName,
      List<DaprConversationInput> daprConversationInputs) {
    return converse(conversationComponentName, daprConversationInputs, null, false, 0.0d);
  }

  @Override
  public Mono<DaprConversationResponse> converse(
      String conversationComponentName,
      List<DaprConversationInput> daprConversationInputs,
      String contextId,
      boolean scrubPii,
      double temperature) {

    try {
      if ((conversationComponentName == null) || (conversationComponentName.trim().isEmpty())) {
        throw new IllegalArgumentException("Conversation component name cannot be null or empty.");
      }

      if ((daprConversationInputs == null) || (daprConversationInputs.isEmpty())) {
        throw new IllegalArgumentException("Conversation inputs cannot be null or empty.");
      }

      DaprProtos.ConversationRequest.Builder conversationRequest = DaprProtos.ConversationRequest.newBuilder()
          .setTemperature(temperature)
          .setScrubPII(scrubPii)
          .setName(conversationComponentName);

      if (contextId != null) {
        conversationRequest.setContextID(contextId);
      }

      for (DaprConversationInput input : daprConversationInputs) {
        if (input.getContent() == null || input.getContent().isEmpty()) {
          throw new IllegalArgumentException("Conversation input content cannot be null or empty.");
        }

        DaprProtos.ConversationInput.Builder conversationInputOrBuilder = DaprProtos.ConversationInput.newBuilder()
            .setContent(input.getContent())
            .setScrubPII(input.isScrubPii());

        if (input.getRole() != null) {
          conversationInputOrBuilder.setRole(input.getRole().toString());
        }

        conversationRequest.addInputs(conversationInputOrBuilder.build());
      }

      Mono<DaprProtos.ConversationResponse> conversationResponseMono = Mono.deferContextual(
          context -> this.createMono(
              it -> intercept(context, asyncStub)
                  .converseAlpha1(conversationRequest.build(), it)
          )
      );

      return conversationResponseMono.map(conversationResponse -> {

        List<DaprConversationOutput> daprConversationOutputs = new ArrayList<>();
        for (DaprProtos.ConversationResult conversationResult : conversationResponse.getOutputsList()) {
          Map<String, byte[]> parameters = new HashMap<>();
          for (Map.Entry<String, Any> entrySet : conversationResult.getParametersMap().entrySet()) {
            parameters.put(entrySet.getKey(), entrySet.getValue().toByteArray());
          }

          DaprConversationOutput daprConversationOutput =
              new DaprConversationOutput(conversationResult.getResult(), parameters);
          daprConversationOutputs.add(daprConversationOutput);
        }

        return new DaprConversationResponse(conversationResponse.getContextID(), daprConversationOutputs);
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  @Override
  public void close() throws Exception {
    ManagedChannel channel = (ManagedChannel) this.asyncStub.getChannel();

    DaprException.wrap(() -> {
      if (channel != null && !channel.isShutdown()) {
        channel.shutdown();
      }

      return true;
    }).call();
  }

  private DaprGrpc.DaprStub intercept(
      ContextView context, DaprGrpc.DaprStub client) {
    return client.withInterceptors(
        new DaprTimeoutInterceptor(this.timeoutPolicy),
        new DaprTracingInterceptor(context));
  }

  private <T> Mono<T> createMono(Consumer<StreamObserver<T>> consumer) {
    return retryPolicy.apply(
        Mono.create(sink -> DaprException.wrap(() -> consumer.accept(
            createStreamObserver(sink))).run()));
  }

  private <T> StreamObserver<T> createStreamObserver(MonoSink<T> sink) {
    return new StreamObserver<T>() {
      @Override
      public void onNext(T value) {
        sink.success(value);
      }

      @Override
      public void onError(Throwable t) {
        sink.error(DaprException.propagate(DaprHttpException.fromGrpcExecutionException(null, t)));
      }

      @Override
      public void onCompleted() {
        sink.success();
      }
    };
  }
}
