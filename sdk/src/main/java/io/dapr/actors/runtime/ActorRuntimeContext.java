/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorTrace;

public class ActorRuntimeContext<T extends AbstractActor> {

  private final ActorRuntime actorRuntime;

  private final ActorStateSerializer actorSerializer;

  private final ActorFactory<T> actorFactory;

  private final ActorTypeInformation<T> actorTypeInformation;

  private final ActorTrace actorTrace;

  private final AppToDaprAsyncClient daprClient;

  ActorRuntimeContext(ActorRuntime actorRuntime,
                      ActorStateSerializer actorSerializer,
                      ActorFactory<T> actorFactory,
                      ActorTypeInformation<T> actorTypeInformation,
                      AppToDaprAsyncClient daprClient) {
    this.actorRuntime = actorRuntime;
    this.actorSerializer = actorSerializer;
    this.actorFactory = actorFactory;
    this.actorTypeInformation = actorTypeInformation;
    this.actorTrace = new ActorTrace();
    this.daprClient = daprClient;
  }

  ActorRuntime getActorRuntime() {
    return this.actorRuntime;
  }

  ActorStateSerializer getActorSerializer() {
    return this.actorSerializer;
  }

  ActorFactory<T> getActorFactory() {
    return this.actorFactory;
  }

  ActorTypeInformation<T> getActorTypeInformation() {
    return this.actorTypeInformation;
  }

  ActorTrace getActorTrace() { return this.actorTrace; }

  AppToDaprAsyncClient getDaprClient() { return this.daprClient; }
}
