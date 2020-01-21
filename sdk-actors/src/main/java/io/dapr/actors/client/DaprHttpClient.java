/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import io.dapr.client.DaprHttp;
import io.dapr.utils.Constants;
import reactor.core.publisher.Mono;

/**
 * DaprClient over HTTP for actor client.
 *
 * @see DaprHttp
 */
class DaprHttpClient implements DaprClient {

  /**
   * The HTTP client to be used
   *
   * @see DaprHttp
   */
  private final DaprHttp client;

  /**
   * Instantiates a new Dapr Http Client to invoke Actors
   *
   * @param client Dapr's http client.
   */
  DaprHttpClient(DaprHttp client) {
    this.client = client;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeActorMethod(String actorType, String actorId, String methodName, byte[] jsonPayload) {
    String url = String.format(Constants.ACTOR_METHOD_RELATIVE_URL_FORMAT, actorType, actorId, methodName);
    Mono<DaprHttp.Response> responseMono =
      this.client.invokeAPI(DaprHttp.HttpMethods.POST.name(), url, null, jsonPayload, null);
    return responseMono.map(r -> r.getBody());
  }

}
