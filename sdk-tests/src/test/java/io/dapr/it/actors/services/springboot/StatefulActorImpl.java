/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.services.springboot;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import io.dapr.actors.runtime.AbstractActor;
import io.dapr.actors.runtime.ActorRuntimeContext;

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
  public void writeName(String something) {
    super.getActorStateManager().set("name", something).block();
  }

  @Override
  public String readName() {
    if (super.getActorStateManager().contains("name").block()) {
      return super.getActorStateManager().get("name", String.class).block();
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

  @Override
  public void writeBytes(byte[] something) {
    super.getActorStateManager().set("bytes", something).block();
  }

  @Override
  public byte[] readBytes() {
    if (super.getActorStateManager().contains("bytes").block()) {
      return super.getActorStateManager().get("bytes", byte[].class).block();
    }

    return null;
  }
}