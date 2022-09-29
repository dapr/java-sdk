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

package io.dapr.springboot;

import io.dapr.Rule;
import io.dapr.client.domain.SubscribeConfigurationResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Internal Singleton to handle Dapr configuration.
 */
public final class DaprRuntime {
  /**
   * The singleton instance.
   */
  private static volatile DaprRuntime instance;

  /**
   * Map of subscription builders.
   */
  private final Map<DaprTopicKey, DaprSubscriptionBuilder> subscriptionBuilders = new HashMap<>();

  /**
   * Map of Store name to BiConsumer of Store name and {@link SubscribeConfigurationResponse}.
   */
  private final Map<String, BiConsumer<String, SubscribeConfigurationResponse>>
      configurationChangeHandlers = Collections.synchronizedMap(new HashMap<>());

  /**
   * Private constructor to make this singleton.
   */
  private DaprRuntime() {
  }

  /**
   * Returns an DaprRuntime object.
   *
   * @return An DaprRuntime object.
   */
  public static DaprRuntime getInstance() {
    if (instance == null) {
      synchronized (DaprRuntime.class) {
        if (instance == null) {
          instance = new DaprRuntime();
        }
      }
    }

    return instance;
  }

  /**
   * Adds a topic to the list of subscribed topics.
   *
   * @param pubsubName Pubsub name to subcribe to.
   * @param topicName Name of the topic being subscribed to.
   * @param rule The optional rule for this route.
   * @param route Destination route for requests.
   * @param metadata Metadata for extended subscription functionality.
   */
  synchronized void addSubscribedTopic(String pubsubName,
                                              String topicName,
                                              String match,
                                              int priority,
                                              String route,
                                              Map<String,String> metadata) {
    DaprTopicKey topicKey = new DaprTopicKey(pubsubName, topicName);

    DaprSubscriptionBuilder builder = subscriptionBuilders.get(topicKey);
    if (builder == null) {
      builder = new DaprSubscriptionBuilder(pubsubName, topicName);
      subscriptionBuilders.put(topicKey, builder);
    }

    if (match.length() > 0) {
      builder.addRule(route, match, priority);
    } else {
      builder.setDefaultPath(route);
    }

    if (metadata != null && !metadata.isEmpty()) {
      builder.setMetadata(metadata);
    }
  }

  synchronized DaprTopicSubscription[] listSubscribedTopics() {
    List<DaprTopicSubscription> values = subscriptionBuilders.values().stream()
            .map(b -> b.build()).collect(Collectors.toList());
    return values.toArray(new DaprTopicSubscription[0]);
  }

  /**
   * Method to Register different configuration change handlers.
   * @param store Name of the configuration store
   * @param handler BiConsumer handler to be called when configurations are modified for this store.
   */
  public void registerConfigurationChangeHandler(
      String store, BiConsumer<String,
      SubscribeConfigurationResponse> handler) {
    this.configurationChangeHandlers.put(store, handler);
  }

  /**
   * Method to call the BiConsumer handler registered for teh given store name.
   * @param store Name of the configuration store
   * @param resp {@link SubscribeConfigurationResponse}
   */
  void handleConfigurationChange(String store, SubscribeConfigurationResponse resp) {
    BiConsumer<String, SubscribeConfigurationResponse> handler = this.configurationChangeHandlers.get(store);
    handler.accept(store, resp);
  }
}
