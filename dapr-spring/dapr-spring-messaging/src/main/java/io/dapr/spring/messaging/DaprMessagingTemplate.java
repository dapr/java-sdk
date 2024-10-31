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
import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.log.LogAccessor;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
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
   * Micrometer's Tracer.
   */
  @Nullable
  private Tracer tracer;

  /**
   * Micrometer's Tracer.
   */
  @Nullable
  private OpenTelemetry openTelemetry;

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
    Hooks.enableAutomaticContextPropagation();
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
    this.tracer = this.applicationContext.getBeanProvider(Tracer.class)
            .getIfUnique(() -> this.tracer);
    this.openTelemetry = this.applicationContext.getBeanProvider(OpenTelemetry.class)
            .getIfUnique(() -> this.openTelemetry);
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

    return observation.observe(() -> {
      System.out.printf("**** Sending [%s]%n", message);

      return daprClient.publishEvent(pubsubName,
                      topic,
                      message,
                      Collections.singletonMap(Metadata.TTL_IN_SECONDS, MESSAGE_TTL_IN_SECONDS))
              .contextWrite(this::addTracingHeaders)
              .doOnError(
                      (err) -> {

                        this.logger.error(err, () -> "Failed to send msg to '%s' topic".formatted(topic));
                        observation.error(err);
                        observation.stop();
                      }
              ).doOnSuccess((err) -> {
                this.logger.trace(() -> "Sent msg to '%s' topic".formatted(topic));
                observation.stop();
              });
    });
  }

  private Context addTracingHeaders(reactor.util.context.Context context) {
    Map<String, String> map = new HashMap<>();
    openTelemetry.getPropagators().getTextMapPropagator()
            .inject(io.opentelemetry.context.Context.current(), map, (carrier, key, value) -> {
              map.put(key, value);
            });
    return context.putAll(Context.of(map).readOnly());
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
