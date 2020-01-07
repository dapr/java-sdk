/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorTrace;
import io.dapr.client.DaprClient;

/**
 * Provides the context for the Actor's runtime.
 *
 * @param <T> Actor's type for the context.
 */
public class ActorRuntimeContext<T extends AbstractActor> {

    /**
     * Runtime.
     */
    private final ActorRuntime actorRuntime;

    /**
     * Serializer.
     */
    private final ActorStateSerializer actorSerializer;

    /**
     * Actor factory.
     */
    private final ActorFactory<T> actorFactory;

    /**
     * Information of the Actor's type.
     */
    private final ActorTypeInformation<T> actorTypeInformation;

    /**
     * Trace for Actor logs.
     */
    private final ActorTrace actorTrace;

    /**
     * Client to communicate to Dapr's API.
     */
    private final DaprClient daprClient;

    /**
     * State provider for given Actor Type.
     */
    private final DaprStateAsyncProvider stateProvider;

    /**
     * Instantiates a new runtime context for the Actor type.
     *
     * @param actorRuntime         Runtime.
     * @param actorSerializer      Serializer.
     * @param actorFactory         Factory for Actors.
     * @param actorTypeInformation Information for Actor's type.
     * @param daprClient           Client to communicate to Dapr.
     * @param stateProvider        State provider for given Actor's type.
     */
    ActorRuntimeContext(ActorRuntime actorRuntime,
                        ActorStateSerializer actorSerializer,
                        ActorFactory<T> actorFactory,
                        ActorTypeInformation<T> actorTypeInformation,
                        DaprClient daprClient,
                        DaprStateAsyncProvider stateProvider) {
        this.actorRuntime = actorRuntime;
        this.actorSerializer = actorSerializer;
        this.actorFactory = actorFactory;
        this.actorTypeInformation = actorTypeInformation;
        this.actorTrace = new ActorTrace();
        this.daprClient = daprClient;
        this.stateProvider = stateProvider;
    }

    /**
     * Gets the Actor's runtime.
     *
     * @return Actor's runtime.
     */
    ActorRuntime getActorRuntime() {
        return this.actorRuntime;
    }

    /**
     * Gets the Actor's serializer.
     *
     * @return Actor's serializer.
     */
    ActorStateSerializer getActorSerializer() {
        return this.actorSerializer;
    }

    /**
     * Gets the Actor's serializer.
     *
     * @return Actor's serializer.
     */
    ActorFactory<T> getActorFactory() {
        return this.actorFactory;
    }

    /**
     * Gets the information about the Actor's type.
     *
     * @return Information about the Actor's type.
     */
    ActorTypeInformation<T> getActorTypeInformation() {
        return this.actorTypeInformation;
    }

    /**
     * Gets the trace for Actor logs.
     *
     * @return Trace for Actor logs.
     */
    ActorTrace getActorTrace() {
        return this.actorTrace;
    }

    /**
     * Gets the client to communicate to Dapr's API.
     *
     * @return Client to communicate to Dapr's API.
     */
    DaprClient getDaprClient() {
        return this.daprClient;
    }

    /**
     * Gets the state provider for given Actor's type.
     *
     * @return State provider for given Actor's type.
     */
    DaprStateAsyncProvider getStateProvider() {
        return stateProvider;
    }
}
