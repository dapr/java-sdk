/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import okhttp3.OkHttpClient;
import io.dapr.actors.*;

/**
 * Builds an instance of AppToDaprAsyncClient.
 */
class AppToDaprClientBuilder extends AbstractClientBuilder {

    /**
     * Default port for Dapr after checking environment variable.
     */
    private int port = AppToDaprClientBuilder.GetEnvPortOrDefault();

    /**
     * Builds an async client.
     * @return Builds an async client.
     */
    public AppToDaprAsyncClient buildAsyncClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // TODO: Expose configurations for OkHttpClient or com.microsoft.rest.RestClient.
        return new AppToDaprHttpAsyncClient(this.port, builder.build());
    }
}
