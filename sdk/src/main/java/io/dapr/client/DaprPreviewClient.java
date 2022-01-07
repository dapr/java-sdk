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

package io.dapr.client;

import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetBulkConfigurationRequest;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface DaprPreviewClient extends AutoCloseable {


  /**
   * Waits for the sidecar, giving up after timeout.
   *
   * @param timeoutInMilliseconds Timeout in milliseconds to wait for sidecar.
   * @return a Mono plan of type Void.
   */
  Mono<Void> waitForSidecar(int timeoutInMilliseconds);

  /**
   * Gracefully shutdown the dapr runtime.
   *
   * @return a Mono plan of type Void.
   */
  Mono<Void> shutdown();

  /**
   * Retrieve a configuration based on a provided key.
   *
   * @param storeName Name of the configuration store
   * @param key       key of the configuration item which is to be retrieved
   * @return Mono of the Configuration Item
   */
  Mono<ConfigurationItem> getConfiguration(String storeName, String key);

  /**
   * Retrieve a configuration based on a provided key.
   *
   * @param storeName Name of the configuration store
   * @param key       key of the configuration item which is to be retrieved
   * @param metadata  optional metadata
   * @return Mono of the Configuration Item
   */
  Mono<ConfigurationItem> getConfiguration(String storeName, String key, Map<String, String> metadata);

  /**
   * Retrieve a configuration based on a provided request object.
   *
   * @param request request for retrieving Configuration for a single key
   * @return Mono of the Configuration Item
   */
  Mono<ConfigurationItem> getConfiguration(GetConfigurationRequest request);

  /**
   * Retrieve List of configurations based on a provided variable number of keys.
   *
   * @param storeName Name of the configuration store
   * @param keys      keys of the configurations which are to be retrieved
   * @return Mono of List of ConfigurationItems
   */
  Mono<List<ConfigurationItem>> getConfigurations(String storeName, String... keys);

  /**
   * Retrieve List of configurations based on a provided variable number of keys.
   *
   * @param storeName Name of the configuration store
   * @param keys      keys of the configurations which are to be retrieved
   * @param metadata  optional metadata
   * @return Mono of List of ConfigurationItems
   */
  Mono<List<ConfigurationItem>> getConfigurations(String storeName, List<String> keys, Map<String, String> metadata);

  /**
   * Retrieve List of configurations based on a provided configuration request object.
   *
   * @param request request for retrieving Configurations for a list keys
   * @return Mono of List of ConfigurationItems
   */

  Mono<List<ConfigurationItem>> getConfigurations(GetBulkConfigurationRequest request);

  /**
   * Subscribe to the keys for any change.
   *
   * @param storeName Name of the configuration store
   * @param keys      keys of the configurations which are to be subscribed
   * @return Flux of List of configuration items
   */
  Flux<List<ConfigurationItem>> subscribeToConfigurations(String storeName, String... keys);

  /**
   * Subscribe to the keys for any change.
   *
   * @param storeName Name of the configuration store
   * @param keys      keys of the configurations which are to be subscribed
   * @param metadata  optional metadata
   * @return Flux of List of configuration items
   */
  Flux<List<ConfigurationItem>> subscribeToConfigurations(String storeName, List<String> keys,
                                                          Map<String, String> metadata);

  /**
   * Subscribe to the keys for any change.
   *
   * @param request request for subscribing to any change for the given keys in request
   * @return Flux of List of configuration items
   */
  Flux<List<ConfigurationItem>> subscribeToConfigurations(SubscribeConfigurationRequest request);
}
