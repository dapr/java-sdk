/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import org.junit.Assert;
import org.junit.Test;

public class ActorProxyBuilderTest {

  @Test(expected = IllegalArgumentException.class)
  public void buildWithNullActorId() {
    new ActorProxyBuilder("test", Object.class)
        .build(null);

  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithEmptyActorType() {
    new ActorProxyBuilder("", Object.class)
        .build(new ActorId("100"));

  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithNullActorType() {
    new ActorProxyBuilder(null, Object.class)
      .build(new ActorId("100"));

  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithNullSerializer() {
    new ActorProxyBuilder("MyActor", Object.class)
      .withObjectSerializer(null)
      .build(new ActorId("100"));

  }

  @Test()
  public void build() {
    ActorProxyBuilder<ActorProxy> builder = new ActorProxyBuilder("test", ActorProxy.class);
    ActorProxy actorProxy = builder.build(new ActorId("100"));

    Assert.assertNotNull(actorProxy);
    Assert.assertEquals("test", actorProxy.getActorType());
    Assert.assertEquals("100", actorProxy.getActorId().toString());
  }

  @Test()
  public void buildWithType() {
    ActorProxyBuilder<MyActor> builder = new ActorProxyBuilder(MyActor.class);
    MyActor actorProxy = builder.build(new ActorId("100"));

    Assert.assertNotNull(actorProxy);
  }

  @Test()
  public void buildWithTypeDefaultName() {
    ActorProxyBuilder<MyActorWithDefaultName> builder = new ActorProxyBuilder(MyActorWithDefaultName.class);
    MyActorWithDefaultName actorProxy = builder.build(new ActorId("100"));

    Assert.assertNotNull(actorProxy);
  }

  @ActorType(name = "MyActor")
  public interface MyActor {
  }

  public interface MyActorWithDefaultName {
  }
}