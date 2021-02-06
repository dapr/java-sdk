/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;

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
        return new DaprClientHttp(client);
    }
}
