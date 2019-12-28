/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import reactor.core.publisher.Mono;

/**
 * Generic Client Adapter to be used regardless of the GRPC or the HTTP Client implementation required.
 *
 * @see io.dapr.client.DaprClientBuilder for information on how to make instance for this interface.
 */
public interface DaprClient {

    /**
     * Publish an event.
     *
     * @param event the event to be published
     * @param <T>   The type of event to be publishded.
     * @return a Mono plan of type Void
     */
    <T> Mono<Void> publishEvent(T event);

    /**
     * Invoke a service
     *
     * @param request The request to be sent to invoke the service
     * @param clazz   the Type needed as return for the call
     * @param <T>     the Type of the return
     * @param <K>     The Type of the request.
     * @return A Mono Plan of type clazz
     */
    <T, K> Mono<T> invokeService(K request, Class<T> clazz);

    /**
     * Creating a Binding
     *
     * @param request the request needed for the binding
     * @param <T>     The type of the request.
     * @return a Mono plan of type Void
     */
    <T> Mono<Void> invokeBinding(T request);

    /**
     * Retrieve a State based on their key.
     *
     * @param key   The key of the State to be retrieved
     * @param clazz the Type of State needed as return.
     * @param <T>   the Type of the return
     * @param <K>   The Type of the key of the State
     * @return A Mono Plan for the requested State
     */
    <T, K> Mono<T> getState(K key, Class<T> clazz);

    /**
     * Save/Update a State.
     *
     * @param state the State to be saved
     * @param <T>   the Type of the State
     * @return a Mono plan of type Void
     */
    <T> Mono<Void> saveState(T state);

    /**
     * Delete a state
     *
     * @param key The key of the State to be removed
     * @param <T> The Type of the key of the State
     * @return a Mono plan of type Void
     */
    <T> Mono<Void> deleteState(T key);
}
