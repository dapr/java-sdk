/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.client.domain.ConfigurationItem;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 *  A Client to be used for all Preview APIs
 */
public interface DaprPreviewClient extends AutoCloseable {
    /**
     *
     * @param storeName
     * @param key
     * @return
     */
    Mono<ConfigurationItem> getConfigurationItem(String storeName, String key);

    /**
     *
     * @param storeName
     * @param keys
     * @return
     */
    Mono<List<ConfigurationItem>> getConfigurationItems(String storeName, List<String> keys);

    /**
     *
     * @param storeName
     * @return
     */
    Mono<List<ConfigurationItem>> getAllConfigurationItems(String storeName);
}
