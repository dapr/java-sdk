/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import java.util.HashSet;
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
   * @param topicName Name of the topic being subscribed to.
   */
  public synchronized void addSubscribedTopic(String topicName) {
    if (!this.subscribedTopics.contains(topicName)) {
      this.subscribedTopics.add(topicName);
    }
  }

  public synchronized String[] listSubscribedTopics() {
    return this.subscribedTopics.toArray(new String[0]);
  }
}
