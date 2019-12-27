/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import io.dapr.actors.AbstractDaprClient;
import io.dapr.actors.Constants;
import okhttp3.OkHttpClient;
import reactor.core.publisher.Mono;

/**
 * Http client to call Dapr's API for actors.
 */
class AppToDaprHttpAsyncClient extends AbstractDaprClient implements AppToDaprAsyncClient {

  /**
   * Creates a new instance of {@link AppToDaprHttpAsyncClient}.
   *
   * @param port Port for calling Dapr. (e.g. 3500)
   * @param httpClient RestClient used for all API calls in this new instance.
   */
  public AppToDaprHttpAsyncClient(int port, OkHttpClient httpClient) {
    super(port, httpClient);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<String> getState(String actorType, String actorId, String keyName) {
    String url = String.format(Constants.ACTOR_STATE_KEY_RELATIVE_URL_FORMAT, actorType, actorId, keyName);
    return super.invokeAPI("GET", url, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveStateTransactionally(String actorType, String actorId, String data) {
    String url = String.format(Constants.ACTOR_STATE_RELATIVE_URL_FORMAT, actorType, actorId);
    return super.invokeAPIVoid("PUT", url, data);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerReminder(String actorType, String actorId, String reminderName, String data) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return super.invokeAPIVoid("PUT", url, data);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return super.invokeAPIVoid("DELETE", url, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerTimer(String actorType, String actorId, String timerName, String data) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return super.invokeAPIVoid("PUT", url, data);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterTimer(String actorType, String actorId, String timerName) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return super.invokeAPIVoid("DELETE", url, null);
  }
}
