/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors;

import okhttp3.OkHttpClient;

/**
 * Builds an instance of DaprAsyncClient or DaprClient.
 */
class DaprClientBuilder {

    /**
     * Default hostname for Dapr.
     */
    private String hostname = Constants.DEFAULT_HOSTNAME;

    /**
     * Default port for Dapr after checking environment variable.
     */
    private int port = DaprClientBuilder.GetEnvPortOrDefault();

    /**
     * Builds an async client.
     * @return Builds an async client.
     */
    public DaprAsyncClient buildAsyncClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // TODO: Expose configurations for OkHttpClient or com.microsoft.rest.RestClient.
        String baseUrl = String.format("http://%s:%d/", this.hostname, this.port);
        return new DaprHttpAsyncClient(baseUrl, builder.build());
    }

    /**
     * Overrides the hostname.
     * @param hostname new hostname.
     * @return This instance.
     */
    public DaprClientBuilder withHostname(String hostname) {
        this.hostname = hostname;
        return this;
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
