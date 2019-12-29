/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import io.dapr.client.AbstractClientBuilder;
import okhttp3.OkHttpClient;

/**
 * Builds an instance of ActorProxyAsyncClient.
 */
class ActorProxyClientBuilder extends AbstractClientBuilder {

    /**
     * Builds an async client.
     *
     * @return Builds an async client.
     */
    public ActorProxyAsyncClient buildAsyncClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // TODO: Expose configurations for OkHttpClient or com.microsoft.rest.RestClient.
        return new ActorProxyHttpAsyncClient(super.getPort(), builder.build());
    }
}
