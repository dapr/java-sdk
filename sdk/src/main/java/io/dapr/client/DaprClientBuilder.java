/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.DaprGrpc;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
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
     * Serializer used for request and response objects in DaprClient.
     */
    private final DaprObjectSerializer objectSerializer;

    /**
     * Serializer used for state objects in DaprClient.
     */
    private final DaprObjectSerializer stateSerializer;

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
     * See {@link DefaultObjectSerializer} as possible serializer for non-production scenarios.
     *
     * @param objectSerializer Serializer for objects to be sent and received from Dapr.
     * @param stateSerializer Serializer for objects to be persisted.
     */
    public DaprClientBuilder(DaprObjectSerializer objectSerializer, DaprObjectSerializer stateSerializer) {
        if (objectSerializer == null) {
            throw new IllegalArgumentException("Serializer is required");
        }
        if (stateSerializer == null) {
            throw new IllegalArgumentException("State serializer is required");
        }

        this.objectSerializer = objectSerializer;
        this.stateSerializer = stateSerializer;
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
        return new DaprClientGrpcAdapter(DaprGrpc.newFutureStub(channel), this.objectSerializer, this.stateSerializer);
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
        return new DaprClientHttpAdapter(daprHttp, this.objectSerializer, this.stateSerializer);
    }
}
