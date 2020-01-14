/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyForTestsImpl;
import io.dapr.client.DaprClient;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActorStatefulTest {

  private static final AtomicInteger ACTOR_ID_COUNT = new AtomicInteger();

  private final ActorRuntimeContext context = createContext();

  private ActorManager<MyActorImpl> manager = new ActorManager<>(context);

  public interface MyActor {
    Mono<String> setMessage(String message);

    Mono<String> getMessage();

    Mono<Boolean> hasMessage();

    Mono<Void> deleteMessage();

    String getIdString();
  }

  @ActorType(Name = "MyActor")
  public static class MyActorImpl extends AbstractActor implements MyActor, Actor {

    private final ActorId id;

    public MyActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
      this.id = id;
    }

    @Override
    public Mono<String> setMessage(String message) {
      return super.setState("message", message).thenReturn(executeSayMethod(message));
    }

    @Override
    public Mono<String> getMessage() {
      return super.getState("message", String.class);
    }

    @Override
    public Mono<Boolean> hasMessage() {
      return super.containsState("message");
    }

    @Override
    public Mono<Void> deleteMessage() {
      return super.removeState("message");
    }

    @Override
    public String getIdString() {
      return this.id.toString();
    }

  }

  @Test
  public void happyGetSetDeleteContains() {
    ActorProxy proxy = newActorProxy();
    Assert.assertEquals(
      proxy.getActorId().toString(), proxy.invokeActorMethod("getIdString", String.class).block());
    Assert.assertFalse(proxy.invokeActorMethod("hasMessage", Boolean.class).block());

    proxy.invokeActorMethod("setMessage", "hello world").block();
    Assert.assertTrue(proxy.invokeActorMethod("hasMessage", Boolean.class).block());

    Assert.assertEquals(
      "hello world", proxy.invokeActorMethod("getMessage", String.class).block());

    Assert.assertEquals(
      executeSayMethod("hello world"),
      proxy.invokeActorMethod("setMessage", "hello world", String.class).block());

    proxy.invokeActorMethod("deleteMessage").block();
    Assert.assertFalse(proxy.invokeActorMethod("hasMessage", Boolean.class).block());
  }

  private ActorProxy newActorProxy() {
    ActorId actorId = newActorId();
    this.manager.activateActor(actorId).block();

    DaprClient daprClient = mock(DaprClient.class);
    when(daprClient.invokeActorMethod(
      eq(context.getActorTypeInformation().getName()),
      eq(actorId.toString()),
      any(),
      any()))
      .thenAnswer(invocationOnMock ->
        this.manager.invokeMethod(
          new ActorId(invocationOnMock.getArgument(1, String.class)),
          invocationOnMock.getArgument(2, String.class),
          context.getActorSerializer().unwrapMethodResponse(
            invocationOnMock.getArgument(3, String.class), String.class))
          .map(s -> {
            try {
              return context.getActorSerializer().wrapMethodRequest(s);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }));

    return new ActorProxyForTestsImpl(
      context.getActorTypeInformation().getName(),
      actorId,
      new ActorStateSerializer(),
      daprClient);
  }

  private static ActorId newActorId() {
    return new ActorId(Integer.toString(ACTOR_ID_COUNT.incrementAndGet()));
  }

  private static String executeSayMethod(String something) {
    return "Said: " + (something == null ? "" : something);
  }

  private static <T extends AbstractActor> ActorRuntimeContext createContext() {
    return new ActorRuntimeContext(
      mock(ActorRuntime.class),
      new ActorStateSerializer(),
      new DefaultActorFactory<T>(),
      ActorTypeInformation.create(MyActorImpl.class),
      mock(DaprClient.class),
      new DaprInMemoryStateProvider(new ActorStateSerializer())
    );
  }
}
