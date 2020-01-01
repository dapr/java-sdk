/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import io.dapr.client.ClientRequestBuilder;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.utils.Constants;
import okhttp3.OkHttpClient;
import reactor.core.publisher.Mono;

/**
 * Http client to call actors methods.
 */
class ActorProxyHttpAsyncClient implements ActorProxyAsyncClient {

    private DaprClient daprClient;

    /**
     * Creates a new instance of {@link ActorProxyHttpAsyncClient}.
     *
     * @param port       Port for calling Dapr. (e.g. 3500)
     * @param httpClient RestClient used for all API calls in this new instance.
     */
    ActorProxyHttpAsyncClient(String host, int port, int threadPoolSize, OkHttpClient.Builder okHttpClientBuilder) {
        DaprClientBuilder clientBuilder = new DaprClientBuilder(DaprClientBuilder.DaprClientTypeEnum.HTTP)
            .withHost(host)
            .withPort(port)
            .withHttpThreadPoolSize(threadPoolSize)
            .withHttpClientbuilder(okHttpClientBuilder);
        daprClient = clientBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload) {
        String url = String.format(Constants.ACTOR_METHOD_RELATIVE_URL_FORMAT, actorType, actorId, methodName);
        ClientRequestBuilder<String> clientRequestBuilder = new ClientRequestBuilder<>()
            .withBody(jsonPayload)
            .withHttpMethod("PUT")
            .withHttpUrl(url);
        return daprClient.invokeService(clientRequestBuilder.build(), String.class);
    }
}
