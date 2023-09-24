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

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ActorProxyBuilderTest {

  private static ActorClient actorClient;

  @BeforeAll
  public static void initClass() {
    actorClient = new ActorClient();
  }

  @AfterAll
  public static void tearDownClass() {
    actorClient.close();
  }

  @Test
  public void buildWithNullActorId() {
    assertThrows(IllegalArgumentException.class, () -> new ActorProxyBuilder("test", Object.class, actorClient)
        .build(null));
  }

  @Test
  public void buildWithEmptyActorType() {
    assertThrows(IllegalArgumentException.class, () -> new ActorProxyBuilder("", Object.class, actorClient));
  }

  @Test
  public void buildWithNullActorType() {
    assertThrows(IllegalArgumentException.class, () -> new ActorProxyBuilder(null, Object.class, actorClient));
  }

  @Test
  public void buildWithNullSerializer() {
    assertThrows(IllegalArgumentException.class, () -> new ActorProxyBuilder("MyActor", Object.class, actorClient)
      .withObjectSerializer(null)
      .build(new ActorId("100")));
  }

  @Test
  public void buildWithNullClient() {
    assertThrows(IllegalArgumentException.class, () -> new ActorProxyBuilder("MyActor", Object.class, null));
  }

  @Test()
  public void build() {
    ActorProxyBuilder<ActorProxy> builder = new ActorProxyBuilder("test", ActorProxy.class, actorClient);
    ActorProxy actorProxy = builder.build(new ActorId("100"));

    Assertions.assertNotNull(actorProxy);
    Assertions.assertEquals("test", actorProxy.getActorType());
    Assertions.assertEquals("100", actorProxy.getActorId().toString());
  }

  @Test()
  public void buildWithType() {
    ActorProxyBuilder<MyActor> builder = new ActorProxyBuilder(MyActor.class, actorClient);
    MyActor actorProxy = builder.build(new ActorId("100"));

    Assertions.assertNotNull(actorProxy);
  }

  @Test()
  public void buildWithTypeDefaultName() {
    ActorProxyBuilder<ActorWithDefaultName> builder = new ActorProxyBuilder(ActorWithDefaultName.class, actorClient);
    ActorWithDefaultName actorProxy = builder.build(new ActorId("100"));

    Assertions.assertNotNull(actorProxy);
  }

  @ActorType(name = "MyActor")
  public interface MyActor {
  }

  public interface ActorWithDefaultName {
  }

}
