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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Internal Singleton to handle Dapr configuration.
 */
class DaprRuntime {

  /**
   * The singleton instance.
   */
  private static volatile DaprRuntime instance;

  /**
   * List of subscribed topics.
   */
  private final Set<String> subscribedTopics = new HashSet<>();

  /**
   * List of subscriptions.
   */
  private final List<DaprTopicSubscription> subscriptions = new ArrayList<>();

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
   * @param route Destination route for requests.
   * @param metadata Metadata for extended subscription functionality.
   */
  public synchronized void addSubscribedTopic(String pubsubName,
                                              String topicName,
                                              String route,
                                              Map<String,String> metadata) {
    if (!this.subscribedTopics.contains(topicName)) {
      this.subscribedTopics.add(topicName);
      this.subscriptions.add(new DaprTopicSubscription(pubsubName, topicName, route, metadata));
    }
  }

  public synchronized DaprTopicSubscription[] listSubscribedTopics() {
    return this.subscriptions.toArray(new DaprTopicSubscription[0]);
  }
}
