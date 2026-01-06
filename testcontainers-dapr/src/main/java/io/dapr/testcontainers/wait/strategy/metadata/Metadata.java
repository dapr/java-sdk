/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.testcontainers.wait.strategy.metadata;

import java.util.Collections;
import java.util.List;

/**
 * Represents the response from the Dapr metadata API (/v1.0/metadata).
 *
 * @see <a href="https://docs.dapr.io/reference/api/metadata_api/">Dapr Metadata API</a>
 */
public class Metadata {
  private String id;
  private String runtimeVersion;
  private List<String> enabledFeatures;
  private List<Actor> actors;
  private List<Component> components;
  private List<Subscription> subscriptions;

  public Metadata() {
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

  public List<String> getEnabledFeatures() {
    return enabledFeatures;
  }

  public void setEnabledFeatures(List<String> enabledFeatures) {
    this.enabledFeatures = enabledFeatures;
  }

  public List<Actor> getActors() {
    return actors != null ? actors : Collections.emptyList();
  }

  public void setActors(List<Actor> actors) {
    this.actors = actors;
  }

  public List<Component> getComponents() {
    return components != null ? components : Collections.emptyList();
  }

  public void setComponents(List<Component> components) {
    this.components = components;
  }

  public List<Subscription> getSubscriptions() {
    return subscriptions != null ? subscriptions : Collections.emptyList();
  }

  public void setSubscriptions(List<Subscription> subscriptions) {
    this.subscriptions = subscriptions;
  }
}
