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
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Manages actors of a specific type.
 */
class ActorManager<T extends AbstractActor> {

  /**
   * Serializer for internal Dapr objects.
   */
  private static final ActorObjectSerializer OBJECT_SERIALIZER = new ActorObjectSerializer();

  /**
   * Context for the Actor runtime.
   */
  private final ActorRuntimeContext<T> runtimeContext;

  /**
   * Methods found in Actors.
   */
  private final ActorMethodInfoMap actorMethods;

  /**
   * Active Actor instances.
   */
  private final Map<ActorId, ActiveActor<T>> activeActors;

  /**
   * Instantiates a new manager for a given actor referenced in the runtimeContext.
   *
   * @param runtimeContext Runtime context for the Actor.
   */
  ActorManager(ActorRuntimeContext runtimeContext) {
    this.runtimeContext = runtimeContext;
    this.actorMethods = new ActorMethodInfoMap(runtimeContext.getActorTypeInformation().getInterfaces());
    this.activeActors = Collections.synchronizedMap(new HashMap<>());
  }

  /**
   * Activates an Actor.
   *
   * @param actorId Actor identifier.
   * @return Asynchronous void response.
   */
  Mono<Void> activateActor(ActorId actorId) {
    return activateActor(actorId, null).then();
  }

  /** Replaces the live instance if Dapr reports a different activation. */
  Mono<ActiveActor<T>> activateActor(ActorId actorId, String activationId) {
    return Mono.defer(() -> {
      ActiveActor<T> current = this.activeActors.get(actorId);
      if (current == null) {
        return this.createActor(actorId, activationId);
      }

      if (activationId == null || activationId.isEmpty() || activationId.equals(current.activationId)) {
        return Mono.just(current);
      }

      Mono<Void> deactivate = this.activeActors.remove(actorId, current)
          ? this.deactivateDiscarded(actorId, current.actor)
          : Mono.empty();
      return deactivate.then(Mono.defer(() -> this.createActor(actorId, activationId)));
    });
  }

  /**
   * Deactivates an Actor.
   *
   * @param actorId Actor identifier.
   * @return Asynchronous void response.
   */
  Mono<Void> deactivateActor(ActorId actorId) {
    return Mono.fromSupplier(() -> this.activeActors.remove(actorId))
        .flatMap(active -> active.actor.onDeactivateInternal());
  }

  /**
   * Invokes reminder for Actor.
   *
   * @param actorId      Identifier for Actor being invoked.
   * @param reminderName Name of reminder being invoked.
   * @param params       Parameters for the reminder.
   * @return Asynchronous void response.
   */
  Mono<Void> invokeReminder(ActorId actorId, String reminderName, byte[] params) {
    return this.invokeReminder(this.lookup(actorId), reminderName, params);
  }

  Mono<Void> invokeReminder(ActiveActor<T> active, String reminderName, byte[] params) {
    return this.invokeReminder(Mono.just(active), reminderName, params);
  }

  private Mono<Void> invokeReminder(Mono<ActiveActor<T>> active, String reminderName, byte[] params) {
    return Mono.fromSupplier(() -> {
      if (!this.runtimeContext.getActorTypeInformation().isRemindable()) {
        return null;
      }

      try {
        return OBJECT_SERIALIZER.deserialize(params, ActorReminderParams.class);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).flatMap(p -> active.flatMap(a ->
        invoke(a,
            ActorMethodContext.createForReminder(reminderName),
            actor -> doReminderInvokation((Remindable) actor, reminderName, p)))).then();
  }

  /**
   * Invokes a timer for a given Actor.
   *
   * @param actorId   Identifier for Actor.
   * @param timerName Name of timer being invoked.
   * @param params    Parameters for the timer.
   * @return Asynchronous void response.
   */
  Mono<Void> invokeTimer(ActorId actorId, String timerName, byte[] params) {
    return this.invokeTimer(this.lookup(actorId), timerName, params);
  }

  Mono<Void> invokeTimer(ActiveActor<T> active, String timerName, byte[] params) {
    return this.invokeTimer(Mono.just(active), timerName, params);
  }

  private Mono<Void> invokeTimer(Mono<ActiveActor<T>> active, String timerName, byte[] params) {
    return Mono.fromSupplier(() -> {
      try {
        return OBJECT_SERIALIZER.deserialize(params, ActorTimerParams.class);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).flatMap(p -> active.flatMap(a ->
            invokeMethod(
                    a,
                    ActorMethodContext.createForTimer(timerName),
                    p.getCallback(),
                    p.getData()))).then();
  }

  private Mono<ActiveActor<T>> createActor(ActorId actorId, String activationId) {
    return Mono.fromSupplier(() -> this.runtimeContext.getActorFactory().createActor(runtimeContext, actorId))
        .flatMap(actor -> actor.onActivateInternal(() -> !this.activeActors.containsKey(actorId))
            .then(this.onActivatedActor(actorId, actor, activationId)));
  }

  /** Discards the new instance if a concurrent activation registered first. */
  private Mono<ActiveActor<T>> onActivatedActor(ActorId actorId, T actor, String activationId) {
    return Mono.defer(() -> {
      ActiveActor<T> active = new ActiveActor<>(actor, activationId);
      ActiveActor<T> registered = this.activeActors.putIfAbsent(actorId, active);
      if (registered == null) {
        return Mono.just(active);
      }

      Mono<Void> discard = this.deactivateDiscarded(actorId, actor);
      if (activationId == null || activationId.isEmpty() || activationId.equals(registered.activationId)) {
        return discard.thenReturn(registered);
      }

      return discard.then(Mono.error(new IllegalStateException(
          String.format("Actor %s of type %s was activated by activation %s during this activation.",
              actorId.toString(),
              this.runtimeContext.getActorTypeInformation().getName(),
              registered.activationId))));
    });
  }

  /** Logs errors so a failed cleanup doesn't fail the call. */
  private Mono<Void> deactivateDiscarded(ActorId actorId, T actor) {
    return Mono.defer(actor::onDeactivateInternal)
        .onErrorResume(e -> {
          this.runtimeContext.getActorTrace().writeError(
              this.getClass().getSimpleName(),
              actorId.toString(),
              "Deactivating discarded actor of type %s failed: %s",
              this.runtimeContext.getActorTypeInformation().getName(),
              e.toString());
          return Mono.empty();
        });
  }

  /**
   * Internal method to actually invoke a reminder.
   *
   * @param actor          Actor that owns the reminder.
   * @param reminderName   Name of the reminder.
   * @param reminderParams Params for the reminder.
   * @return Asynchronous void response.
   */
  private Mono<Boolean> doReminderInvokation(
      Remindable actor,
      String reminderName,
      ActorReminderParams reminderParams) {
    return Mono.fromSupplier(() -> {
      if (actor == null) {
        throw new IllegalArgumentException("actor is mandatory.");
      }
      if (reminderName == null) {
        throw new IllegalArgumentException("reminderName is mandatory.");
      }
      if (reminderParams == null) {
        throw new IllegalArgumentException("reminderParams is mandatory.");
      }

      return true;
    }).flatMap(x -> {
      try {
        Object data = this.runtimeContext.getObjectSerializer().deserialize(
            reminderParams.getData(),
            actor.getStateType());
        return actor.receiveReminder(
            reminderName,
            data,
            reminderParams.getDueTime(),
            reminderParams.getPeriod());
      } catch (Exception e) {
        return Mono.error(e);
      }
    }).thenReturn(true);
  }

  /**
   * Invokes a given method in the Actor.
   *
   * @param actorId    Identifier for Actor being invoked.
   * @param methodName Name of method being invoked.
   * @param request    Input object for the method being invoked.
   * @return Asynchronous void response.
   */
  Mono<byte[]> invokeMethod(ActorId actorId, String methodName, byte[] request) {
    return this.lookup(actorId).flatMap(active -> this.invokeMethod(active, methodName, request));
  }

  Mono<byte[]> invokeMethod(ActiveActor<T> active, String methodName, byte[] request) {
    return invokeMethod(active, null, methodName, request);
  }

  /**
   * Internal method to actually invoke Actor's method.
   *
   * @param active     Actor instance to invoke.
   * @param context    Method context to be invoked.
   * @param methodName Method name to be invoked.
   * @param request    Input object to be passed in to the invoked method.
   * @return Asynchronous serialized response.
   */
  private Mono<byte[]> invokeMethod(
      ActiveActor<T> active, ActorMethodContext context, String methodName, byte[] request) {
    ActorMethodContext actorMethodContext = context;
    if (actorMethodContext == null) {
      actorMethodContext = ActorMethodContext.createForActor(methodName);
    }

    return this.invoke(active, actorMethodContext, actor -> {
      try {
        // Finds the actor method with the given name and 1 or no parameter.
        Method method = this.actorMethods.get(methodName);

        Object input = null;
        if (method.getParameterCount() == 1) {
          // Actor methods must have a one or no parameter, which is guaranteed at this point.
          Class<?> inputClass = method.getParameterTypes()[0];
          input = this.runtimeContext.getObjectSerializer().deserialize(request, TypeRef.get(inputClass));
        }

        if (method.getReturnType().equals(Mono.class)) {
          return invokeMonoMethod(actor, method, input);
        }

        return invokeMethod(actor, method, input);
      } catch (Exception e) {
        return Mono.error(e);
      }
    }).map(r -> {
      try {
        return this.runtimeContext.getObjectSerializer().serialize(r);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Invokes a method that returns a plain object (not Mono).
   *
   * @param actor  Actor to be invoked.
   * @param method Method to be invoked.
   * @param input  Input object for the method (or null).
   * @return Asynchronous object response.
   */
  private Mono<Object> invokeMethod(AbstractActor actor, Method method, Object input) {
    return Mono.fromSupplier(() -> {
      try {
        if (method.getParameterCount() == 0) {
          return method.invoke(actor);
        } else {
          // Actor methods must have a one or no parameter, which is guaranteed at this point.
          return method.invoke(actor, input);
        }
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Invokes a method that returns Mono.
   *
   * @param actor  Actor to be invoked.
   * @param method Method to be invoked.
   * @param input  Input object for the method (or null).
   * @return Asynchronous object response.
   */
  private Mono<Object> invokeMonoMethod(AbstractActor actor, Method method, Object input) {
    try {
      if (method.getParameterCount() == 0) {
        return (Mono<Object>) method.invoke(actor);
      } else {
        // Actor methods must have a one or no parameter, which is guaranteed at this point.
        return (Mono<Object>) method.invoke(actor, input);
      }
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  /**
   * Internal call to invoke a method, timer or reminder for an Actor.
   *
   * @param active  Actor instance to invoke.
   * @param context Context for the method/timer/reminder call.
   * @param func    Function to perform the method call.
   * @param <R>     Expected return type for the function call.
   * @return Asynchronous response for the returned object.
   */
  private <R> Mono<R> invoke(ActiveActor<T> active, ActorMethodContext context, Function<AbstractActor, Mono<R>> func) {
    try {
      AbstractActor actor = active.actor;
      return Mono.fromRunnable(() -> actor.rollback(true))
          .onErrorMap(throwable -> {
            actor.rollback(false);
            return throwable;
          })
          .then(actor.onPreActorMethodInternal(context))
          .then((Mono<Object>) func.apply(actor))
          .switchIfEmpty(actor.onPostActorMethodInternal(context, this.checkLive(active)))
          .flatMap(r -> actor.onPostActorMethodInternal(context, this.checkLive(active)).thenReturn(r))
          .map(o -> (R) o);
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  private Mono<ActiveActor<T>> lookup(ActorId actorId) {
    return Mono.defer(() -> {
      ActiveActor<T> active = this.activeActors.get(actorId);
      if (active == null) {
        return Mono.error(new IllegalArgumentException(
            String.format("Could not find actor %s of type %s.",
                actorId.toString(),
                this.runtimeContext.getActorTypeInformation().getName())));
      }

      return Mono.just(active);
    });
  }

  /** Fails the call before its state save if the instance was replaced during the call. */
  private Mono<Void> checkLive(ActiveActor<T> active) {
    return Mono.defer(() -> {
      ActorId actorId = active.actor.getId();
      if (this.activeActors.get(actorId) != active) {
        return Mono.error(new IllegalStateException(
            String.format("Actor %s of type %s was replaced during the call, state not saved.",
                actorId.toString(),
                this.runtimeContext.getActorTypeInformation().getName())));
      }

      return Mono.empty();
    });
  }

  static final class ActiveActor<A extends AbstractActor> {

    private final A actor;

    private final String activationId;

    private ActiveActor(A actor, String activationId) {
      this.actor = actor;
      this.activationId = activationId;
    }
  }
}
