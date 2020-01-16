package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
    T actor = this.runtimeContext.getActorFactory().createActor(runtimeContext, actorId);

    return actor.onActivateInternal().then(this.onActivatedActor(actorId, actor));
  }

  /**
   * Deactivates an Actor.
   *
   * @param actorId Actor identifier.
   * @return Asynchronous void response.
   */
  Mono<Void> deactivateActor(ActorId actorId) {
    T actor = this.activeActors.remove(actorId);
    if (actor != null) {
      return actor.onDeactivateInternal();
    }

    return Mono.empty();
  }

  /**
   * Invokes a given method in the Actor.
   *
   * @param actorId    Identifier for Actor being invoked.
   * @param methodName Name of method being invoked.
   * @param request    Input object for the method being invoked.
   * @return Asynchronous void response.
   */
  Mono<String> invokeMethod(ActorId actorId, String methodName, String request) {
    return invokeMethod(actorId, null, methodName, request);
  }

  /**
   * Invokes reminder for Actor.
   *
   * @param actorId      Identifier for Actor being invoked.
   * @param reminderName Name of reminder being invoked.
   * @param params      Parameters for the reminder.
   * @return Asynchronous void response.
   */
  Mono<Void> invokeReminder(ActorId actorId, String reminderName, String params) {
    if (!this.runtimeContext.getActorTypeInformation().isRemindable()) {
      return Mono.empty();
    }

    try {
      ActorReminderParams paramsObject = this
        .runtimeContext
        .getActorSerializer()
        .deserialize(params, ActorReminderParams.class);
      return invoke(
        actorId,
        ActorMethodContext.CreateForReminder(reminderName),
        actor -> doReminderInvokation((Remindable) actor, reminderName, paramsObject))
        .then();
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  /**
   * Invokes a timer for a given Actor.
   *
   * @param actorId   Identifier for Actor.
   * @param timerName Name of timer being invoked.
   * @return Asynchronous void response.
   */
  Mono<Void> invokeTimer(ActorId actorId, String timerName) {
    return Mono.fromSupplier(() -> {
      AbstractActor actor = this.activeActors.getOrDefault(actorId, null);
      if (actor == null) {
        throw new IllegalArgumentException(
          String.format("Could not find actor %s of type %s.",
            actorId.toString(),
            this.runtimeContext.getActorTypeInformation().getName()));
      }

      ActorTimer actorTimer = actor.getActorTimer(timerName);
      if (actorTimer == null) {
        throw new IllegalStateException(
          String.format("Could not find timer %s for actor %s.",
            timerName,
            this.runtimeContext.getActorTypeInformation().getName()));
      }

      return actorTimer;
    }).flatMap(actorTimer -> invokeMethod(
      actorId,
      ActorMethodContext.CreateForTimer(actorTimer.getName()),
      actorTimer.getCallback(),
      actorTimer.getState()))
      .then();
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
        Object data = this.runtimeContext.getActorSerializer().deserialize(
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
   * Internal method to actually invoke Actor's method.
   *
   * @param actorId    Identifier for the Actor.
   * @param context    Method context to be invoked.
   * @param methodName Method name to be invoked.
   * @param request    Input object to be passed in to the invoked method.
   * @return Asynchronous void response.
   */
  private Mono<String> invokeMethod(ActorId actorId, ActorMethodContext context, String methodName, Object request) {
    ActorMethodContext actorMethodContext = context;
    if (actorMethodContext == null) {
      actorMethodContext = ActorMethodContext.CreateForActor(methodName);
    }

    return this.invoke(actorId, actorMethodContext, actor -> {
      try {
        // Finds the actor method with the given name and 1 or no parameter.
        Method method = this.actorMethods.get(methodName);

        if (method.getReturnType().equals(Mono.class)) {
          Mono<Object> mono = (Mono<Object>) invokeMethod(actor, method, request);
          if (mono == null) {
            return Mono.just(new Object());
          }

          return mono.defaultIfEmpty("").map(r -> {
            try {
              return (Object) this.runtimeContext.getActorSerializer().serializeString(r);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
        }

        return Mono.fromSupplier(() -> {
          try {
            Object response = invokeMethod(actor, method, request);

            if (response == null) {
              return new Object();
            }

            // Method was not Mono, so we serialize response.
            return this.runtimeContext.getActorSerializer().serializeString(response);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
      } catch (Exception e) {
        return Mono.error(e);
      }
    }).map(r -> r.toString());
  }

  private Object invokeMethod(AbstractActor actor, Method method, Object request)
    throws IllegalAccessException, InvocationTargetException, IOException {
    Object response;
    if (method.getParameterCount() == 0) {
      response = method.invoke(actor);
    } else {
      // Actor methods must have a one or no parameter, which is guaranteed at this point.
      Class<?> inputClass = method.getParameterTypes()[0];

      if ((request != null) && !inputClass.isInstance(request)) {
        // If request object is String, we deserialize it.
        response = method.invoke(
          actor,
          this.runtimeContext.getActorSerializer().deserialize(request, inputClass));
      } else {
        // If input already of the right type, so we just cast it.
        response = method.invoke(actor, inputClass.cast(request));
      }
    }
    return response;
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
        .then(func.apply(actor))
        .flatMap(result -> actor.onPostActorMethodInternal(context).thenReturn(result))
        .onErrorMap(throwable -> {
          actor.resetState();
          return throwable;
        });
    } catch (Exception e) {
      return Mono.error(e);
    }
  }
}
