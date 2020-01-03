/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import io.dapr.client.AbstractClientBuilder;

/**
 * Builds an instance of AppToDaprAsyncClient.
 */
class AppToDaprClientBuilder extends AbstractClientBuilder {

    /**
     * Builds an async client.
     *
     * @return Builds an async client.
     */
    public AppToDaprAsyncClient buildAsyncClient() {
        return new AppToDaprHttpAsyncClient(super.getHost(),
            super.getPort(),
            super.getThreadPoolSize(),
            super.getOkHttpClientBuilder());
    }
}
