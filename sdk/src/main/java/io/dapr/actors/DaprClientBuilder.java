/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors;

import okhttp3.OkHttpClient;
import io.dapr.actors.*;

/**
 * Builds an instance of DaprAsyncClient or DaprClient.
 */
public class DaprClientBuilder {

    /**
     * Default port for Dapr after checking environment variable.
     */
    private int port = DaprClientBuilder.GetEnvPortOrDefault();

    /**
     * Builds an async client.
     * @return Builds an async client.
     */
    public ActorProxyToAppAsyncClient buildAsyncClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // TODO: Expose configurations for OkHttpClient or com.microsoft.rest.RestClient.
        return new ActorProxyToAppHttpAsyncClient(this.port, builder.build());
    }

    /**
     * Overrides the port.
     * @param port New port.
     * @return This instance.
     */
    public DaprClientBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Tries to get a valid port from environment variable or returns default.
     * @return Port defined in env variable or default.
     */
    private static int GetEnvPortOrDefault() {
        String envPort = System.getenv(Constants.ENV_DAPR_HTTP_PORT);
        if (envPort == null) {
            return Constants.DEFAULT_PORT;
        }

        try {
            return Integer.parseInt(envPort.trim());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return Constants.DEFAULT_PORT;
    }
}
