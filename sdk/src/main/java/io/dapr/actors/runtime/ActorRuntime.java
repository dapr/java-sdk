/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;

import io.dapr.actors.*;

/**
 * Contains methods to register actor types. Registering the types allows the runtime to create instances of the actor.
 */
public class ActorRuntime {
    private static volatile ActorRuntime instance;
    private static AppToDaprAsyncClient appToDaprHttpAsyncClient;
    private final String TraceType = "ActorRuntime";
    private final HashMap<String, ActorManager> actorManagers;

    private ActorRuntime() throws IllegalStateException{
        if (instance != null) {
            throw new IllegalStateException("ActorRuntime should only be constructed once");
        }

        this.actorManagers = new HashMap<String, ActorManager>();
        appToDaprHttpAsyncClient = new DaprClientBuilder().buildAsyncClient();
    }

    /**
     * Returns an ActorRuntime object.
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

        return  instance;
    }

    /**
     *
     * @return Actor type names registered with the runtime.
     */
    public Set<String> getRegisteredActorTypes() {
        return this.actorManagers.keySet();
    }

    /**
     * Registers an actor with the runtime.
     * @param clazz The type of actor.
     */
    <TActor extends AbstractActor> void RegisterActor(Class<TActor> clazz) {
        RegisterActor(clazz, null);
    }

    /**
     * Registers an actor with the runtime.
     * @param clazz The type of actor.
     * @param actorServiceFactory An optional delegate to create actor service. This can be used for dependency injection into actors.
     */
    <TActor extends AbstractActor> void RegisterActor(Class<TActor> clazz, Function<ActorTypeInformation, ActorService> actorServiceFactory)
    {
        ActorTypeInformation actorTypeInfo = ActorTypeInformation.create(clazz);

        ActorService actorService;
        if (actorServiceFactory != null)
        {
            actorService = actorServiceFactory.apply(actorTypeInfo);
        }
        else
        {
            actorService = new ActorService(actorTypeInfo);
        }

        // Create ActorManagers, override existing entry if registered again.
        this.actorManagers.put(actorTypeInfo.getName(), new ActorManager(actorService));
    }

    /**
     * Activates an actor for an actor type with given actor id.
     * @param actorTypeName Actor type name to activate the actor for.
     * @param actorId Actor id for the actor to be activated.
     */
    static void Activate(String actorTypeName, String actorId)
    {
        // uncomment when ActorManager implemented
        // return instance.GetActorManager(actorTypeName).ActivateActor(new ActorId(actorId));
    }

    /**
     * Deactivates an actor for an actor type with given actor id.
     * @param actorTypeName Actor type name to deactivate the actor for.
     * @param actorId Actor id for the actor to be deactivated.
     */
    static void Deactivate(String actorTypeName, String actorId)
    {
        // uncomment when ActorManager implemented
        // return instance.GetActorManager(actorTypeName).DeactivateActor(new ActorId(actorId));
    }

    /**
     *  Invokes the specified method for the actor, this is mainly used for cross language invocation.
     * @param actorTypeName Actor type name to invoke the method for.
     * @param actorId Actor id for the actor for which method will be invoked.
     * @param actorMethodName Method name on actor type which will be invoked.
     * @param requestBodyStream Payload for the actor method.
     * @param responseBodyStream Response for the actor method.
     * @return
     */
    static void Dispatch(String actorTypeName, String actorId, String actorMethodName, byte[] requestBodyStream, byte[] responseBodyStream)
    {
        // uncomment when ActorManager implemented
        // return instance.GetActorManager(actorTypeName).Dispatch(new ActorId(actorId), actorMethodName, requestBodyStream, responseBodyStream);
    }

    /**
     * Fires a reminder for the Actor.
     * @param actorTypeName Actor type name to invoke the method for.
     * @param actorId Actor id for the actor for which method will be invoked.
     * @param reminderName The name of reminder provided during registration.
     * @param requestBodyStream Payload for the actor method
     */
    static void FireReminder(String actorTypeName, String actorId, String reminderName, byte[] requestBodyStream)
    {
        // uncomment when ActorManager implemented
        // return instance.GetActorManager(actorTypeName).FireReminder(new ActorId(actorId), reminderName, requestBodyStream);
    }

    /**
     * Fires a timer for the Actor.
     * @param actorTypeName Actor type name to invoke the method for.
     * @param actorId Actor id for the actor for which method will be invoked.
     * @param timerName The name of timer provided during registration.
     */
    static void FireTimer(String actorTypeName, String actorId, String timerName)
    {
        // uncomment when ActorManager implemented
        // return instance.GetActorManager(actorTypeName).FireTimerAsync(new ActorId(actorId), timerName);
    }

    private ActorManager GetActorManager(String actorTypeName) throws IllegalStateException
    {
        ActorManager actorManager = this.actorManagers.get(actorTypeName);

        if (actorManager == null)
        {
            String errorMsg = String.format("Actor type %s is not registered with Actor runtime.", actorTypeName);

            ActorTrace.WriteError(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        return actorManager;
    }
}