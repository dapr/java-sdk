/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

public class ActorRuntimeContext<T extends AbstractActor> {

  private final ActorRuntime actorRuntime;

  private final ActorStateSerializer actorSerializer;

  private final ActorService<T> actorService;

  private final ActorTypeInformation<T> actorTypeInformation;

  ActorRuntimeContext(ActorRuntime actorRuntime,
                      ActorStateSerializer actorSerializer,
                      ActorService<T> actorService,
                      ActorTypeInformation<T> actorTypeInformation) {
    this.actorRuntime = actorRuntime;
    this.actorSerializer = actorSerializer;
    this.actorService = actorService;
    this.actorTypeInformation = actorTypeInformation;
  }

  ActorRuntime getActorRuntime() {
    return actorRuntime;
  }

  ActorStateSerializer getActorSerializer() {
    return actorSerializer;
  }

  ActorService<T> getActorService() {
    return actorService;
  }

  ActorTypeInformation<T> getActorTypeInformation() {
    return actorTypeInformation;
  }
}
