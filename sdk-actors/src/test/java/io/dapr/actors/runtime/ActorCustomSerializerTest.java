/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyImplForTests;
import io.dapr.actors.client.DaprClientStub;
import io.dapr.serializer.DaprObjectSerializer;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActorCustomSerializerTest {

  private static final ActorObjectSerializer INTERNAL_SERIALIZER = new ActorObjectSerializer();

  private static final DaprObjectSerializer CUSTOM_SERIALIZER = new JavaSerializer();

  private static final AtomicInteger ACTOR_ID_COUNT = new AtomicInteger();

  private final ActorRuntimeContext context = createContext();

  private ActorManager<ActorImpl> manager = new ActorManager<>(context);

  public interface MyActor {
    Mono<Integer> intInIntOut(int input);

    Mono<String> stringInStringOut(String input);

    Mono<MyData> classInClassOut(MyData input);
  }

  @ActorType(name = "MyActor")
  public static class ActorImpl extends AbstractActor implements MyActor {

    //public MyActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
    public ActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
    }

    @Override
    public Mono<Integer> intInIntOut(int input) {
      return Mono.fromSupplier(() -> input + input);
    }

    @Override
    public Mono<String> stringInStringOut(String input) {
      return Mono.fromSupplier(() -> input + input);
    }

    @Override
    public Mono<MyData> classInClassOut(MyData input) {
      return Mono.fromSupplier(() -> new MyData(
          input.getName() + input.getName(),
          input.getNum() + input.getNum())
      );
    }
  }

  static class MyData implements Serializable {
    private String name;
    private int num;

    public MyData() {
      this.name = "";
      this.num = 0;
    }

    public MyData(String name, int num) {
      this.name = name;
      this.num = num;
    }

    public String getName() {
      return this.name;
    }

    public int getNum() {
      return this.num;
    }
  }

  @Test
  public void classInClassOut() {
    ActorProxy actorProxy = createActorProxy();
    MyData d = new MyData("hi", 3);

    MyData response = actorProxy.invokeMethod("classInClassOut", d, MyData.class).block();

    Assert.assertEquals("hihi", response.getName());
    Assert.assertEquals(6, response.getNum());
  }

  @Test
  public void stringInStringOut() {
    ActorProxy actorProxy = createActorProxy();
    String response = actorProxy.invokeMethod("stringInStringOut", "oi", String.class).block();

    Assert.assertEquals("oioi", response);
  }

  @Test
  public void intInIntOut() {
    ActorProxy actorProxy = createActorProxy();
    int response = actorProxy.invokeMethod("intInIntOut", 2, int.class).block();

    Assert.assertEquals(4, response);
  }

  private static ActorId newActorId() {
    return new ActorId(Integer.toString(ACTOR_ID_COUNT.incrementAndGet()));
  }

  private ActorProxy createActorProxy() {
    ActorId actorId = newActorId();

    // Mock daprClient for ActorProxy only, not for runtime.
    DaprClientStub daprClient = mock(DaprClientStub.class);

    when(daprClient.invoke(
      eq(context.getActorTypeInformation().getName()),
      eq(actorId.toString()),
      any(),
      any()))
      .thenAnswer(invocationOnMock ->
        this.manager.invokeMethod(
          new ActorId(invocationOnMock.getArgument(1, String.class)),
          invocationOnMock.getArgument(2, String.class),
          invocationOnMock.getArgument(3, byte[].class)));

    this.manager.activateActor(actorId).block();

    return new ActorProxyImplForTests(
      context.getActorTypeInformation().getName(),
      actorId,
      CUSTOM_SERIALIZER,
      daprClient);
  }

  private static <T extends AbstractActor> ActorRuntimeContext createContext() {
    DaprClient daprClient = mock(DaprClient.class);

    when(daprClient.registerTimer(any(), any(), any(), any())).thenReturn(Mono.empty());
    when(daprClient.registerReminder(any(), any(), any(), any())).thenReturn(Mono.empty());
    when(daprClient.unregisterTimer(any(), any(), any())).thenReturn(Mono.empty());
    when(daprClient.unregisterReminder(any(), any(), any())).thenReturn(Mono.empty());

    return new ActorRuntimeContext(
      mock(ActorRuntime.class),
      CUSTOM_SERIALIZER,
      new DefaultActorFactory<T>(),
      ActorTypeInformation.create(ActorImpl.class),
      daprClient,
      mock(DaprStateAsyncProvider.class)
    );
  }
}
