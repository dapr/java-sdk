package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import reactor.core.publisher.Flux;
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

  private final ActorTypeInformation<T> actorType;

  private final ActorService<T> actorService;

  private final Map<ActorId, T> activeActors;

  private final ActorMethodInfoMap actorMethods;

  private final ActorStateSerializer actorStateSerializer;

  ActorManager(ActorTypeInformation<T> actorType, ActorStateSerializer actorStateSerializer, ActorService<T> actorService) {
    this.actorType = actorType;
    this.actorStateSerializer = actorStateSerializer;
    this.actorService = actorService;
    this.activeActors = Collections.synchronizedMap(new HashMap<>());
    this.actorMethods = new ActorMethodInfoMap(actorType.getInterfaces());
  }

  Mono<String> invokeMethod(ActorId actorId, String methodName, String request) {
    return invokeMethod(actorId, null, methodName, request);
  }

  Mono<Void> invokeReminder(ActorId actorId, String reminderName, String request) {
    if (!this.actorType.isRemindable()) {
      return Mono.empty();
    }

    try {
      ReminderInfo reminder = this.actorStateSerializer.deserialize(request, ReminderInfo.class);

      return invoke(actorId, ActorMethodContext.CreateForReminder(reminderName), actor ->
        ((Remindable) actor).receiveReminder(
          reminderName,
          reminder.getData(),
          reminder.getDueTime(),
          reminder.getPeriod())).then();
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  Mono<Void> invokeTimer(ActorId actorId, String timerName) {
    try {
      AbstractActor actor = this.activeActors.getOrDefault(actorId, null);
      if (actor == null) {
        throw new IllegalArgumentException(
          String.format("Could not find actor %s of type %s.", actorId.getStringId(), this.actorType.getName()));
      }

      ActorTimer<?> actorTimer = actor.getActorTimer(timerName);
      if (actorTimer == null) {
        throw new IllegalStateException(
          String.format("Could not find timer %s for actor %s.", timerName, this.actorType.getName()));
      }

      return invokeMethod(
        actorId,
        ActorMethodContext.CreateForTimer(timerName),
        actorTimer.getMethodName(),
        actorTimer.getState())
        .then();
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  Mono<Void> activateActor(ActorId actorId) {
    AbstractActor actor = this.actorService.createActor(actorId);

    actor.
  }

  private Mono<String> invokeMethod(ActorId actorId, ActorMethodContext context, String methodName, Object request) {
    ActorMethodContext actorMethodContext = context;
    if (actorMethodContext == null) {
      actorMethodContext = ActorMethodContext.CreateForActor(methodName);
    }

    return this.invoke(actorId, actorMethodContext, actor -> {
      try {
        Class<T> clazz = this.actorType.getImplementationClass();

        // Finds the actor method with the given name and 1 or no parameter.
        Method method = this.actorMethods.get(methodName);

        Object response = null;

        if (method.getParameterCount() == 0) {
          response = method.invoke(actor);
        } else {
          // Actor methods must have a one or no parameter, which is guaranteed at this point.
          Class<?> inputClass = method.getParameterTypes()[0];

          if ((request != null) && !inputClass.isInstance(request)) {
            // If request object is String, we deserialize it.
            response = method.invoke(actor, this.actorStateSerializer.deserialize((String) request, inputClass));
          } else {
            // If input already of the right type, so we just cast it.
            response = method.invoke(actor, inputClass.cast(request));
          }
        }

        if (response == null) {
          return Mono.empty();
        }

        if (response instanceof Mono) {
          return ((Mono<Object>) response).map(r -> {
            try {
              return this.actorStateSerializer.serialize(r);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
        }

        // Method was not Mono, so we serialize response.
        return Mono.just(this.actorStateSerializer.serialize(response));
      } catch (Exception e) {
        return Mono.error(e);
      }
    }).map(r -> r.toString());
  }

  private <T> Mono<Object> invoke(ActorId actorId, ActorMethodContext context, Function<AbstractActor, Mono<T>> func) {
    try {
      AbstractActor actor = this.activeActors.getOrDefault(actorId, null);
      if (actor == null) {
        throw new IllegalArgumentException(
          String.format("Could not find actor %s of type %s.", actorId.getStringId(), this.actorType.getName()));
      }

      Mono<Void> preMethodCall = actor.onPreActorMethodInternal(context);
      Mono<T> methodCall = func.apply(actor);
      Mono<Void> postMethodCall = actor.onPostActorMethodInternal(context);

      // TODO: find a way to make this generic and return Mono<T> instead of Mono<Object>.
      return Flux.concat(preMethodCall, methodCall, postMethodCall).singleOrEmpty();
    } catch (Exception e) {
      return Mono.error(e);
    }
  }
}
