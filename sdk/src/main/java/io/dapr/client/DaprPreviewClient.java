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

/**
 *  A Client to be used for all Preview APIs
 */
public interface DaprPreviewClient extends AutoCloseable {


    /**
     * Waits for the sidecar, giving up after timeout.
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
     * Retrieve a configuration based on a provided key
     *
     * @param storeName Name of the configuration store
     * @param key key of the configuration item which is to be retrieved
     * @return Mono of the Configuration Item
     */
    Mono<ConfigurationItem> getConfiguration(String storeName, String key);

    /**
     * Retrieve a configuration based on a provided request object
     *
     * @param request request for retrieving Configuration for a single key
     * @return Mono of the Configuration Item
     */
    Mono<ConfigurationItem> getConfiguration(GetConfigurationRequest request);

    /**
     * Retrieve List of configurations based on a provided variable number of keys
     *
     * @param storeName Name of the configuration store
     * @param keys keys of the configurations which are to be retrieved
     * @return Mono of List of ConfigurationItems
     */
    Mono<List<ConfigurationItem>> getConfigurations(String storeName, String... keys);

    /**
     * Retrieve List of configurations based on a provided configuration request object
     * @param request request for retrieving Configurations for a list keys
     * @return Mono of List of ConfigurationItems
     */
    Mono<List<ConfigurationItem>> getConfigurations(GetBulkConfigurationRequest request);

    /**
     * Subscribe to the keys for any change
     *
     * @param storeName Name of the configuration store
     * @param keys keys of the configurations which are to be subscribed
     * @return Flux of List of configuration items
     */
    Flux<List<ConfigurationItem>> subscribeToConfigurations(String storeName, String... keys);

    /**
     * Subscribe to the keys for any change
     *
     * @param request request for subscribing to any change for the given keys in request
     * @return Flux of List of configuration items
     */
    Flux<List<ConfigurationItem>> subscribeToConfigurations(SubscribeConfigurationRequest request);
}
