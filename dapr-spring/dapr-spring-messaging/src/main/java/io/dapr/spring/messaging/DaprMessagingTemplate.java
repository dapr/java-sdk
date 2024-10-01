/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.spring.messaging;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.Metadata;
import io.dapr.spring.messaging.observation.DaprMessageSenderContext;
import io.dapr.spring.messaging.observation.DaprTemplateObservation;
import io.dapr.spring.messaging.observation.DaprTemplateObservationConvention;
import io.dapr.spring.messaging.observation.DefaultDaprTemplateObservationConvention;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.log.LogAccessor;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Create a new DaprMessagingTemplate.
 * @param <T> templated message type
 */
public class DaprMessagingTemplate<T> implements DaprMessagingOperations<T>,
        ApplicationContextAware, BeanNameAware, SmartInitializingSingleton {

  private final LogAccessor logger = new LogAccessor(this.getClass());

  private static final String MESSAGE_TTL_IN_SECONDS = "10";

  private final DaprClient daprClient;
  private final String pubsubName;

  /**
   * Whether to record observations.
   */
  private final boolean observationEnabled;

  /**
   * The registry to record observations with.
   */
  @Nullable
  private ObservationRegistry observationRegistry;

  /**
   * The optional custom observation convention to use when recording observations.
   */
  @Nullable
  private DaprTemplateObservationConvention observationConvention;

  @Nullable
  private ApplicationContext applicationContext;

  private String beanName = "";

  /**
   * New DaprMessagingTemplate.
   *
   * @param daprClient the DaprClient that will be used for sending the message
   * @param pubsubName the configured PubSub Dapr Component
   * @param observationEnabled if observation is enabled
   */
  public DaprMessagingTemplate(DaprClient daprClient, String pubsubName, boolean observationEnabled) {
    this.daprClient = daprClient;
    this.pubsubName = pubsubName;
    this.observationEnabled = observationEnabled;
  }

  /**
   * If observations are enabled, attempt to obtain the Observation registry and
   * convention.
   */
  @Override
  public void afterSingletonsInstantiated() {
    if (!this.observationEnabled) {
      this.logger.debug(() -> "Observations are not enabled - not recording");
      return;
    }
    if (this.applicationContext == null) {
      this.logger.warn(() -> "Observations enabled but application context null - not recording");
      return;
    }
    this.observationRegistry = this.applicationContext.getBeanProvider(ObservationRegistry.class)
            .getIfUnique(() -> this.observationRegistry);
    this.observationConvention = this.applicationContext.getBeanProvider(DaprTemplateObservationConvention.class)
            .getIfUnique(() -> this.observationConvention);
  }

  @Override
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void send(String topic, T message) {
    doSend(topic, message);
  }

  @Override
  public SendMessageBuilder<T> newMessage(T message) {
    return new SendMessageBuilderImpl<>(this, message);
  }

  private void doSend(String topic, T message) {
    doSendAsync(topic, message).block();
  }

  private Mono<Void> doSendAsync(String topic, T message) {

    this.logger.trace(() -> "Sending msg to '%s' topic".formatted(topic));

    DaprMessageSenderContext senderContext = DaprMessageSenderContext.newContext(topic, this.beanName);
    Observation observation = newObservation(senderContext);
    observation.start();


    return  daprClient.publishEvent(pubsubName,
                    topic,
                    message,
                    Collections.singletonMap(Metadata.TTL_IN_SECONDS, MESSAGE_TTL_IN_SECONDS))
            .contextWrite(getReactorContext(observation.getContextView())).doOnError(
                    (err) -> {

                      this.logger.error(err, () -> "Failed to send msg to '%s' topic".formatted(topic));
                      observation.error(err);
                      observation.stop();
                    }
            ).doOnSuccess((err) -> {
              this.logger.trace(() -> "Sent msg to '%s' topic".formatted(topic));
              observation.stop();
            });
  }

  /**
   * Converts current OpenTelemetry's context into Reactor's context.
   * @return Reactor's context.
   */
  public static reactor.util.context.ContextView getReactorContext() {
    return getReactorContext(Context.current());
  }

  /**
   * Converts given OpenTelemetry's context into Reactor's context.
   * @param context OpenTelemetry's context.
   * @return Reactor's context.
   */
  public static reactor.util.context.Context getReactorContext(Context context) {
    Map<String, String> map = new HashMap<>();
    TextMapSetter<Map<String, String>> setter =
            (carrier, key, value) -> map.put(key, value);

    GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(context, map, setter);
    reactor.util.context.Context reactorContext = reactor.util.context.Context.empty();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      reactorContext = reactorContext.put(entry.getKey(), entry.getValue());
    }
    return reactorContext;
  }

  /**
   * Converts given Micrometer's context into Reactor's context.
   * @param contextView Micrometers's contextView.
   * @return Reactor's context.
   */
  public static reactor.util.context.Context getReactorContext(io.micrometer.observation.Observation.ContextView
                                                                       contextView) {
    Map<String, String> map = new HashMap<>();
    TextMapSetter<Map<String, String>> setter =
            (carrier, key, value) -> map.put(key, value);

    final reactor.util.context.Context reactorContext = reactor.util.context.Context.empty();

    contextView.getAllKeyValues().forEach((entry) -> {
      reactorContext.put(entry.getKey(), entry.getValue());
    });

    Context otelContext = Context.root();
    reactorContext.stream()
            .forEach(entry -> {
              Object key = entry.getKey();
              Object value = entry.getValue();

              // Create a Context.Key for OpenTelemetry
              ContextKey<Object> otelKey = ContextKey.named(key.toString());

              // Store the entry in OpenTelemetry Context
              otelContext.with(otelKey, value);
            });

    GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
            .inject(otelContext, map, setter);
    return reactorContext;
  }




  private Observation newObservation(DaprMessageSenderContext senderContext) {
    if (this.observationRegistry == null) {
      return Observation.NOOP;
    }
    return DaprTemplateObservation.TEMPLATE_OBSERVATION.observation(this.observationConvention,
            DefaultDaprTemplateObservationConvention.INSTANCE, () -> senderContext, this.observationRegistry);
  }

  private static class SendMessageBuilderImpl<T> implements SendMessageBuilder<T> {

    private final DaprMessagingTemplate<T> template;

    private final T message;

    private String topic;

    SendMessageBuilderImpl(DaprMessagingTemplate<T> template, T message) {
      this.template = template;
      this.message = message;
    }

    @Override
    public SendMessageBuilder<T> withTopic(String topic) {
      this.topic = topic;
      return this;
    }


    @Override
    public void send() {
      this.template.doSend(this.topic, this.message);
    }

    @Override
    public Mono<Void> sendAsync() {
      return this.template.doSendAsync(this.topic, this.message);
    }

  }

}
