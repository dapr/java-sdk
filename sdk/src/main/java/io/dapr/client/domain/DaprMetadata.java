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

package io.dapr.client.domain;

import java.util.List;

/**
 * DaprMetadata describes the Dapr Metadata.
 */
public final class DaprMetadata {

  private String id;
  private String runtimeVersion;
  private List<ComponentMetadata> components;
  private List<SubscriptionMetadata> subscriptions;

  public DaprMetadata() {
  }

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
    this.components = components;
    this.subscriptions = subscriptions;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getRuntimeVersion() {
    return runtimeVersion;
  }

  public void setRuntimeVersion(String runtimeVersion) {
    this.runtimeVersion = runtimeVersion;
  }

  public List<ComponentMetadata> getComponents() {
    return components;
  }

  public void setComponents(List<ComponentMetadata> components) {
    this.components = components;
  }

  public List<SubscriptionMetadata> getSubscriptions() {
    return subscriptions;
  }

  public void setSubscriptions(List<SubscriptionMetadata> subscriptions) {
    this.subscriptions = subscriptions;
  }

}
