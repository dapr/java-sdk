/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.serializer.DaprObjectSerializer;

public class ActorProxyImplForTests extends ActorProxyImpl {

  public ActorProxyImplForTests(String actorType,
                                ActorId actorId,
                                DaprObjectSerializer serializer,
                                ActorClient actorClient,
                                ActorRuntime actorRuntime) {
    super(actorType, actorId, serializer, actorClient, actorRuntime);
  }
}
