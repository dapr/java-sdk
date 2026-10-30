/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorTrace;
import io.dapr.config.Properties;
import io.dapr.internal.grpc.interceptors.DaprApiTokenInterceptor;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.NetworkUtils;
import io.dapr.v1.DaprGrpc;
import io.grpc.ManagedChannel;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Contains methods to register actor types. Registering the types allows the
 * runtime to create instances of the actor.
 */
public class ActorRuntime implements Closeable {

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
   * Maximum time to wait for Dapr to acknowledge the actor registration on the actor event stream.
   */
  private static final Duration ACTOR_EVENTS_HANDSHAKE_TIMEOUT = Duration.ofSeconds(10);

  /**
   * Gets an instance to the ActorRuntime. There is only 1.
   */
  private static volatile ActorRuntime instance;

  /**
   * Channel for communication with Dapr.
   */
  private final ManagedChannel channel;

  /**
   * Configuration for the Actor runtime.
   */
  private final ActorRuntimeConfig config;

  /**
   * Decides whether to trust the activation id sent by Dapr.
   */
  private final ActivationIdGate activationIdGate;

  /**
   * A client used to communicate from the actor to the Dapr runtime.
   */
  private final DaprClient daprClient;

  /**
   * Map of ActorType --> ActorManager.
   */
  private final ConcurrentMap<String, ActorManager> actorManagers;

  /**
   * Stream that hosts the actors over gRPC, or null if not subscribed.
   */
  private volatile ActorEventSubscription actorEventSubscription;

  /**
   * Dapr API token sent on the actor event stream, or null.
   */
  private String apiToken;

  /**
   * The default constructor. This should not be called directly.
   *
   * @throws IllegalStateException If cannot instantiate Runtime.
   */
  private ActorRuntime() throws IllegalStateException {
    this(new Properties());
  }

  /**
   * The default constructor. This should not be called directly.
   *
   * @throws IllegalStateException If cannot instantiate Runtime.
   */
  private ActorRuntime(Properties properties) throws IllegalStateException {
    this(NetworkUtils.buildGrpcManagedChannel(properties));
    this.apiToken = properties.getValue(Properties.API_TOKEN);
  }

  /**
   * Constructor once channel is available. This should not be called directly.
   *
   * @param channel GRPC managed channel to be closed (or null).
   * @throws IllegalStateException If you cannot instantiate Runtime.
   */
  private ActorRuntime(ManagedChannel channel) throws IllegalStateException {
    this(channel, new DaprClientImpl(channel));
  }

  /**
   * Constructor with dependency injection, useful for testing. This should not be called directly.
   *
   * @param channel    GRPC managed channel to be closed (or null).
   * @param daprClient Client to communicate with Dapr.
   * @throws IllegalStateException If class has one instance already.
   */
  private ActorRuntime(ManagedChannel channel, DaprClient daprClient) throws IllegalStateException {
    if (instance != null) {
      throw new IllegalStateException("ActorRuntime should only be constructed once");
    }

    this.config = new ActorRuntimeConfig();
    this.actorManagers = new ConcurrentHashMap<>();
    this.daprClient = daprClient;
    this.channel = channel;
    this.activationIdGate = new ActivationIdGate(daprClient::getRuntimeVersion);
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
   * Returns an ActorRuntime object.
   *
   * @param properties Properties to be used for the runtime.
   * @return An ActorRuntime object.
   */
  public static ActorRuntime getInstance(Properties properties) {
    if (instance == null) {
      synchronized (ActorRuntime.class) {
        if (instance == null) {
          instance = new ActorRuntime(properties);
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
    return INTERNAL_SERIALIZER.serialize(this.config);
  }

  /**
   * Registers an actor with the runtime, using {@link DefaultObjectSerializer} and {@link DefaultActorFactory}.
   * {@link DefaultObjectSerializer} is not recommended for production scenarios.
   *
   * @param clazz The type of actor.
   * @param <T>   Actor class type.
   */
  public <T extends AbstractActor> void registerActor(Class<T> clazz) {
    registerActor(clazz, new DefaultObjectSerializer(), new DefaultObjectSerializer());
  }

  /**
   * Registers an actor with the runtime, using {@link DefaultObjectSerializer}.
   * {@link DefaultObjectSerializer} is not recommended for production scenarios.
   *
   * @param clazz        The type of actor.
   * @param actorFactory An optional factory to create actors. This can be used for dependency injection.
   * @param <T>          Actor class type.
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
    registerActor(clazz, new DefaultActorFactory<T>(), objectSerializer, stateSerializer);
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

    // Create ActorManager, if not yet registered.
    this.actorManagers.computeIfAbsent(actorTypeInfo.getName(), (k) -> {
      ActorRuntimeContext<T> context = new ActorRuntimeContext<>(
          this,
          objectSerializer,
          actorFactory,
          actorTypeInfo,
          this.daprClient,
          new DaprStateAsyncProvider(this.daprClient, stateSerializer));
      this.config.addRegisteredActorType(actorTypeInfo.getName());
      ActorEventSubscription subscription = this.actorEventSubscription;
      if (subscription != null && subscription.isRunning()) {
        ACTOR_TRACE.writeWarning(TRACE_TYPE, actorTypeInfo.getName(),
            "Actor type registered after subscribing to actor events; Dapr learns about it on the next reconnect.");
      }
      return new ActorManager<T>(context);
    });
  }

  /**
   * Hosts the registered actor types over gRPC. Dapr delivers actor invocations, reminders, timers and
   * deactivations on a stream opened by the app, so the app does not need to expose a server port for actor
   * callbacks. Register the actor types before calling this method.
   *
   * <p>Dapr rejects the stream when it is configured with an HTTP app channel: run Dapr without an app port, or
   * with the gRPC app protocol.
   *
   * <p>This API is in alpha and may change.
   *
   * @return The running subscription. Close it to stop hosting the actors.
   * @throws IllegalStateException If the runtime has no gRPC channel or is already subscribed.
   */
  public ActorEventSubscription subscribeActorEvents() {
    if (this.channel == null) {
      throw new IllegalStateException("ActorRuntime has no gRPC channel to Dapr.");
    }
    DaprGrpc.DaprStub stub = DaprGrpc.newStub(this.channel)
        .withInterceptors(new DaprApiTokenInterceptor(this.apiToken));
    return subscribeActorEvents(stub, ACTOR_EVENTS_HANDSHAKE_TIMEOUT);
  }

  /**
   * Hosts the registered actor types over gRPC using the given stub. Useful for testing.
   *
   * @param stub             Dapr gRPC stub.
   * @param handshakeTimeout Maximum time to wait for Dapr to acknowledge the registration.
   * @return The running subscription.
   */
  synchronized ActorEventSubscription subscribeActorEvents(DaprGrpc.DaprStub stub, Duration handshakeTimeout) {
    if (this.actorEventSubscription != null && this.actorEventSubscription.isRunning()) {
      throw new IllegalStateException("ActorRuntime is already subscribed to actor events.");
    }
    ActorEventSubscription subscription = new ActorEventSubscription(this, stub, this.channel, handshakeTimeout);
    this.actorEventSubscription = subscription;
    subscription.start();
    return subscription;
  }

  /**
   * Gets the serializer for the request and response objects of an actor type.
   *
   * @param actorTypeName Actor type name.
   * @return Object serializer.
   * @throws IllegalArgumentException If the actor type is not registered.
   */
  DaprObjectSerializer getObjectSerializer(String actorTypeName) {
    return this.getActorManager(actorTypeName).getObjectSerializer();
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
    return invoke(actorTypeName, actorId, actorMethodName, payload, null);
  }

  /**
   * Invokes the method on the activation Dapr sent.
   *
   * @param actorTypeName   Actor type name.
   * @param actorId         Actor id.
   * @param actorMethodName Method name.
   * @param payload         RAW payload.
   * @param activationId    Activation id sent by Dapr, or null if absent.
   * @return Response for the actor method.
   */
  public Mono<byte[]> invoke(
      String actorTypeName, String actorId, String actorMethodName, byte[] payload, String activationId) {
    ActorId id = new ActorId(actorId);
    return Mono.fromSupplier(() -> this.getActorManager(actorTypeName))
        .flatMap(m -> this.activationIdGate.filter(activationId)
            .flatMap(a -> m.activateActor(id, a.orElse(null)))
            .flatMap(a -> m.invokeMethod((ActorManager.ActiveActor) a, actorMethodName, payload)));
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
    return invokeReminder(actorTypeName, actorId, reminderName, params, null);
  }

  /**
   * Fires a reminder on the activation Dapr sent.
   *
   * @param actorTypeName Actor type name.
   * @param actorId       Actor id.
   * @param reminderName  Reminder name.
   * @param params        Reminder params.
   * @param activationId  Activation id sent by Dapr, or null if absent.
   * @return Async void task.
   */
  public Mono<Void> invokeReminder(
      String actorTypeName, String actorId, String reminderName, byte[] params, String activationId) {
    ActorId id = new ActorId(actorId);
    return Mono.fromSupplier(() -> this.getActorManager(actorTypeName))
        .flatMap(m -> this.activationIdGate.filter(activationId)
            .flatMap(a -> m.activateActor(id, a.orElse(null)))
            .flatMap(a -> m.invokeReminder((ActorManager.ActiveActor) a, reminderName, params)));
  }

  /**
   * Fires a timer for the Actor.
   *
   * @param actorTypeName Actor type name to invoke the method for.
   * @param actorId       Actor id for the actor for which method will be invoked.
   * @param timerName     The name of timer provided during registration.
   * @param params        Params to trigger timer.
   * @return Async void task.
   */
  public Mono<Void> invokeTimer(String actorTypeName, String actorId, String timerName, byte[] params) {
    return invokeTimer(actorTypeName, actorId, timerName, params, null);
  }

  /**
   * Fires a timer on the activation Dapr sent.
   *
   * @param actorTypeName Actor type name.
   * @param actorId       Actor id.
   * @param timerName     Timer name.
   * @param params        Timer params.
   * @param activationId  Activation id sent by Dapr, or null if absent.
   * @return Async void task.
   */
  public Mono<Void> invokeTimer(
      String actorTypeName, String actorId, String timerName, byte[] params, String activationId) {
    ActorId id = new ActorId(actorId);
    return Mono.fromSupplier(() -> this.getActorManager(actorTypeName))
        .flatMap(m -> this.activationIdGate.filter(activationId)
            .flatMap(a -> m.activateActor(id, a.orElse(null)))
            .flatMap(a -> m.invokeTimer((ActorManager.ActiveActor) a, timerName, params)));
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
      throw new IllegalArgumentException(errorMsg);
    }

    return actorManager;
  }

  /**
   * Build an instance of the Client based on the provided setup.
   *
   * @param channel GRPC managed channel.
   * @return an instance of the setup Client
   * @throws java.lang.IllegalStateException if any required field is missing
   */
  private static DaprClient buildDaprClient(ManagedChannel channel) {
    return new DaprClientImpl(channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    ActorEventSubscription subscription = this.actorEventSubscription;
    if (subscription != null) {
      subscription.close();
    }
    if (channel != null && !channel.isShutdown()) {
      channel.shutdown();
    }
  }
}
