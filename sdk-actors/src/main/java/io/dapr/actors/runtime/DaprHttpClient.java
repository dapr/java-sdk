/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.client.DaprHttp;
import reactor.core.publisher.Mono;

/**
 * A DaprClient over HTTP for Actor's runtime.
 */
class DaprHttpClient implements DaprClient {

  /**
   * Base URL for Dapr Actor APIs.
   */
  private static final String ACTORS_BASE_URL = DaprHttp.API_VERSION + "/" + "actors";

  /**
   * String format for Actors state management relative url.
   */
  private static final String ACTOR_STATE_KEY_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/state/%s";

  /**
   * String format for Actors state management relative url.
   */
  private static final String ACTOR_STATE_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/state";

  /**
   * String format for Actors reminder registration relative url.
   */
  private static final String ACTOR_REMINDER_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/reminders/%s";

  /**
   * String format for Actors timer registration relative url.
   */
  private static final String ACTOR_TIMER_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/timers/%s";

  /**
   * The HTTP client to be used.
   *
   * @see DaprHttp
   */
  private final DaprHttp client;

  /**
   * Internal constructor.
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
  public Mono<byte[]> getActorState(String actorType, String actorId, String keyName) {
    String url = String.format(ACTOR_STATE_KEY_RELATIVE_URL_FORMAT, actorType, actorId, keyName);
    Mono<DaprHttp.Response> responseMono =
        this.client.invokeApi(DaprHttp.HttpMethods.GET.name(), url, null, "", null, null);
    return responseMono.map(r -> {
      if ((r.getStatusCode() != 200) && (r.getStatusCode() != 204)) {
        throw new IllegalStateException(
            String.format("Error getting actor state: %s/%s/%s", actorType, actorId, keyName));
      }
      return r.getBody();
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveActorStateTransactionally(String actorType, String actorId, byte[] data) {
    String url = String.format(ACTOR_STATE_RELATIVE_URL_FORMAT, actorType, actorId);
    return this.client.invokeApi(DaprHttp.HttpMethods.PUT.name(), url, null, data, null, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerActorReminder(String actorType, String actorId, String reminderName, byte[] data) {
    String url = String.format(ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return this.client.invokeApi(DaprHttp.HttpMethods.PUT.name(), url, null, data, null, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterActorReminder(String actorType, String actorId, String reminderName) {
    String url = String.format(ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return this.client.invokeApi(DaprHttp.HttpMethods.DELETE.name(), url, null, null, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerActorTimer(String actorType, String actorId, String timerName, byte[] data) {
    String url = String.format(ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return this.client.invokeApi(DaprHttp.HttpMethods.PUT.name(), url, null, data, null, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterActorTimer(String actorType, String actorId, String timerName) {
    String url = String.format(ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return this.client.invokeApi(DaprHttp.HttpMethods.DELETE.name(), url, null, null, null).then();
  }

}
