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

package io.dapr.testcontainers;

import java.util.Collections;
import java.util.List;

public class Subscription {
  private final String name;
  private final String pubsubName;
  private final String topic;
  private final String route;
  private final List<String> scopes;

  /**
   * Creates a new subscription.
   *
   * @param name       Subscription name.
   * @param pubsubName PubSub name.
   * @param topic      Topic name.
   * @param route      Route.
   */
  public Subscription(String name, String pubsubName, String topic, String route) {
    this(name, pubsubName, topic, route, Collections.emptyList());
  }

  /**
   * Creates a new subscription with scopes.
   *
   * @param name       Subscription name.
   * @param pubsubName PubSub name.
   * @param topic      Topic name.
   * @param route      Route.
   * @param scopes     App IDs allowed to use this subscription.
   */
  public Subscription(String name, String pubsubName, String topic, String route, List<String> scopes) {
    this.name = name;
    this.pubsubName = pubsubName;
    this.topic = topic;
    this.route = route;
    this.scopes = scopes == null ? Collections.emptyList() : List.copyOf(scopes);
  }

  public String getName() {
    return name;
  }

  public String getPubsubName() {
    return pubsubName;
  }

  public String getTopic() {
    return topic;
  }

  public String getRoute() {
    return route;
  }

  public List<String> getScopes() {
    return scopes;
  }
}
