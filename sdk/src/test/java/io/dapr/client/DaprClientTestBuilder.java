/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.DaprGrpc;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.Constants;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Builder for DaprClient used in tests only.
 */
public class DaprClientTestBuilder {

    /**
     * Builds a DaprClient.
     * @param client DaprHttp used for http calls (can be mocked or stubbed)
     * @return New instance of DaprClient.
     */
    public static DaprClient buildHttpClient(DaprHttp client) {
        return new DaprClientHttpAdapter(client);
    }

    /**
     * Builds a DaprGrpcClient.
     * @return New instance of DaprClient.
     */
    public static DaprClient buildGrpcClient(){
       int gprcPort = Integer.parseInt(System.getenv(Constants.ENV_DAPR_GRPC_PORT));
        ManagedChannel channel = ManagedChannelBuilder.forAddress(Constants.DEFAULT_HOSTNAME, gprcPort).usePlaintext().build();
        return new DaprClientGrpcAdapter(DaprGrpc.newFutureStub(channel),
          new DefaultObjectSerializer(),
          new DefaultObjectSerializer());
    }
}
