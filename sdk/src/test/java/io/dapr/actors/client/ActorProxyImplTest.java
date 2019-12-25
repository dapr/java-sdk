package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import org.junit.Assert;
import org.junit.Test;

public class ActorProxyImplTest {

    @Test()
    public void constructorActorProxyTest() {
        final ActorProxyHttpAsyncClient actorProxyAsyncClient = (ActorProxyHttpAsyncClient)new ActorProxyClientBuilder().buildAsyncClient();
        final ActorProxyImpl actorProxy= new ActorProxyImpl(new ActorId("100"),"myActorType",actorProxyAsyncClient);
        Assert.assertEquals(actorProxy.getActorId().getStringId(),"100");
        Assert.assertEquals(actorProxy.getActorType(),"myActorType");
    }
}
