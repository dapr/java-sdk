/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import io.dapr.actors.*;
import okhttp3.*;
import reactor.core.publisher.Mono;

/**
 * Http client to call actors methods.
 */
class ActorProxyHttpAsyncClient extends AbstractDaprClient implements ActorProxyAsyncClient {

  /**
   * Creates a new instance of {@link ActorProxyHttpAsyncClient}.
   *
   * @param port Port for calling Dapr. (e.g. 3500)
   * @param httpClient RestClient used for all API calls in this new instance.
   */
  ActorProxyHttpAsyncClient(int port, OkHttpClient httpClient) {
    super(port, httpClient);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload) {
    String url = String.format(Constants.ACTOR_METHOD_RELATIVE_URL_FORMAT, actorType, actorId, methodName);
    return super.invokeAPI("PUT", url, jsonPayload);
  }
}
