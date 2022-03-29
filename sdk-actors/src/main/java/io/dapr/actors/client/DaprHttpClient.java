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
