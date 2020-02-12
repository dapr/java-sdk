/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorTrace;
import io.dapr.client.DaprHttpBuilder;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
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
   * Serializer for internal Dapr objects.
   */
  private static final ActorObjectSerializer INTERNAL_SERIALIZER = new ActorObjectSerializer();

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
   * Map of ActorType --> ActorManager.
   */
  private final Map<String, ActorManager> actorManagers;

  /**
   * The default constructor. This should not be called directly.
   *
   * @throws IllegalStateException If cannot instantiate Runtime.
   */
  private ActorRuntime() throws IllegalStateException {
    this(new DaprHttpClient(new DaprHttpBuilder().build()));
  }

  /**
   * Constructor with dependency injection, useful for testing. This should not be called directly.
   *
   * @param daprClient Client to communicate with Dapr.
   * @throws IllegalStateException If class has one instance already.
   */
  private ActorRuntime(DaprClient daprClient) throws IllegalStateException {
    if (instance != null) {
      throw new IllegalStateException("ActorRuntime should only be constructed once");
    }

    this.config = new ActorRuntimeConfig();
    this.actorManagers = Collections.synchronizedMap(new HashMap<>());
    this.daprClient = daprClient;
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
   * @return Actor configuration.
   */
  public ActorRuntimeConfig getConfig() {
    return this.config;
  }

  /**
   * Gets the Actor configuration for this runtime.
   *
   * @return Actor configuration serialized.
   * @throws IOException If cannot serialize config.
   */
  public byte[] serializeConfig() throws IOException {
    return this.INTERNAL_SERIALIZER.serialize(this.config);
  }

  /**
   * Registers an actor with the runtime, using {@link DefaultObjectSerializer} and {@link DefaultActorFactory}.
   *
   * {@link DefaultObjectSerializer} is not recommended for production scenarios.
   *
   * @param clazz            The type of actor.
   * @param <T>              Actor class type.
   */
  public <T extends AbstractActor> void registerActor(Class<T> clazz) {
    registerActor(clazz, new DefaultObjectSerializer(), new DefaultObjectSerializer());
  }

  /**
   * Registers an actor with the runtime, using {@link DefaultObjectSerializer}.
   *
   * {@link DefaultObjectSerializer} is not recommended for production scenarios.
   *
   * @param clazz            The type of actor.
   * @param actorFactory     An optional factory to create actors. This can be used for dependency injection.
   * @param <T>              Actor class type.
   */
  public <T extends AbstractActor> void registerActor(Class<T> clazz, ActorFactory<T> actorFactory) {
    registerActor(clazz, actorFactory, new DefaultObjectSerializer(), new DefaultObjectSerializer());
  }

  /**
   * Registers an actor with the runtime.
   *
   * @param clazz            The type of actor.
   * @param objectSerializer Serializer for Actor's request and response objects.
   * @param stateSerializer  Serializer for Actor's state objects.
   * @param <T>              Actor class type.
   */
  public <T extends AbstractActor> void registerActor(
        Class<T> clazz, DaprObjectSerializer objectSerializer, DaprObjectSerializer stateSerializer) {
    registerActor(clazz,  new DefaultActorFactory<T>(), objectSerializer, stateSerializer);
  }

  /**
   * Registers an actor with the runtime.
   *
   * @param clazz            The type of actor.
   * @param actorFactory     An optional factory to create actors. This can be used for dependency injection.
   * @param objectSerializer Serializer for Actor's request and response objects.
   * @param stateSerializer  Serializer for Actor's state objects.
   * @param <T>              Actor class type.
   */
  public <T extends AbstractActor> void registerActor(
        Class<T> clazz, ActorFactory<T> actorFactory,
        DaprObjectSerializer objectSerializer,
        DaprObjectSerializer stateSerializer) {
    if (clazz == null) {
      throw new IllegalArgumentException("Class is required.");
    }
    if (actorFactory == null) {
      throw new IllegalArgumentException("Actor factory is required.");
    }
    if (objectSerializer == null) {
      throw new IllegalArgumentException("Object serializer is required.");
    }
    if (stateSerializer == null) {
      throw new IllegalArgumentException("State serializer is required.");
    }

    ActorTypeInformation<T> actorTypeInfo = ActorTypeInformation.create(clazz);

    ActorRuntimeContext<T> context = new ActorRuntimeContext<>(
          this,
          objectSerializer,
          actorFactory,
          actorTypeInfo,
          this.daprClient,
          new DaprStateAsyncProvider(this.daprClient, stateSerializer));

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
    return Mono.fromSupplier(() -> this.getActorManager(actorTypeName))
          .flatMap(m -> m.activateActor(new ActorId(actorId)));
  }

  /**
   * Deactivates an actor for an actor type with given actor id.
   *
   * @param actorTypeName Actor type name to deactivate the actor for.
   * @param actorId       Actor id for the actor to be deactivated.
   * @return Async void task.
   */
  public Mono<Void> deactivate(String actorTypeName, String actorId) {
    return Mono.fromSupplier(() -> this.getActorManager(actorTypeName))
          .flatMap(m -> m.deactivateActor(new ActorId(actorId)));
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
  public Mono<byte[]> invoke(String actorTypeName, String actorId, String actorMethodName, byte[] payload) {
    return Mono.fromSupplier(() -> this.getActorManager(actorTypeName))
          .flatMap(m -> m.invokeMethod(new ActorId(actorId), actorMethodName, payload));
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
  public Mono<Void> invokeReminder(String actorTypeName, String actorId, String reminderName, byte[] params) {
    return Mono.fromSupplier(() -> this.getActorManager(actorTypeName))
          .flatMap(m -> m.invokeReminder(new ActorId(actorId), reminderName, params));
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
    return Mono.fromSupplier(() -> this.getActorManager(actorTypeName))
          .flatMap(m -> m.invokeTimer(new ActorId(actorId), timerName));
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
}