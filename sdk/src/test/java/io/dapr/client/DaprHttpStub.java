/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
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
