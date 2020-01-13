/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.DaprGrpc;
import io.dapr.utils.Constants;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import okhttp3.OkHttpClient;

/**
 * A builder for the DaprClient,
 * Currently only and HTTP Client will be supported.
 */
public class DaprClientBuilder {

    /**
     * Default port for Dapr after checking environment variable.
     */
    private static final int port = DaprClientBuilder.getEnvPortOrDefault();

    /**
     * Unique instance of httpClient to be shared.
     */
    private static volatile DaprClientHttpAdapter daprHttClient;

    /**
     * Tries to get a valid port from environment variable or returns default.
     *
     * @return Port defined in env variable or default.
     */
    private static int getEnvPortOrDefault() {
        String envPort = System.getenv(Constants.ENV_DAPR_HTTP_PORT);
        if (envPort == null || envPort.trim().isEmpty()) {
            return Constants.DEFAULT_PORT;
        }

        try {
            return Integer.parseInt(envPort.trim());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return Constants.DEFAULT_PORT;
    }

    /**
     * Build an instance of the Client based on the provided setup.
     *
     * @return an instance of the setup Client
     * @throws java.lang.IllegalStateException if any required field is missing
     */
    public DaprClient build() {
        return buildDaprClientHttp();
    }

    /**
     * Creates an instance of the GPRC Client.
     *
     * @return the GRPC Client.
     * @throws java.lang.IllegalStateException if either host is missing or if port is missing or a negative number.
     */
    private DaprClient buildDaprClientGrpc() {
        if (port <= 0) {
            throw new IllegalStateException("Invalid port.");
        }
        ManagedChannel channel = ManagedChannelBuilder.forAddress(Constants.DEFAULT_HOSTNAME, port).usePlaintext().build();
        return new DaprClientGrpcAdapter(DaprGrpc.newFutureStub(channel));
    }

    /**
     * Creates and instance of the HTTP CLient.
     * If an okhttp3.OkHttpClient.Builder has not been provided, a defult builder will be used.
     *
     * @return
     */
    private DaprClient buildDaprClientHttp() {
        if (port <= 0) {
            throw new IllegalStateException("Invalid port.");
        }
        if (this.daprHttClient == null) {
            synchronized (DaprClientBuilder.class) {
                if (this.daprHttClient == null) {
                    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
                    DaprHttp daprHtt = new DaprHttp(Constants.DEFAULT_HOSTNAME, port, okHttpClient);
                    this.daprHttClient = new DaprClientHttpAdapter(daprHtt);
                }

            }
        }
        return this.daprHttClient;
    }
}
