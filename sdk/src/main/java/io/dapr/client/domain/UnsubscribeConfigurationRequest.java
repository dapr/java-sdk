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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Request to unsubscribe to one or more configuration items using subscription id.
 */
public class UnsubscribeConfigurationRequest {
  private final String storeName;
  private final String id;

  /**
   * Constructor for UnsubscribeConfigurationRequest.
   *
   * @param id        Subscription id for the items subscribed to.This id is returned by subscribeToConfiguration API.
   * @param storeName Name of the configuration store.
   */
  public UnsubscribeConfigurationRequest(String id, String storeName) {
    this.storeName = storeName;
    this.id = id;
  }

  public String getStoreName() {
    return storeName;
  }

  public String getId() {
    return id;
  }
}
