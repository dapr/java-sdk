/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.ClientRequestBuilder;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.Constants;
import okhttp3.OkHttpClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;

/**
 * Http client to call Dapr's API for actors.
 */
//public class DaprHttpAsyncClient implements DaprAsyncClient {
class AppToDaprHttpAsync implements AppToDaprAsyncClient {

  private DaprClient daprClient;

  /**
   * Creates a new instance of {@link AppToDaprHttpAsync}.
   *
   * @param host              The base url for calling dapr (e.g. http://localhost
   * @param port              Port for calling Dapr. (e.g. 3500)
   * @param threadPoolSize    The size of the thread pool to be used by the http async client.
   * @param httpClientBuilder The HTTPClientBuilder already configured to build the HttpClient
   */
  public AppToDaprHttpAsync(String host, int port, int threadPoolSize, OkHttpClient.Builder okHttpClientBuilder) {
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
  public Mono<String> getState(String actorType, String actorId, String keyName) {
    String url = String.format(Constants.ACTOR_STATE_KEY_RELATIVE_URL_FORMAT, actorType, actorId, keyName);
    ClientRequestBuilder<String> clientRequestBuilder = new ClientRequestBuilder<>()
        .withHttpMethod("GET")
        .withHttpUrl(url);
    return daprClient.invokeService(clientRequestBuilder.build(), String.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveStateTransactionally(String actorType, String actorId, String data) {
    String url = String.format(Constants.ACTOR_STATE_RELATIVE_URL_FORMAT, actorType, actorId);
    ClientRequestBuilder<String> clientRequestBuilder = new ClientRequestBuilder<>()
        .withBody(data)
        .withHttpMethod("PUT")
        .withHttpUrl(url);

    return daprClient.invokeService(clientRequestBuilder.build());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerReminder(String actorType, String actorId, String reminderName, String data) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    ClientRequestBuilder<String> clientRequestBuilder = new ClientRequestBuilder<>()
        .withBody(data)
        .withHttpMethod("PUT")
        .withHttpUrl(url);
    return daprClient.invokeService(clientRequestBuilder.build());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    ClientRequestBuilder<String> clientRequestBuilder = new ClientRequestBuilder<>()
        .withHttpMethod("DELETE")
        .withHttpUrl(url);
    return daprClient.invokeService(clientRequestBuilder.build());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerTimer(String actorType, String actorId, String timerName, String data) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    ClientRequestBuilder<String> clientRequestBuilder = new ClientRequestBuilder<>()
        .withBody(data)
        .withHttpMethod("PUT")
        .withHttpUrl(url);
    return daprClient.invokeService(clientRequestBuilder.build());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterTimer(String actorType, String actorId, String timerName) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    ClientRequestBuilder<String> clientRequestBuilder = new ClientRequestBuilder<>()
        .withHttpMethod("DELETE")
        .withHttpUrl(url);
    return daprClient.invokeService(clientRequestBuilder.build());
  }
}
