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
                .withPort(20)
                .build();

    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithEmptyActorType() {
        new ActorProxyBuilder()
                .withActorId(new ActorId("100"))
                .withActorType("")
                .withPort(20)
                .build();

    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithNullActorType() {
        new ActorProxyBuilder()
                .withActorId(new ActorId("100"))
                .withActorType(null)
                .withPort(20)
                .build();

    }

    @Test()
    public void build() {
        ActorProxy actorProxy = new ActorProxyBuilder()
                .withActorId(new ActorId("100"))
                .withActorType("test")
                .withPort(20)
                .build();

        Assert.assertNotNull(actorProxy);
        Assert.assertEquals("test",actorProxy.getActorType());
        Assert.assertEquals("100",actorProxy.getActorId().toString());

    }
}