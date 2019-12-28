/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.utils.Constants;

/**
 * Base class for client builders
 */
public abstract class AbstractClientBuilder {

    /**
     * Default port for Dapr after checking environment variable.
     */
    private int port = AbstractClientBuilder.GetEnvPortOrDefault();

    /**
     * Overrides the port.
     *
     * @param port New port.
     * @return This instance.
     */
    public AbstractClientBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Returns configured port.
     *
     * @return Port to connect to Dapr.
     */
    protected int getPort() {
        return this.port;
    }

    /**
     * Tries to get a valid port from environment variable or returns default.
     *
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
