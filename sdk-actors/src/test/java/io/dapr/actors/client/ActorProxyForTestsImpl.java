/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.ActorStateSerializer;
import io.dapr.client.DaprClient;

public class ActorProxyForTestsImpl extends ActorProxyImpl {

  public ActorProxyForTestsImpl(String actorType, ActorId actorId, ActorStateSerializer serializer, DaprClient daprClient) {
    super(actorType, actorId, serializer, daprClient);
  }
}
