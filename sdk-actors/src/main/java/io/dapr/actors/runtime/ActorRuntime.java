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

import java.io.IOException;
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
   * Configuration for the Actor runtime.
   */
  private final ActorRuntimeConfig config;

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
    this(new DaprClientBuilder().build());
  }

  /**
   * Constructor with dependency injection, useful for testing. This should not be called directly.
   *
   * @param daprClient Client to communicate with Dapr.
   * @throws IllegalStateException
   */
  private ActorRuntime(DaprClient daprClient) throws IllegalStateException {
    if (instance != null) {
      throw new IllegalStateException("ActorRuntime should only be constructed once");
    }

    this.config = new ActorRuntimeConfig();
    this.actorManagers = Collections.synchronizedMap(new HashMap<>());
    this.daprClient = daprClient;
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
   * Gets the Actor configuration for this runtime.
   *
   * @return Actor configuration serialized in a String.
   * @throws IOException If cannot serialize config.
   */
  public String serializeConfig() throws IOException {
    return this.actorSerializer.serializeString(this.config);
  }

  /**
   * Registers an actor with the runtime.
   *
   * @param clazz The type of actor.
   * @param <T>   Actor class type.
   */
  public <T extends AbstractActor> void registerActor(Class<T> clazz) {
    registerActor(clazz, null);
  }

  /**
   * Registers an actor with the runtime.
   *
   * @param clazz        The type of actor.
   * @param actorFactory An optional factory to create actors.
   * @param <T>          Actor class type.
   *                     This can be used for dependency injection into actors.
   */
  public <T extends AbstractActor> void registerActor(Class<T> clazz, ActorFactory<T> actorFactory) {
    ActorTypeInformation<T> actorTypeInfo = ActorTypeInformation.create(clazz);

    ActorFactory<T> actualActorFactory = actorFactory != null ? actorFactory : new DefaultActorFactory<T>();

    ActorRuntimeContext<T> context = new ActorRuntimeContext<T>(
      this,
      this.actorSerializer,
      actualActorFactory,
      actorTypeInfo,
      this.daprClient,
      this.daprStateProvider);

    // Create ActorManagers, override existing entry if registered again.
    this.actorManagers.put(actorTypeInfo.getName(), new ActorManager<T>(context));
    this.config.addRegisteredActorType(actorTypeInfo.getName());
  }

  /**
   * Activates an actor for an actor type with given actor id.
   *
   * @param actorTypeName Actor type name to activate the actor for.
   * @param actorId       Actor id for the actor to be activated.
   * @return Async void task.
   */
  public Mono<Void> activate(String actorTypeName, String actorId) {
    return Mono.defer(() -> this.getActorManager(actorTypeName).activateActor(new ActorId(actorId)));
  }

  /**
   * Deactivates an actor for an actor type with given actor id.
   *
   * @param actorTypeName Actor type name to deactivate the actor for.
   * @param actorId       Actor id for the actor to be deactivated.
   * @return Async void task.
   */
  public Mono<Void> deactivate(String actorTypeName, String actorId) {
    return Mono.defer(() -> this.getActorManager(actorTypeName).deactivateActor(new ActorId(actorId)));
  }

  /**
   * Invokes the specified method for the actor, this is mainly used for cross
   * language invocation.
   *
   * @param actorTypeName   Actor type name to invoke the method for.
   * @param actorId         Actor id for the actor for which method will be invoked.
   * @param actorMethodName Method name on actor type which will be invoked.
   * @param payload         RAW payload for the actor method.
   * @return Response for the actor method.
   */
  public Mono<String> invoke(String actorTypeName, String actorId, String actorMethodName, String payload) {
    return Mono.defer(() ->
      this.getActorManager(actorTypeName).invokeMethod(new ActorId(actorId), actorMethodName, unwrap(payload)))
      .map(response -> wrap(response.toString()));
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
    return Mono.defer(() ->
      this.getActorManager(actorTypeName).invokeReminder(new ActorId(actorId), reminderName, params));
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
    return Mono.defer(() -> this.getActorManager(actorTypeName).invokeTimer(new ActorId(actorId), timerName));
  }

  /**
   * Finds the actor manager or errors out.
   *
   * @param actorTypeName Actor type for the actor manager to be found.
   * @return Actor manager instance, never null.
   * @throws IllegalStateException if cannot find actor's manager.
   */
  private ActorManager getActorManager(String actorTypeName) {
    ActorManager actorManager = this.actorManagers.get(actorTypeName);

    if (actorManager == null) {
      String errorMsg = String.format("Actor type %s is not registered with Actor runtime.", actorTypeName);
      ACTOR_TRACE.writeError(TRACE_TYPE, actorTypeName, "Actor type is not registered with runtime.");
      throw new IllegalStateException(errorMsg);
    }

    return actorManager;
  }

  /**
   * Extracts the data as String from the Actor's method result.
   *
   * @param payload String returned by API.
   * @return String or null.
   * @throws RuntimeException In case it cannot generate String.
   */
  private String unwrap(final String payload) {
    try {
      byte[] data = this.actorSerializer.unwrapData(payload);
      if (data == null) {
        return null;
      }

      return new String(data);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Builds the request to invoke an API for Actors.
   *
   * @param payload String to be wrapped in the request.
   * @return String to be sent to Dapr's API.
   * @throws RuntimeException In case it cannot generate String.
   */
  private String wrap(final String payload) {
    try {
      byte[] data = null;
      if (payload != null) {
        data = payload.getBytes();
      }
      return this.actorSerializer.wrapData(data);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}