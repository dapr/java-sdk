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
 * Http client to call Dapr's API for actors.
 */
//public class DaprHttpAsyncClient implements DaprAsyncClient {
public class AppToDaprHttpAsyncClient extends DaprClientBase implements AppToDaprAsyncClient {

    /**
     * Defines the standard application/json type for HTTP calls in Dapr.
     */
    private static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * Shared object representing an empty request body in JSON.
     */
    private static final RequestBody REQUEST_BODY_EMPTY_JSON = RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, "");

    /**
     * JSON Object Mapper.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /**
     * Base Url for calling Dapr. (e.g. http://localhost:3500/)
     */
    private final String baseUrl;

    /**
     * Http client used for all API calls.
     */
    private final OkHttpClient httpClient;

    /**
     * Creates a new instance of {@link AppToDaprHttpAsyncClient}.
     * @param port Port for calling Dapr. (e.g. 3500)
     * @param httpClient RestClient used for all API calls in this new instance.
     */
    public AppToDaprHttpAsyncClient(int port, OkHttpClient httpClient)
    {
        this.baseUrl = String.format("http://%s:%d/", Constants.DEFAULT_HOSTNAME, port);;
        this.httpClient = httpClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload) {
        String url = String.format(Constants.ACTOR_METHOD_RELATIVE_URL_FORMAT, actorType, actorId, methodName);
        return invokeAPI("PUT", url, jsonPayload);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> getState(String actorType, String actorId, String keyName) {
        String url = String.format(Constants.ACTOR_STATE_KEY_RELATIVE_URL_FORMAT, actorType, actorId, keyName);
        return invokeAPI("GET", url, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> removeState(String actorType, String actorId, String keyName) {
        String url = String.format(Constants.ACTOR_STATE_KEY_RELATIVE_URL_FORMAT, actorType, actorId, keyName);
        return invokeAPIVoid("DELETE", url, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> saveStateTransactionally(String actorType, String actorId, String data) {
        String url = String.format(Constants.ACTOR_STATE_RELATIVE_URL_FORMAT, actorType, actorId);
        return invokeAPIVoid("PUT", url, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> registerReminder(String actorType, String actorId, String reminderName, String data) {
        String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
        return invokeAPIVoid("PUT", url, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName) {
        String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
        return invokeAPIVoid("DELETE", url, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> registerTimer(String actorType, String actorId, String timerName, String data) {
        String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
        return invokeAPIVoid("PUT", url, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> unregisterTimerAsync(String actorType, String actorId, String timerName) {
        String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
        return invokeAPIVoid("DELETE", url, null);
    }
}
