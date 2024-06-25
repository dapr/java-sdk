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

package io.dapr.client.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * DaprMetadata describes the Dapr Metadata.
 */
public final class DaprMetadata {

  private String id;
  private String runtimeVersion;
  private List<ComponentMetadata> components;
  private List<SubscriptionMetadata> subscriptions;

  /**
   * Constructor for a DaprMetadata.
   *
   * @param id of the application
   * @param runtimeVersion Dapr version
   * @param components list of registered componnets
   * @param subscriptions list of registered subscription
   */
  public DaprMetadata(String id, String runtimeVersion, List<ComponentMetadata> components,
      List<SubscriptionMetadata> subscriptions) {
    this.id = id;
    this.runtimeVersion = runtimeVersion;
    this.components = components == null ? Collections.emptyList() : Collections.unmodifiableList(components);
    this.subscriptions = subscriptions == null ? Collections.emptyList() : Collections.unmodifiableList(subscriptions);
  }

  public String getId() {
    return id;
  }

  public String getRuntimeVersion() {
    return runtimeVersion;
  }

  public List<ComponentMetadata> getComponents() {
    return components;
  }

  public List<SubscriptionMetadata> getSubscriptions() {
    return subscriptions;
  }
  
}
