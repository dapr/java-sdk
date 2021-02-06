/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ActorProxyBuilderTest {

  private static ActorClient actorClient;

  @BeforeClass
  public static void initClass() {
    actorClient = new ActorClient();
  }

  @AfterClass
  public static void tearDownClass() {
    actorClient.close();
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithNullActorId() {
    new ActorProxyBuilder("test", Object.class, actorClient)
        .build(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithEmptyActorType() {
    new ActorProxyBuilder("", Object.class, actorClient);
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithNullActorType() {
    new ActorProxyBuilder(null, Object.class, actorClient);
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithNullSerializer() {
    new ActorProxyBuilder("MyActor", Object.class, actorClient)
      .withObjectSerializer(null)
      .build(new ActorId("100"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithNullClient() {
    new ActorProxyBuilder("MyActor", Object.class, null);
  }

  @Test()
  public void build() {
    ActorProxyBuilder<ActorProxy> builder = new ActorProxyBuilder("test", ActorProxy.class, actorClient);
    ActorProxy actorProxy = builder.build(new ActorId("100"));

    Assert.assertNotNull(actorProxy);
    Assert.assertEquals("test", actorProxy.getActorType());
    Assert.assertEquals("100", actorProxy.getActorId().toString());
  }

  @Test()
  public void buildWithType() {
    ActorProxyBuilder<MyActor> builder = new ActorProxyBuilder(MyActor.class, actorClient);
    MyActor actorProxy = builder.build(new ActorId("100"));

    Assert.assertNotNull(actorProxy);
  }

  @Test()
  public void buildWithTypeDefaultName() {
    ActorProxyBuilder<ActorWithDefaultName> builder = new ActorProxyBuilder(ActorWithDefaultName.class, actorClient);
    ActorWithDefaultName actorProxy = builder.build(new ActorId("100"));

    Assert.assertNotNull(actorProxy);
  }

  @ActorType(name = "MyActor")
  public interface MyActor {
  }

  public interface ActorWithDefaultName {
  }

}
