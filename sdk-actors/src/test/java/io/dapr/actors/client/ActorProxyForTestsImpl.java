/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.serializer.DaprObjectSerializer;

public class ActorProxyForTestsImpl extends ActorProxyImpl {

  public ActorProxyForTestsImpl(String actorType, ActorId actorId, DaprObjectSerializer serializer, DaprClient daprClient) {
    super(actorType, actorId, serializer, daprClient);
  }
}
