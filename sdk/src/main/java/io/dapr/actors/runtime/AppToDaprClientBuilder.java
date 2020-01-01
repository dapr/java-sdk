/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import io.dapr.client.AbstractClientBuilder;
import okhttp3.OkHttpClient;

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
        return new AppToDaprHttpAsync(super.getHost(),
            super.getPort(),
            super.getThreadPoolSize(),
            super.getOkHttpClientBuilder());
    }
}
