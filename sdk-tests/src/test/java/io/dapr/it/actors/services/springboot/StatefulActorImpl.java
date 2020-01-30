/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.services.springboot;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.AbstractActor;
import io.dapr.actors.runtime.ActorRuntimeContext;
import io.dapr.actors.runtime.ActorType;

@ActorType(name = "StatefulActorTest")
public class StatefulActorImpl extends AbstractActor implements StatefulActor {

  public StatefulActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
    super(runtimeContext, id);
  }

  @Override
  public void writeMessage(String something) {
    super.getActorStateManager().set("message", something).block();
  }

  @Override
  public String readMessage() {
    if (super.getActorStateManager().contains("message").block()) {
      return super.getActorStateManager().get("message", String.class).block();
    }

    return null;
  }

  @Override
  public void writeData(MyData something) {
    super.getActorStateManager().set("mydata", something).block();
  }

  @Override
  public MyData readData() {
    if (super.getActorStateManager().contains("mydata").block()) {
      return super.getActorStateManager().get("mydata", MyData.class).block();
    }

    return null;
  }
}