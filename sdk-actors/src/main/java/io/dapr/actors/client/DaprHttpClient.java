/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.client.DaprHttp;
import reactor.core.publisher.Mono;

/**
 * DaprClient over HTTP for actor client.
 *
 * @see DaprHttp
 */
class DaprHttpClient implements DaprClient {

  /**
   * The HTTP client to be used.
   *
   * @see DaprHttp
   */
  private final DaprHttp client;

  /**
   * Instantiates a new Dapr Http Client to invoke Actors.
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
  public Mono<byte[]> invoke(String actorType, String actorId, String methodName, byte[] jsonPayload) {
    String[] pathSegments = new String[] { DaprHttp.API_VERSION, "actors", actorType, actorId, "method", methodName };
    Mono<DaprHttp.Response> responseMono =
          this.client.invokeApi(DaprHttp.HttpMethods.POST.name(), pathSegments, null, jsonPayload, null, null);
    return responseMono.map(r -> r.getBody());
  }
}
