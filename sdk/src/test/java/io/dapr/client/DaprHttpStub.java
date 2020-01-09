/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Stub class for DaprHttp.
 * Useful to mock as well since it provides a default constructor.
 */
public class DaprHttpStub extends DaprHttp {

    /**
     * Instantiates a stub for DaprHttp
     */
    public DaprHttpStub() {
        super("http://localhost", 3000, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> invokeAPI(String method, String urlString, Map<String, String> headers) {
        return Mono.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> invokeAPI(String method, String urlString, String content, Map<String, String> headers) {
        return Mono.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> invokeAPI(String method, String urlString, byte[] content, Map<String, String> headers) {
        return Mono.empty();
    }
}
