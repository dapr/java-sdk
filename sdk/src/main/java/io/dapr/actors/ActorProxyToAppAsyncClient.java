/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors;

import reactor.core.publisher.Mono;

/**
 * Interface to invoke actor methods.
 */
interface ActorProxyToAppAsyncClient {

    /**
     * Invokes an Actor method on Dapr.
     * @param actorType Type of actor.
     * @param actorId Actor Identifier.
     * @param methodName Method name to invoke.
     * @param jsonPayload Serialized body.
     * @return Asynchronous result with the Actor's response.
     */
    Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload);
}
