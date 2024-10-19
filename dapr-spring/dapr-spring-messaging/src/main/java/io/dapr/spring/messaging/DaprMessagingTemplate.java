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
import io.dapr.spring.messaging.observation.DaprMessagingObservationConvention;
import io.dapr.spring.messaging.observation.DaprMessagingObservationDocumentation;
import io.dapr.spring.messaging.observation.DaprMessagingSenderContext;
import io.dapr.spring.messaging.observation.DefaultDaprMessagingObservationConvention;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

import java.util.Map;

/**
 * Create a new DaprMessagingTemplate.
 * @param <T> templated message type
 */
public class DaprMessagingTemplate<T> implements DaprMessagingOperations<T>, ApplicationContextAware, BeanNameAware,
    SmartInitializingSingleton {

  private static final Logger LOGGER = LoggerFactory.getLogger(DaprMessagingTemplate.class);
  private static final String MESSAGE_TTL_IN_SECONDS = "10";
  private static final DaprMessagingObservationConvention DEFAULT_OBSERVATION_CONVENTION =
      DefaultDaprMessagingObservationConvention.INSTANCE;

  private final DaprClient daprClient;
  private final String pubsubName;
  private final Map<String, String> metadata;
  private final boolean observationEnabled;

  @Nullable
  private ApplicationContext applicationContext;

  @Nullable
  private String beanName;

  @Nullable
  private ObservationRegistry observationRegistry;

  @Nullable
  private DaprMessagingObservationConvention observationConvention;

  /**
   * Constructs a new DaprMessagingTemplate.
   * @param daprClient Dapr client
   * @param pubsubName pubsub name
   * @param observationEnabled whether to enable observations
   */
  public DaprMessagingTemplate(DaprClient daprClient, String pubsubName, boolean observationEnabled) {
    this.daprClient = daprClient;
    this.pubsubName = pubsubName;
    this.metadata = Map.of(Metadata.TTL_IN_SECONDS, MESSAGE_TTL_IN_SECONDS);
    this.observationEnabled = observationEnabled;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  /**
   * If observations are enabled, attempt to obtain the Observation registry and
   * convention.
   */
  @Override
  public void afterSingletonsInstantiated() {
    if (!observationEnabled) {
      LOGGER.debug("Observations are not enabled - not recording");
      return;
    }

    if (applicationContext == null) {
      LOGGER.warn("Observations enabled but application context null - not recording");
      return;
    }

    observationRegistry = applicationContext.getBeanProvider(ObservationRegistry.class)
        .getIfUnique(() -> observationRegistry);
    observationConvention = applicationContext.getBeanProvider(DaprMessagingObservationConvention.class)
        .getIfUnique(() -> observationConvention);
  }

  @Override
  public void send(String topic, T message) {
    doSend(topic, message);
  }

  @Override
  public SendMessageBuilder<T> newMessage(T message) {
    return new DeefaultSendMessageBuilder<>(this, message);
  }

  private void doSend(String topic, T message) {
    doSendAsync(topic, message).block();
  }

  private Mono<Void> doSendAsync(String topic, T message) {
    LOGGER.trace("Sending message to '{}' topic", topic);

    if (canUseObservation()) {
      return publishEventWithObservation(pubsubName, topic, message);
    }

    return publishEvent(pubsubName, topic, message);
  }

  private boolean canUseObservation() {
    return observationEnabled
        && observationRegistry != null
        && observationConvention != null
        && beanName != null;
  }

  private Mono<Void> publishEvent(String pubsubName, String topic, T message) {
    return daprClient.publishEvent(pubsubName, topic, message, metadata);
  }

  private Mono<Void> publishEventWithObservation(String pubsubName, String topic, T message) {
    DaprMessagingSenderContext senderContext = DaprMessagingSenderContext.newContext(topic, this.beanName);
    Observation observation = createObservation(senderContext);

    return observation.observe(() ->
      publishEvent(pubsubName, topic, message)
        .doOnError(err -> {
          LOGGER.error("Failed to send msg to '{}' topic", topic, err);

          observation.error(err);
          observation.stop();
        })
        .doOnSuccess(ignore -> {
          LOGGER.trace("Sent msg to '{}' topic", topic);

          observation.stop();
        })
    );
  }

  private Observation createObservation(DaprMessagingSenderContext senderContext) {
    return DaprMessagingObservationDocumentation.TEMPLATE_OBSERVATION.observation(
        observationConvention,
        DEFAULT_OBSERVATION_CONVENTION,
        () -> senderContext,
        observationRegistry
    );
  }

  private static class DeefaultSendMessageBuilder<T> implements SendMessageBuilder<T> {

    private final DaprMessagingTemplate<T> template;

    private final T message;

    private String topic;

    DeefaultSendMessageBuilder(DaprMessagingTemplate<T> template, T message) {
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
      template.doSend(topic, message);
    }

    @Override
    public Mono<Void> sendAsync() {
      return template.doSendAsync(topic, message);
    }

  }

}
