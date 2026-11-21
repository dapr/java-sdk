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

import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.client.domain.UnsubscribeConfigurationRequest;
import io.dapr.client.domain.UnsubscribeConfigurationResponse;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprConfigurationProtos;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Configuration API calls of {@link DaprClientImpl}: get, subscribe and unsubscribe.
 */
final class DaprConfigurationClient {

  private final DaprGrpcCalls grpcCalls;

  DaprConfigurationClient(DaprGrpcCalls grpcCalls) {
    this.grpcCalls = grpcCalls;
  }

  Mono<Map<String, ConfigurationItem>> getConfiguration(GetConfigurationRequest request) {
    try {
      final String configurationStoreName = request.getStoreName();
      final Map<String, String> metadata = request.getMetadata();
      final List<String> keys = request.getKeys();
      if ((configurationStoreName == null) || (configurationStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Configuration Store Name cannot be null or empty.");
      }

      DaprConfigurationProtos.GetConfigurationRequest.Builder builder = DaprConfigurationProtos.GetConfigurationRequest
          .newBuilder()
          .setStoreName(configurationStoreName).addAllKeys(keys);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprConfigurationProtos.GetConfigurationRequest envelope = builder.build();
      return this.getConfiguration(envelope);

    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private Mono<Map<String, ConfigurationItem>> getConfiguration(DaprConfigurationProtos
                                                                    .GetConfigurationRequest envelope) {
    return Mono.deferContextual(
        context ->
            grpcCalls.<DaprConfigurationProtos.GetConfigurationResponse>createMono(
                it -> grpcCalls.intercept(context).getConfiguration(envelope, it)
            )
    ).map(
        it -> {
          Map<String, ConfigurationItem> configMap = new HashMap<>();
          Iterator<Map.Entry<String, CommonProtos.ConfigurationItem>> itr = it.getItems().entrySet().iterator();
          while (itr.hasNext()) {
            Map.Entry<String, CommonProtos.ConfigurationItem> entry = itr.next();
            configMap.put(entry.getKey(), buildConfigurationItem(entry.getValue(), entry.getKey()));
          }
          return Collections.unmodifiableMap(configMap);
        }
    );
  }

  Flux<SubscribeConfigurationResponse> subscribeConfiguration(SubscribeConfigurationRequest request) {
    try {
      final String configurationStoreName = request.getStoreName();
      final List<String> keys = request.getKeys();
      final Map<String, String> metadata = request.getMetadata();

      if (configurationStoreName == null || (configurationStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Configuration Store Name can not be null or empty.");
      }

      // keys can and empty list for subscribe all scenario, so we do not need check for empty keys.
      DaprConfigurationProtos.SubscribeConfigurationRequest.Builder builder = DaprConfigurationProtos
          .SubscribeConfigurationRequest
          .newBuilder()
          .setStoreName(configurationStoreName)
          .addAllKeys(keys);

      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprConfigurationProtos.SubscribeConfigurationRequest envelope = builder.build();
      return grpcCalls.<DaprConfigurationProtos.SubscribeConfigurationResponse>createFlux(
          it -> grpcCalls.intercept(null).subscribeConfiguration(envelope, it)
      ).map(
          it -> {
            Map<String, ConfigurationItem> configMap = new HashMap<>();
            Iterator<Map.Entry<String, CommonProtos.ConfigurationItem>> itr = it.getItemsMap().entrySet().iterator();
            while (itr.hasNext()) {
              Map.Entry<String, CommonProtos.ConfigurationItem> entry = itr.next();
              configMap.put(entry.getKey(), buildConfigurationItem(entry.getValue(), entry.getKey()));
            }
            return new SubscribeConfigurationResponse(it.getId(), Collections.unmodifiableMap(configMap));
          }
      );
    } catch (Exception ex) {
      return DaprException.wrapFlux(ex);
    }
  }

  Mono<UnsubscribeConfigurationResponse> unsubscribeConfiguration(UnsubscribeConfigurationRequest request) {
    try {
      final String configurationStoreName = request.getStoreName();
      final String id = request.getSubscriptionId();

      if (configurationStoreName == null || (configurationStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Configuration Store Name can not be null or empty.");
      }
      if (id.isEmpty()) {
        throw new IllegalArgumentException("Subscription id can not be null or empty.");
      }
      DaprConfigurationProtos.UnsubscribeConfigurationRequest.Builder builder =
          DaprConfigurationProtos.UnsubscribeConfigurationRequest.newBuilder()
              .setId(id)
              .setStoreName(configurationStoreName);

      DaprConfigurationProtos.UnsubscribeConfigurationRequest envelope = builder.build();

      return grpcCalls.<DaprConfigurationProtos.UnsubscribeConfigurationResponse>createMono(
          it -> grpcCalls.intercept(null).unsubscribeConfiguration(envelope, it)
      ).map(
          it -> new UnsubscribeConfigurationResponse(it.getOk(), it.getMessage())
      );
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * Build a new Configuration Item from provided parameter.
   *
   * @param configurationItem CommonProtos.ConfigurationItem
   * @return io.dapr.client.domain.ConfigurationItem
   */
  private ConfigurationItem buildConfigurationItem(
      CommonProtos.ConfigurationItem configurationItem, String key) {
    return new ConfigurationItem(
        key,
        configurationItem.getValue(),
        configurationItem.getVersion(),
        configurationItem.getMetadataMap());
  }
}
