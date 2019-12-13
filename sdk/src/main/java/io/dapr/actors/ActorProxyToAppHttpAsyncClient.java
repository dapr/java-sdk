/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

/**
 * Http client to call actors methods.
 */
class ActorProxyToAppHttpAsyncClient extends DaprClientBase implements ActorProxyToAppAsyncClient {


    /**
     * Creates a new instance of {@link ActorProxyToAppHttpAsyncClient}.
     * @param port Port for calling Dapr. (e.g. 3500)
     * @param httpClient RestClient used for all API calls in this new instance.
     */
    public ActorProxyToAppHttpAsyncClient(int port, OkHttpClient httpClient)
    {
        super(port, httpClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload) {
        String url = String.format(Constants.ACTOR_METHOD_RELATIVE_URL_FORMAT, actorType, actorId, methodName);
        return invokeAPI("PUT", url, jsonPayload);
    }
}
