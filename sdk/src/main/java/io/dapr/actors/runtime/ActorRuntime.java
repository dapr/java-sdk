/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorTrace;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains methods to register actor types. Registering the types allows the
 * runtime to create instances of the actor.
 */
public class ActorRuntime {

    /**
     * A trace type used when logging.
     */
    private static final String TRACE_TYPE = "ActorRuntime";

    /**
     * Tracing errors, warnings and info logs.
     */
    private static final ActorTrace ACTOR_TRACE = new ActorTrace();

    /**
     * Gets an instance to the ActorRuntime. There is only 1.
     */
    private static volatile ActorRuntime instance;

    /**
     * A client used to communicate from the actor to the Dapr runtime.
     */
    private final DaprClient daprClient;

    /**
     * State provider for Dapr.
     */
    private final DaprStateAsyncProvider daprStateProvider;

    /**
     * Serializes/deserializes objects for Actors.
     */
    private final ActorStateSerializer actorSerializer;

    /**
     * Map of ActorType --> ActorManager.
     */
    private final Map<String, ActorManager> actorManagers;

    /**
     * The default constructor. This should not be called directly.
     *
     * @throws IllegalStateException
     */
    private ActorRuntime() throws IllegalStateException {
        if (instance != null) {
            throw new IllegalStateException("ActorRuntime should only be constructed once");
        }

        this.actorManagers = Collections.synchronizedMap(new HashMap<>());
        this.daprClient = new DaprClientBuilder().build();
        this.actorSerializer = new ActorStateSerializer();
        this.daprStateProvider = new DaprStateAsyncProvider(this.daprClient, this.actorSerializer);
    }

    /**
     * Returns an ActorRuntime object.
     *
     * @return An ActorRuntime object.
     */
    public static ActorRuntime getInstance() {
        if (instance == null) {
            synchronized (ActorRuntime.class) {
                if (instance == null) {
                    instance = new ActorRuntime();
                }
            }
        }

        return instance;
    }

    /**
     * Gets the Actor type names registered with the runtime.
     *
     * @return Actor type names.
     */
    public Collection<String> getRegisteredActorTypes() {
        return Collections.unmodifiableCollection(this.actorManagers.keySet());
    }

    /**
     * Registers an actor with the runtime.
     *
     * @param clazz The type of actor.
     * @param <T>   Actor class type.
     * @return Async void task.
     */
    public <T extends AbstractActor> Mono<Void> registerActor(Class<T> clazz) {
        return registerActor(clazz, null);
    }

    /**
     * Registers an actor with the runtime.
     *
     * @param clazz        The type of actor.
     * @param actorFactory An optional factory to create actors.
     * @param <T>          Actor class type.
     * @return Async void task.
     * This can be used for dependency injection into actors.
     */
    public <T extends AbstractActor> Mono<Void> registerActor(Class<T> clazz, ActorFactory<T> actorFactory) {
        ActorTypeInformation<T> actorTypeInfo = ActorTypeInformation.create(clazz);

        ActorFactory<T> actualActorFactory = actorFactory != null ? actorFactory : new DefaultActorFactory<T>();

        ActorRuntimeContext<T> context = new ActorRuntimeContext<T>(
                this,
                this.actorSerializer,
                actualActorFactory,
                actorTypeInfo,
                this.daprClient,
                new DaprStateAsyncProvider(this.daprClient, this.actorSerializer));

        // Create ActorManagers, override existing entry if registered again.
        this.actorManagers.put(actorTypeInfo.getName(), new ActorManager<T>(context));
        return Mono.empty();
    }

    /**
     * Activates an actor for an actor type with given actor id.
     *
     * @param actorTypeName Actor type name to activate the actor for.
     * @param actorId       Actor id for the actor to be activated.
     * @return Async void task.
     */
    public Mono<Void> activate(String actorTypeName, String actorId) {
        return this.getActorManager(actorTypeName).flatMap(m -> m.activateActor(new ActorId(actorId)));
    }

    /**
     * Deactivates an actor for an actor type with given actor id.
     *
     * @param actorTypeName Actor type name to deactivate the actor for.
     * @param actorId       Actor id for the actor to be deactivated.
     * @return Async void task.
     */
    public Mono<Void> deactivate(String actorTypeName, String actorId) {
        return this.getActorManager(actorTypeName).flatMap(m -> m.deactivateActor(new ActorId(actorId)));
    }

    /**
     * Invokes the specified method for the actor, this is mainly used for cross
     * language invocation.
     *
     * @param actorTypeName   Actor type name to invoke the method for.
     * @param actorId         Actor id for the actor for which method will be invoked.
     * @param actorMethodName Method name on actor type which will be invoked.
     * @param request         Payload for the actor method.
     * @return Response for the actor method.
     */
    public Mono<String> invoke(String actorTypeName, String actorId, String actorMethodName, String request) {
        return this.getActorManager(actorTypeName).flatMap(m -> m.invokeMethod(new ActorId(actorId), actorMethodName, request));
    }

    /**
     * Fires a reminder for the Actor.
     *
     * @param actorTypeName Actor type name to invoke the method for.
     * @param actorId       Actor id for the actor for which method will be invoked.
     * @param reminderName  The name of reminder provided during registration.
     * @param params        Params for the reminder.
     * @return Async void task.
     */
    public Mono<Void> invokeReminder(String actorTypeName, String actorId, String reminderName, String params) {
        return this.getActorManager(actorTypeName).flatMap(m -> m.invokeReminder(new ActorId(actorId), reminderName, params));
    }

    /**
     * Fires a timer for the Actor.
     *
     * @param actorTypeName Actor type name to invoke the method for.
     * @param actorId       Actor id for the actor for which method will be invoked.
     * @param timerName     The name of timer provided during registration.
     * @return Async void task.
     */
    public Mono<Void> invokeTimer(String actorTypeName, String actorId, String timerName) {
        return this.getActorManager(actorTypeName).flatMap(m -> m.invokeTimer(new ActorId(actorId), timerName));
    }

    /**
     * Finds the actor manager or errors out.
     *
     * @param actorTypeName Actor type for the actor manager to be found.
     * @return Actor manager or error if not found.
     */
    private Mono<ActorManager> getActorManager(String actorTypeName) {
        ActorManager actorManager = this.actorManagers.get(actorTypeName);

        try {
            if (actorManager == null) {
                String errorMsg = String.format("Actor type %s is not registered with Actor runtime.", actorTypeName);

                ACTOR_TRACE.writeError(TRACE_TYPE, actorTypeName, "Actor type is not registered with runtime.");

                throw new IllegalStateException(errorMsg);
            }
        } catch (IllegalStateException e) {
            return Mono.error(e);
        }

        return Mono.just(actorManager);
    }
}