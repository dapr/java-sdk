package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.Assert;
import org.junit.Test;

public class ActorProxyBuilderTest {

  @Test(expected = IllegalArgumentException.class)
  public void buildWithNullActorId() {
    new ActorProxyBuilder("test", new DefaultObjectSerializer())
        .build(null);

  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithEmptyActorType() {
    new ActorProxyBuilder("", new DefaultObjectSerializer())
        .build(new ActorId("100"));

  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithNullActorType() {
    new ActorProxyBuilder(null, new DefaultObjectSerializer())
      .build(new ActorId("100"));

  }

  @Test(expected = IllegalArgumentException.class)
  public void buildWithNullSerializer() {
    new ActorProxyBuilder("MyActor", null)
      .build(new ActorId("100"));

  }

  @Test()
  public void build() {
    ActorProxyBuilder builder = new ActorProxyBuilder("test", new DefaultObjectSerializer());
    ActorProxy actorProxy = builder.build(new ActorId("100"));

    Assert.assertNotNull(actorProxy);
    Assert.assertEquals("test", actorProxy.getActorType());
    Assert.assertEquals("100", actorProxy.getActorId().toString());

  }
}