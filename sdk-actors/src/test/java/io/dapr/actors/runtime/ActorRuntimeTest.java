/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.mock;

public class ActorRuntimeTest {

  private static final String ACTOR_NAME = "MyGreatActor";

  public interface MyActor {
    String say();
  }

  @ActorType(name = ACTOR_NAME)
  public static class MyActorImpl extends AbstractActor implements MyActor {

    public MyActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
    }

    public String say() {
      return "Nothing to say.";
    }
  }

  private static final ActorObjectSerializer ACTOR_STATE_SERIALIZER = new ActorObjectSerializer();

  private static Constructor<ActorRuntime> constructor;

  private DaprClient mockDaprClient;

  private ActorRuntime runtime;

  @BeforeClass
  public static void beforeAll() throws Exception {
    constructor = (Constructor<ActorRuntime>) Arrays.stream(ActorRuntime.class.getDeclaredConstructors())
      .filter(c -> c.getParameters().length == 1)
      .map(c -> {
        c.setAccessible(true);
        return c;
      })
      .findFirst()
      .get();
  }

  @Before
  public void setup() throws Exception {
    this.mockDaprClient = mock(DaprClient.class);
    this.runtime = constructor.newInstance(this.mockDaprClient);
  }

  @Test
  public void registerActor() throws Exception {
    this.runtime.registerActor(MyActorImpl.class);
    Assert.assertTrue(new String(this.runtime.serializeConfig()).contains(ACTOR_NAME));
  }

  @Test
  public void activateActor() throws Exception {
    String actorId = UUID.randomUUID().toString();
    this.runtime.registerActor(MyActorImpl.class);
    this.runtime.activate(ACTOR_NAME, actorId).block();
  }

  @Test
  public void invokeActor() throws Exception {
    String actorId = UUID.randomUUID().toString();
    this.runtime.registerActor(MyActorImpl.class);
    this.runtime.activate(ACTOR_NAME, actorId).block();

    byte[] response = this.runtime.invoke(ACTOR_NAME, actorId, "say", null).block();
    String message = ACTOR_STATE_SERIALIZER.deserialize(response, String.class);
    Assert.assertEquals("Nothing to say.", message);
  }

  @Test
  public void activateThendeactivateActor() throws Exception {
    String actorId = UUID.randomUUID().toString();
    this.runtime.registerActor(MyActorImpl.class);
    this.runtime.activate(ACTOR_NAME, actorId).block();
    this.runtime.deactivate(ACTOR_NAME, actorId).block();
  }

  @Test
  public void deactivateActor() throws Exception {
    String actorId = UUID.randomUUID().toString();
    this.runtime.registerActor(MyActorImpl.class);
    this.runtime.deactivate(ACTOR_NAME, actorId).block();
  }

  @Test
  public void lazyActivate() throws Exception {
    String actorId = UUID.randomUUID().toString();
    this.runtime.registerActor(MyActorImpl.class);
    this.runtime.activate(ACTOR_NAME, actorId).block();

    this.runtime.invoke(ACTOR_NAME, actorId, "say", null)
      .doOnError(e -> Assert.assertTrue(e.getMessage().contains("Could not find actor")))
      .doOnSuccess(s -> Assert.fail())
      .onErrorReturn("".getBytes())
      .block();
  }

  @Test
  public void lazyDeactivate() throws Exception {
    String actorId = UUID.randomUUID().toString();
    this.runtime.registerActor(MyActorImpl.class);
    this.runtime.activate(ACTOR_NAME, actorId).block();

    Mono<Void> deacticateCall = this.runtime.deactivate(ACTOR_NAME, actorId);

    this.runtime.invoke(ACTOR_NAME, actorId, "say", null).block();

    deacticateCall.block();

    this.runtime.invoke(ACTOR_NAME, actorId, "say", null)
      .doOnError(e -> Assert.assertTrue(e.getMessage().contains("Could not find actor")))
      .doOnSuccess(s -> Assert.fail())
      .onErrorReturn("".getBytes())
      .block();
  }

  @Test
  public void lazyInvoke() throws Exception {
    String actorId = UUID.randomUUID().toString();
    this.runtime.registerActor(MyActorImpl.class);

    Mono<byte[]> invokeCall = this.runtime.invoke(ACTOR_NAME, actorId, "say", null);

    this.runtime.activate(ACTOR_NAME, actorId).block();

    invokeCall.block();
  }

}
