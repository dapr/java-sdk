/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
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
  private final Map<ActorId, T> activeActors;

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
    return Mono.fromSupplier(() -> {
      if (this.activeActors.containsKey(actorId)) {
        return null;
      }

      return this.runtimeContext.getActorFactory().createActor(runtimeContext, actorId);
    }).flatMap(actor -> actor.onActivateInternal().then(this.onActivatedActor(actorId, actor)));
  }

  /**
   * Deactivates an Actor.
   *
   * @param actorId Actor identifier.
   * @return Asynchronous void response.
   */
  Mono<Void> deactivateActor(ActorId actorId) {
    return Mono.fromSupplier(() -> this.activeActors.remove(actorId)).flatMap(actor -> actor.onDeactivateInternal());
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
    return Mono.fromSupplier(() -> {
      if (!this.runtimeContext.getActorTypeInformation().isRemindable()) {
        return null;
      }

      try {
        return OBJECT_SERIALIZER.deserialize(params, ActorReminderParams.class);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).flatMap(p ->
        invoke(actorId,
            ActorMethodContext.createForReminder(reminderName),
            actor -> doReminderInvokation((Remindable) actor, reminderName, p))).then();
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
    return Mono.fromSupplier(() -> {
      try {
        return OBJECT_SERIALIZER.deserialize(params, ActorTimerParams.class);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).flatMap(p ->
            invokeMethod(
                    actorId,
                    ActorMethodContext.createForTimer(timerName),
                    p.getCallback(),
                    p.getData())).then();
  }

  /**
   * Internal callback for when Actor is activated.
   *
   * @param actorId Actor identifier.
   * @param actor   Actor's instance.
   * @return Asynchronous void response.
   */
  private Mono<Void> onActivatedActor(ActorId actorId, T actor) {
    return Mono.fromRunnable(() -> this.activeActors.put(actorId, actor));
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
    return invokeMethod(actorId, null, methodName, request);
  }

  /**
   * Internal method to actually invoke Actor's method.
   *
   * @param actorId    Identifier for the Actor.
   * @param context    Method context to be invoked.
   * @param methodName Method name to be invoked.
   * @param request    Input object to be passed in to the invoked method.
   * @return Asynchronous serialized response.
   */
  private Mono<byte[]> invokeMethod(ActorId actorId, ActorMethodContext context, String methodName, byte[] request) {
    ActorMethodContext actorMethodContext = context;
    if (actorMethodContext == null) {
      actorMethodContext = ActorMethodContext.createForActor(methodName);
    }

    return this.invoke(actorId, actorMethodContext, actor -> {
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
   * @param actorId Actor identifier.
   * @param context Context for the method/timer/reminder call.
   * @param func    Function to perform the method call.
   * @param <T>     Expected return type for the function call.
   * @return Asynchronous response for the returned object.
   */
  private <T> Mono<T> invoke(ActorId actorId, ActorMethodContext context, Function<AbstractActor, Mono<T>> func) {
    try {
      AbstractActor actor = this.activeActors.getOrDefault(actorId, null);
      if (actor == null) {
        throw new IllegalArgumentException(
            String.format("Could not find actor %s of type %s.",
                actorId.toString(),
                this.runtimeContext.getActorTypeInformation().getName()));
      }

      return actor.onPreActorMethodInternal(context)
          .then((Mono<Object>) func.apply(actor))
          .switchIfEmpty(
              actor.onPostActorMethodInternal(context))
          .flatMap(r -> actor.onPostActorMethodInternal(context).thenReturn(r))
          .onErrorMap(throwable -> {
            actor.rollback();
            return throwable;
          })
          .map(o -> (T) o);
    } catch (Exception e) {
      return Mono.error(e);
    }
  }
}
