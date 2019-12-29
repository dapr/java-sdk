/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import io.dapr.client.AbstractDaprHttpClient;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.Constants;
import io.dapr.utils.ObjectSerializer;
import okhttp3.OkHttpClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Http client to call Dapr's API for actors.
 */
//public class DaprHttpAsyncClient implements DaprAsyncClient {
class AppToDaprHttpAsyncClient extends AbstractDaprHttpClient implements AppToDaprAsyncClient {

    /**
     * ObjectMapper to Serialize data
     */
    private static final ObjectSerializer MAPPER = new ObjectSerializer();

    private Map<String, String> dataMap;


    /**
     * Creates a new instance of {@link AppToDaprHttpAsyncClient}.
     *
     * @param port       Port for calling Dapr. (e.g. 3500)
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

    /**
     * Creating publishEvent for Http Client
     *
     * @param topic  HTTP method.
     * @param data   url as String.
     * @param method JSON payload or null.
     * @return Mono<String>
     */
    public Mono<String> publishEvent(String topic, String data, String method) throws Exception {

        if (topic.isEmpty() || topic == null) {
            throw new DaprException("500", "Topic cannot be null or empty.");
        }

        if (method.isEmpty() || method == null) {
            throw new DaprException("500", "Method cannot be null or empty.");
        }

        String url = method.equals("POST") ? Constants.PUBLISH_PATH : Constants.PUBLISH_PATH + "/" + topic;

        dataMap = new HashMap();
        dataMap.put(topic, data);

        String jsonResult = MAPPER.serialize(dataMap);

        return super.invokeAPI(method, url, jsonResult);
    }

    /**
     * Creating invokeBinding Method for Http Client
     *
     * @param name   HTTP method.
     * @param data   url as String.
     * @param method JSON payload or null.
     * @return Mono<String>
     */
    public Mono<String> invokeBinding(String name, String data, String method) throws Exception {

        if (name.isEmpty() || name == null) {
            throw new DaprException("500", "Name cannot be null or empty.");
        }

        if (method.isEmpty() || method == null) {
            throw new DaprException("500", "Method cannot be null or empty.");
        }

        String url = method.equals("POST") ? Constants.BINDING_PATH : Constants.BINDING_PATH + "/" + name;

        dataMap = new HashMap();
        dataMap.put(name, data);

        String jsonResult = MAPPER.serialize(dataMap);

        return super.invokeAPI(method, url, jsonResult);
    }

    /**
     * Creating invokeBinding Method for Http Client
     *
     * @param key HTTP method.
     * @return Mono<String>
     */
    public Mono<String> getState(String key) throws DaprException {

        if (key.isEmpty() || key == null) {
            throw new DaprException("500", "Name cannot be null or empty.");
        }

        String url = Constants.STATE_PATH + "/" + key;

        return super.invokeAPI("GET", url, null);
    }

    /**
     * Creating invokeBinding Method for Http Client
     *
     * @param key  HTTP method.
     * @param data HTTP method.
     * @return Mono<String>
     */
    public Mono<String> saveState(String key, String data) throws Exception {

        if (key.isEmpty() || key == null) {
            throw new DaprException("500", "Name cannot be null or empty.");
        }

        String url = Constants.STATE_PATH;

        dataMap = new HashMap();
        dataMap.put(key, data);

        String jsonResult = MAPPER.serialize(dataMap);

        return super.invokeAPI("POST", url, jsonResult);
    }

    /**
     * Creating invokeBinding Method for Http Client
     *
     * @param key HTTP method.
     * @return Mono<String>
     */
    public Mono<String> deleteState(String key) throws DaprException {

        if (key.isEmpty() || key == null) {
            throw new DaprException("500", "Name cannot be null or empty.");
        }

        String url = Constants.STATE_PATH + "/" + key;

        return super.invokeAPI("DELETE", url, null);
    }

}
