/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.client;

import io.dapr.client.domain.ActorMetadata;
import io.dapr.client.domain.AppConnectionPropertiesHealthMetadata;
import io.dapr.client.domain.AppConnectionPropertiesMetadata;
import io.dapr.client.domain.ComponentMetadata;
import io.dapr.client.domain.DaprMetadata;
import io.dapr.client.domain.HttpEndpointMetadata;
import io.dapr.client.domain.RuleMetadata;
import io.dapr.client.domain.SubscriptionMetadata;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.DaprMetadataProtos;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Metadata API call of {@link DaprClientImpl} and the mapping of its gRPC response to {@link DaprMetadata}.
 */
final class DaprMetadataClient {

  private final DaprGrpcCalls grpcCalls;

  DaprMetadataClient(DaprGrpcCalls grpcCalls) {
    this.grpcCalls = grpcCalls;
  }

  Mono<DaprMetadata> getMetadata() {
    DaprMetadataProtos.GetMetadataRequest metadataRequest = DaprMetadataProtos.GetMetadataRequest.newBuilder().build();
    return Mono.deferContextual(
            context -> grpcCalls.<DaprMetadataProtos.GetMetadataResponse>createMono(
                it -> grpcCalls.intercept(context).getMetadata(metadataRequest, it)))
        .map(
            it -> {
              try {
                return buildDaprMetadata(it);
              } catch (IOException ex) {
                throw DaprException.propagate(ex);
              }
            });
  }

  private DaprMetadata buildDaprMetadata(DaprMetadataProtos.GetMetadataResponse response) throws IOException {
    String id = response.getId();
    String runtimeVersion = response.getRuntimeVersion();
    List<String> enabledFeatures = response.getEnabledFeaturesList();
    List<ActorMetadata> actors = getActors(response);
    Map<String, String> attributes = response.getExtendedMetadataMap();
    List<ComponentMetadata> components = getComponents(response);
    List<HttpEndpointMetadata> httpEndpoints = getHttpEndpoints(response);
    List<SubscriptionMetadata> subscriptions = getSubscriptions(response);
    AppConnectionPropertiesMetadata appConnectionProperties = getAppConnectionProperties(response);

    return new DaprMetadata(id, runtimeVersion, enabledFeatures, actors, attributes, components, httpEndpoints,
      subscriptions, appConnectionProperties);
  }

  private List<ActorMetadata> getActors(DaprMetadataProtos.GetMetadataResponse response) {
    DaprMetadataProtos.ActorRuntime actorRuntime = response.getActorRuntime();
    List<DaprMetadataProtos.ActiveActorsCount> activeActorsList = actorRuntime.getActiveActorsList();

    List<ActorMetadata> actors = new ArrayList<>();
    for (DaprMetadataProtos.ActiveActorsCount aac : activeActorsList) {
      actors.add(new ActorMetadata(aac.getType(), aac.getCount()));
    }

    return actors;
  }

  private List<ComponentMetadata> getComponents(DaprMetadataProtos.GetMetadataResponse response) {
    List<DaprMetadataProtos.RegisteredComponents> registeredComponentsList = response.getRegisteredComponentsList();

    List<ComponentMetadata> components = new ArrayList<>();
    for (DaprMetadataProtos.RegisteredComponents rc : registeredComponentsList) {
      components.add(new ComponentMetadata(rc.getName(), rc.getType(), rc.getVersion(), rc.getCapabilitiesList()));
    }

    return components;
  }

  private List<SubscriptionMetadata> getSubscriptions(DaprMetadataProtos.GetMetadataResponse response) {
    List<DaprMetadataProtos.PubsubSubscription> subscriptionsList = response.getSubscriptionsList();

    List<SubscriptionMetadata> subscriptions = new ArrayList<>();
    for (DaprMetadataProtos.PubsubSubscription s : subscriptionsList) {
      List<DaprMetadataProtos.PubsubSubscriptionRule> rulesList = s.getRules().getRulesList();
      List<RuleMetadata> rules = new ArrayList<>();
      for (DaprMetadataProtos.PubsubSubscriptionRule r : rulesList) {
        rules.add(new RuleMetadata(r.getMatch(), r.getPath()));
      }
      subscriptions.add(new SubscriptionMetadata(s.getPubsubName(), s.getTopic(), s.getMetadataMap(), rules,
          s.getDeadLetterTopic()));
    }

    return subscriptions;
  }

  private List<HttpEndpointMetadata> getHttpEndpoints(DaprMetadataProtos.GetMetadataResponse response) {
    List<DaprMetadataProtos.MetadataHTTPEndpoint> httpEndpointsList = response.getHttpEndpointsList();

    List<HttpEndpointMetadata> httpEndpoints = new ArrayList<>();
    for (DaprMetadataProtos.MetadataHTTPEndpoint m : httpEndpointsList) {
      httpEndpoints.add(new HttpEndpointMetadata(m.getName()));
    }

    return httpEndpoints;
  }

  private AppConnectionPropertiesMetadata getAppConnectionProperties(DaprMetadataProtos.GetMetadataResponse response) {
    DaprMetadataProtos.AppConnectionProperties appConnectionProperties = response.getAppConnectionProperties();
    int port = appConnectionProperties.getPort();
    String protocol = appConnectionProperties.getProtocol();
    String channelAddress = appConnectionProperties.getChannelAddress();
    int maxConcurrency = appConnectionProperties.getMaxConcurrency();
    AppConnectionPropertiesHealthMetadata health = getAppConnectionPropertiesHealth(appConnectionProperties);

    return new AppConnectionPropertiesMetadata(port, protocol, channelAddress, maxConcurrency, health);
  }

  private AppConnectionPropertiesHealthMetadata getAppConnectionPropertiesHealth(
      DaprMetadataProtos.AppConnectionProperties appConnectionProperties) {
    if (!appConnectionProperties.hasHealth()) {
      return null;
    }

    DaprMetadataProtos.AppConnectionHealthProperties health = appConnectionProperties.getHealth();
    String healthCheckPath = health.getHealthCheckPath();
    String healthProbeInterval = health.getHealthProbeInterval();
    String healthProbeTimeout = health.getHealthProbeTimeout();
    int healthThreshold = health.getHealthThreshold();

    return new AppConnectionPropertiesHealthMetadata(healthCheckPath, healthProbeInterval, healthProbeTimeout,
        healthThreshold);
  }
}
