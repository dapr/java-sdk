/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import io.dapr.actors.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * Contains methods to register actor types. Registering the types allows the
 * runtime to create instances of the actor.
 */
public class ActorRuntime {

  /**
   * Gets an instance to the ActorRuntime. There is only 1.
   */
  private static volatile ActorRuntime instance;

  /**
   * A client used to communicate from the actor to the Dapr runtime.
   */
  private static AppToDaprAsyncClient appToDaprAsyncClient;

  /**
   * A trace type used when logging.
   */
  private static final String TraceType = "ActorRuntime";

  /**
   * Map of ActorType --> ActorManager.
   */
  private final HashMap<String, ActorManager> actorManagers;

  /**
   * The default constructor. This should not be called directly.
   *
   * @throws IllegalStateException
   */
  private ActorRuntime() throws IllegalStateException {
    if (instance != null) {
      throw new IllegalStateException("ActorRuntime should only be constructed once");
    }

    this.actorManagers = new HashMap<String, ActorManager>();
    appToDaprAsyncClient = new AppToDaprClientBuilder().buildAsyncClient();
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
   *
   * @return Actor type names registered with the runtime.
   */
  public Collection<String> getRegisteredActorTypes() {
    return Collections.unmodifiableCollection(this.actorManagers.keySet());
  }

  /**
   * Registers an actor with the runtime.
   *
   * @param clazz The type of actor.
   * @param <T> Actor class type.
   */
  public <T extends AbstractActor> void RegisterActor(Class<T> clazz) {
    RegisterActor(clazz, null);
  }

  /**
   * Registers an actor with the runtime.
   *
   * @param clazz The type of actor.
   * @param actorFactory An optional factory to create actors.
   * @param <T> Actor class type.
   * This can be used for dependency injection into actors.
   */
  public <T extends AbstractActor> void RegisterActor(Class<T> clazz, ActorFactory actorFactory) {
    ActorTypeInformation actorTypeInfo = ActorTypeInformation.create(clazz);

    ActorFactory actualActorFactory = actorFactory != null ? actorFactory : new DefaultActorFactory<T>(actorTypeInfo);
      // TODO: Refactor into a Builder class.
      DaprStateAsyncProvider stateProvider = new DaprStateAsyncProvider(this.appToDaprAsyncClient, new ActorStateSerializer());
      ActorService actorService = new ActorServiceImpl(actorTypeInfo, stateProvider, actualActorFactory);

    // Create ActorManagers, override existing entry if registered again.
    synchronized (this.actorManagers) {
      this.actorManagers.put(actorTypeInfo.getName(), new ActorManager(actorService));
    }
  }

  /**
   * Activates an actor for an actor type with given actor id.
   *
   * @param actorTypeName Actor type name to activate the actor for.
   * @param actorId Actor id for the actor to be activated.
   */
  static void Activate(String actorTypeName, String actorId) {
    // uncomment when ActorManager implemented
    // return instance.GetActorManager(actorTypeName).ActivateActor(new ActorId(actorId));
  }

  /**
   * Deactivates an actor for an actor type with given actor id.
   *
   * @param actorTypeName Actor type name to deactivate the actor for.
   * @param actorId Actor id for the actor to be deactivated.
   */
  static void Deactivate(String actorTypeName, String actorId) {
    // uncomment when ActorManager implemented
    // return instance.GetActorManager(actorTypeName).DeactivateActor(new ActorId(actorId));
  }

  /**
   * Invokes the specified method for the actor, this is mainly used for cross
   * language invocation.
   *
   * @param actorTypeName Actor type name to invoke the method for.
   * @param actorId Actor id for the actor for which method will be invoked.
   * @param actorMethodName Method name on actor type which will be invoked.
   * @param requestBodyStream Payload for the actor method.
   * @param responseBodyStream Response for the actor method.
   * @return
   */
  static void Dispatch(String actorTypeName, String actorId, String actorMethodName, byte[] requestBodyStream, byte[] responseBodyStream) {
    // uncomment when ActorManager implemented
    // return instance.GetActorManager(actorTypeName).Dispatch(new ActorId(actorId), actorMethodName, requestBodyStream, responseBodyStream);
  }

  /**
   * Fires a reminder for the Actor.
   *
   * @param actorTypeName Actor type name to invoke the method for.
   * @param actorId Actor id for the actor for which method will be invoked.
   * @param reminderName The name of reminder provided during registration.
   * @param requestBodyStream Payload for the actor method
   */
  static void FireReminder(String actorTypeName, String actorId, String reminderName, byte[] requestBodyStream) {
    // uncomment when ActorManager implemented
    // return instance.GetActorManager(actorTypeName).FireReminder(new ActorId(actorId), reminderName, requestBodyStream);
  }

  /**
   * Fires a timer for the Actor.
   *
   * @param actorTypeName Actor type name to invoke the method for.
   * @param actorId Actor id for the actor for which method will be invoked.
   * @param timerName The name of timer provided during registration.
   */
  static void FireTimer(String actorTypeName, String actorId, String timerName) {
    // uncomment when ActorManager implemented
    // return instance.GetActorManager(actorTypeName).FireTimerAsync(new ActorId(actorId), timerName);
  }

  private ActorManager GetActorManager(String actorTypeName) throws IllegalStateException {
    ActorManager actorManager = this.actorManagers.get(actorTypeName);

    if (actorManager == null) {
      String errorMsg = String.format("Actor type %s is not registered with Actor runtime.", actorTypeName);

      ActorTrace.WriteError(errorMsg);
      throw new IllegalStateException(errorMsg);
    }

    return actorManager;
  }
}