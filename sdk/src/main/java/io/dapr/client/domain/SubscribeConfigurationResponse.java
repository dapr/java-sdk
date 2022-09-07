/*
 * Copyright 2022 The Dapr Authors
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

package io.dapr.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/**
 * Domain object for response from subscribeConfiguration API.
 */
public class SubscribeConfigurationResponse {

  /**
   * Subscription id for the items subscribed to.
   */
  @JsonProperty("id")
  private String subscriptionId;

  /**
   * Map of Configuration key to {@link ConfigurationItem}.
   */
  private Map<String, ConfigurationItem> items;

  /**
   * Default constructor for SubscribeConfigurationResponse.
   */
  public SubscribeConfigurationResponse() {

  }

  /**
   * Constructor for SubscribeConfigurationResponse.
   *
   * @param id        Subscription id for the items subscribed to.This id is returned by subscribeToConfiguration API.
   * @param items     Map of configuration items user subscribed to.
   */
  public SubscribeConfigurationResponse(String id, Map<String, ConfigurationItem> items) {
    this.subscriptionId = id;
    this.items = Collections.unmodifiableMap(items);
  }

  public Map<String, ConfigurationItem> getItems() {
    return items;
  }

  public String getSubscriptionId() {
    return subscriptionId;
  }
}
