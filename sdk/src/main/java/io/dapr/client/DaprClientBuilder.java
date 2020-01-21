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

import java.time.Duration;
import java.time.temporal.TemporalUnit;

/**
 * A builder for the DaprClient,
 * Currently only and HTTP Client will be supported.
 */
public class DaprClientBuilder {

    /**
     * HTTP port for Dapr after checking environment variable.
     */
    private static final int HTTP_PORT = DaprClientBuilder.getEnvHttpPortOrDefault(
      Constants.ENV_DAPR_HTTP_PORT, Constants.DEFAULT_HTTP_PORT);

    /**
     * GRPC port for Dapr after checking environment variable.
     */
    private static final int GRPC_PORT = DaprClientBuilder.getEnvHttpPortOrDefault(
      Constants.ENV_DAPR_GRPC_PORT, Constants.DEFAULT_GRPC_PORT);

    /**
     * Default serializer.
     */
    private static final DaprObjectSerializer DEFAULT_SERIALIZER = new DefaultObjectSerializer();

    /**
     * Serializer used for objects in DaprClient.
     */
    private final DaprObjectSerializer serializer;

    /**
     * Finds the port defined by env variable or sticks to default.
     * @param envName Name of env variable with the port.
     * @param defaultPort Default port if cannot find a valid port.
     *
     * @return Port from env variable or default.
     */
    private static int getEnvHttpPortOrDefault(String envName, int defaultPort) {
        String envPort = System.getenv(envName);
        if (envPort == null || envPort.trim().isEmpty()) {
            return defaultPort;
        }

        try {
            return Integer.parseInt(envPort.trim());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return defaultPort;
    }

    /**
     * Creates a constructor for DaprClient.
     *
     * @param serializer Serializer for objects to be sent and received from Dapr.
     */
    public DaprClientBuilder(DaprObjectSerializer serializer) {
        if (serializer == null) {
            throw new IllegalArgumentException("Serializer is required");
        }

        this.serializer = serializer;
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
        if (GRPC_PORT <= 0) {
            throw new IllegalStateException("Invalid port.");
        }
        ManagedChannel channel = ManagedChannelBuilder.forAddress(Constants.DEFAULT_HOSTNAME, GRPC_PORT).usePlaintext().build();
        return new DaprClientGrpcAdapter(DaprGrpc.newFutureStub(channel), new DefaultObjectSerializer());
    }

    /**
     * Creates and instance of DaprClient over HTTP.
     *
     * @return DaprClient over HTTP.
     */
    private DaprClient buildDaprClientHttp() {
        if (HTTP_PORT <= 0) {
            throw new IllegalStateException("Invalid port.");
        }
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        DaprHttp daprHttp = new DaprHttp(HTTP_PORT, okHttpClient);
        return new DaprClientHttpAdapter(daprHttp, this.serializer);
    }
}
