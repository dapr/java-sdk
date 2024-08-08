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

public class Subscription {
  private String name;
  private String pubsubName;
  private String topic;
  private String route;

  /**
   * Creates a new subscription.
   *
   * @param name       Subscription name.
   * @param pubsubName PubSub name.
   * @param topic      Topic name.
   * @param route      Route.
   */
  public Subscription(String name, String pubsubName, String topic, String route) {
    this.name = name;
    this.pubsubName = pubsubName;
    this.topic = topic;
    this.route = route;
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
}
