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
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // TODO: Expose configurations for OkHttpClient or com.microsoft.rest.RestClient.
        return new AppToDaprHttpAsyncClient(super.getPort(), builder.build());
    }
}
