/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
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