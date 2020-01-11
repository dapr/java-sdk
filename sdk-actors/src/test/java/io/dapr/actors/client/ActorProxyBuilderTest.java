package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class ActorProxyBuilderTest {

  @Test(expected = IllegalArgumentException.class)
  public void buildWithNullActorId() {
    new ActorProxyBuilder()
        .withActorId(null)
        .withActorType("test")
        .build();

  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithEmptyActorType() {
    new ActorProxyBuilder()
        .withActorId(new ActorId("100"))
        .withActorType("")
        .build();

  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithNullActorType() {
    new ActorProxyBuilder()
        .withActorId(new ActorId("100"))
        .withActorType(null)
        .build();

  }

  @Test()
  public void build() {
    ActorProxyBuilder builder = new ActorProxyBuilder();
    builder.withActorId(new ActorId("100"));
    builder.withActorType("test");
    ActorProxy actorProxy = builder.build();

    Assert.assertNotNull(actorProxy);
    Assert.assertEquals("test", actorProxy.getActorType());
    Assert.assertEquals("100", actorProxy.getActorId().toString());

  }
}