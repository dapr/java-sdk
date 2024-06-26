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
import java.util.Map;

/**
 * DaprMetadata describes the Dapr Metadata.
 */
public final class DaprMetadata {

  private final String id;
  private final String runtimeVersion;
  private final List<String> enabledFeatures;
  private final List<ActorMetadata> actors;
  private final Map<String, String> attributes;
  private final List<ComponentMetadata> components;
  private final List<HttpEndpointMetadata> httpEndpoints;
  private final List<SubscriptionMetadata> subscriptions;
  private final AppConnectionPropertiesMetadata appConnectionProperties;

  /**
   * Constructor for a DaprMetadata.
   *
   * @param id of the application
   * @param runtimeVersion Dapr version
   * @param enabledFeatures list of enabled features
   * @param actors list of registered features
   * @param attributes map of extended attributes
   * @param components list of registered components
   * @param httpEndpoints list of registered http endpoints
   * @param subscriptions list of registered subscription
   * @param appConnectionProperties connection properties of the application
   */
  public DaprMetadata(String id, String runtimeVersion, List<String> enabledFeatures, List<ActorMetadata> actors,
      Map<String, String> attributes, List<ComponentMetadata> components, List<HttpEndpointMetadata> httpEndpoints,
      List<SubscriptionMetadata> subscriptions, AppConnectionPropertiesMetadata appConnectionProperties) {
    this.id = id;
    this.runtimeVersion = runtimeVersion;
    this.enabledFeatures = enabledFeatures == null ? Collections.emptyList() :
      Collections.unmodifiableList(enabledFeatures);
    this.actors = actors == null ? Collections.emptyList() : Collections.unmodifiableList(actors);
    this.attributes = attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(attributes);
    this.components = components == null ? Collections.emptyList() : Collections.unmodifiableList(components);
    this.httpEndpoints = httpEndpoints == null ? Collections.emptyList() : Collections.unmodifiableList(httpEndpoints);
    this.subscriptions = subscriptions == null ? Collections.emptyList() : Collections.unmodifiableList(subscriptions);
    this.appConnectionProperties = appConnectionProperties;
  }

  public String getId() {
    return id;
  }

  public String getRuntimeVersion() {
    return runtimeVersion;
  }

  public List<String> getEnabledFeatures() {
    return enabledFeatures;
  }

  public List<ActorMetadata> getActors() {
    return actors;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public List<ComponentMetadata> getComponents() {
    return components;
  }

  public List<HttpEndpointMetadata> getHttpEndpoints() {
    return httpEndpoints;
  }

  public List<SubscriptionMetadata> getSubscriptions() {
    return subscriptions;
  }

  public AppConnectionPropertiesMetadata getAppConnectionProperties() {
    return appConnectionProperties;
  }
  
}
