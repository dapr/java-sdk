/*
 * Copyright (c) Microsoft Corporation.
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
   * Base URL for Dapr Actor APIs.
   */
  private static final String ACTORS_BASE_URL = DaprHttp.API_VERSION + "/" + "actors";

  /**
   * String format for Actors method invocation relative url.
   */
  private static final String ACTOR_METHOD_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/method/%s";

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
    String url = String.format(ACTOR_METHOD_RELATIVE_URL_FORMAT, actorType, actorId, methodName);
    Mono<DaprHttp.Response> responseMono =
          this.client.invokeApi(DaprHttp.HttpMethods.POST.name(), url, null, jsonPayload, null, null);
    return responseMono.map(r -> r.getBody());
  }
}
