/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
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

public class DaprPreviewClientProxy implements DaprPreviewClient {

  /**
   * Client for all API invocations.
   */
  private final DaprPreviewClient client;

  /**
   * Client to override Dapr's service invocation APIs.
   */
  private final DaprPreviewClient methodInvocationOverrideClient;

  /**
   * Constructor with delegate client.
   *
   * @param client Client for all API invocations.
   * @see DaprPreviewClientBuilder
   */
  DaprPreviewClientProxy(DaprPreviewClient client) {
    this(client, client);
  }

  /**
   * Constructor with delegate client and override client for Dapr's method invocation APIs.
   *
   * @param client                         Client for all API invocations, except override below.
   * @param methodInvocationOverrideClient Client to override Dapr's service invocation APIs.
   * @see DaprClientBuilder
   */
  DaprPreviewClientProxy(
      DaprPreviewClient client,
      DaprPreviewClient methodInvocationOverrideClient) {
    this.client = client;
    this.methodInvocationOverrideClient = methodInvocationOverrideClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> waitForSidecar(int timeoutInMilliseconds) {
    return client.waitForSidecar(timeoutInMilliseconds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws Exception {
    client.close();
    if (client != methodInvocationOverrideClient) {
      methodInvocationOverrideClient.close();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> shutdown() {
    return client.shutdown();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<ConfigurationItem> getConfiguration(String storeName, String key) {
    return client.getConfiguration(storeName, key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<ConfigurationItem> getConfiguration(String storeName, String key, Map<String, String> metadata) {
    return client.getConfiguration(storeName, key, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<ConfigurationItem> getConfiguration(GetConfigurationRequest request) {
    return client.getConfiguration(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<ConfigurationItem>> getConfigurations(String storeName, String... keys) {
    return client.getConfigurations(storeName, keys);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<ConfigurationItem>> getConfigurations(String storeName, List<String> keys,
                                                         Map<String, String> metadata) {
    return client.getConfigurations(storeName, keys, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<ConfigurationItem>> getConfigurations(GetBulkConfigurationRequest request) {
    return client.getConfigurations(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<ConfigurationItem>> getAllConfigurations(String storeName) {
    return client.getAllConfigurations(storeName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<ConfigurationItem>> getAllConfigurations(String storeName, Map<String, String> metadata) {
    return client.getAllConfigurations(storeName, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<ConfigurationItem>> getAllConfigurations(GetBulkConfigurationRequest request) {
    return client.getAllConfigurations(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<List<ConfigurationItem>> subscribeToConfigurations(String storeName, String... keys) {
    return client.subscribeToConfigurations(storeName, keys);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<List<ConfigurationItem>> subscribeToConfigurations(String storeName, List<String> keys,
                                                                 Map<String, String> metadata) {
    return client.subscribeToConfigurations(storeName, keys, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<List<ConfigurationItem>> subscribeToConfigurations(SubscribeConfigurationRequest request) {
    return client.subscribeToConfigurations(request);
  }
}
