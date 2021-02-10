/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.List;
import java.util.Map;

/**
 * Stub class for DaprHttp.
 * Useful to mock as well since it provides a default constructor.
 */
public class DaprHttpStub extends DaprHttp {

    public static class ResponseStub extends DaprHttp.Response {
        public ResponseStub(byte[] body, Map<String, String> headers, int statusCode) {
            super(body, headers, statusCode);
        }
    }
    /**
     * Instantiates a stub for DaprHttp
     */
    public DaprHttpStub() {
        super(null, 3000, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Response> invokeApi(String method,
        String[] pathSegments,
        Map<String, List<String>> urlParameters,
        Map<String, String> headers,
        Context context) {
        return Mono.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Response> invokeApi(String method,
        String[] pathSegments,
        Map<String, List<String>> urlParameters,
        String content,
        Map<String, String> headers,
        Context context) {
        return Mono.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Response> invokeApi(String method,
        String[] pathSegments,
        Map<String, List<String>> urlParameters,
        byte[] content,
        Map<String, String> headers,
        Context context) {
        return Mono.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
    }
}
