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

package io.dapr.client;

import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import io.dapr.client.domain.query.Query;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Generic client interface for preview or alpha APIs in Dapr, regardless of GRPC or HTTP.
 *
 * @see io.dapr.client.DaprClientBuilder for information on how to make instance for this interface.
 */
public interface DaprPreviewClient extends AutoCloseable {

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
   * Retrieve List of configurations based on a provided variable number of keys.
   *
   * @param storeName Name of the configuration store
   * @param keys      keys of the configurations which are to be retrieved
   * @return Mono of List of ConfigurationItems
   */
  Mono<List<ConfigurationItem>> getConfiguration(String storeName, String... keys);

  /**
   * Retrieve List of configurations based on a provided variable number of keys.
   *
   * @param storeName Name of the configuration store
   * @param keys      keys of the configurations which are to be retrieved
   * @param metadata  optional metadata
   * @return Mono of List of ConfigurationItems
   */
  Mono<List<ConfigurationItem>> getConfiguration(String storeName, List<String> keys, Map<String, String> metadata);

  /**
   * Retrieve List of configurations based on a provided configuration request object.
   *
   * @param request request for retrieving Configurations for a list keys
   * @return Mono of List of ConfigurationItems
   */

  Mono<List<ConfigurationItem>> getConfiguration(GetConfigurationRequest request);

  /**
   * Subscribe to the keys for any change.
   *
   * @param storeName Name of the configuration store
   * @param keys      keys of the configurations which are to be subscribed
   * @return Flux of List of configuration items
   */
  Flux<List<ConfigurationItem>> subscribeToConfiguration(String storeName, String... keys);

  /**
   * Subscribe to the keys for any change.
   *
   * @param storeName Name of the configuration store
   * @param keys      keys of the configurations which are to be subscribed
   * @param metadata  optional metadata
   * @return Flux of List of configuration items
   */
  Flux<List<ConfigurationItem>> subscribeToConfiguration(String storeName, List<String> keys,
                                                         Map<String, String> metadata);

  /**
   * Subscribe to the keys for any change.
   *
   * @param request request for subscribing to any change for the given keys in request
   * @return Flux of List of configuration items
   */
  Flux<List<ConfigurationItem>> subscribeToConfiguration(SubscribeConfigurationRequest request);

  /**
   * Query for states using a query string.
   *
   * @param storeName Name of the state store to query.
   * @param query String value of the query.
   * @param metadata Optional metadata passed to the state store.
   * @param clazz The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, String query,
                                             Map<String, String> metadata, Class<T> clazz);

  /**
   * Query for states using a query string.
   *
   * @param storeName Name of the state store to query.
   * @param query String value of the query.
   * @param metadata Optional metadata passed to the state store.
   * @param type The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, String query,
                                             Map<String, String> metadata, TypeRef<T> type);

  /**
   * Query for states using a query string.
   *
   * @param storeName Name of the state store to query.
   * @param query String value of the query.
   * @param clazz The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, String query, Class<T> clazz);

  /**
   * Query for states using a query string.
   *
   * @param storeName Name of the state store to query.
   * @param query String value of the query.
   * @param type The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, String query, TypeRef<T> type);

  /**
   * Query for states using a query domain object.
   *
   * @param storeName Name of the state store to query.
   * @param query Query value domain object.
   * @param metadata Optional metadata passed to the state store.
   * @param clazz The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, Query query,
                                             Map<String, String> metadata, Class<T> clazz);

  /**
   * Query for states using a query domain object.
   *
   * @param storeName Name of the state store to query.
   * @param query Query value domain object.
   * @param metadata Optional metadata passed to the state store.
   * @param type The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, Query query,
                                             Map<String, String> metadata, TypeRef<T> type);

  /**
   * Query for states using a query domain object.
   *
   * @param storeName Name of the state store to query.
   * @param query Query value domain object.
   * @param clazz The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, Query query, Class<T> clazz);

  /**
   * Query for states using a query domain object.
   *
   * @param storeName Name of the state store to query.
   * @param query Query value domain object.
   * @param type The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, Query query, TypeRef<T> type);

  /**
   * Query for states using a query request.
   *
   * @param request Query request object.
   * @param clazz The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(QueryStateRequest request, Class<T> clazz);

  /**
   * Query for states using a query request.
   *
   * @param request Query request object.
   * @param type The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(QueryStateRequest request, TypeRef<T> type);
}
