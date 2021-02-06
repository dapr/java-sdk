/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorMethod;
import io.dapr.actors.ActorType;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyImplForTests;
import io.dapr.actors.client.DaprClientStub;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActorNoStateTest {
  private static final ActorObjectSerializer INTERNAL_SERIALIZER = new ActorObjectSerializer();

  private static final AtomicInteger ACTOR_ID_COUNT = new AtomicInteger();

  private final ActorRuntimeContext context = createContext();

  private ActorManager<ActorImpl> manager = new ActorManager<>(context);

  public interface MyActor {
    // The test will only call the versions of this in a derived class to the user code base class.
    // The user code base class version will throw.
    Mono<String> getMyId();
    Mono<String> stringInStringOut(String input);
    Mono<Boolean> stringInBooleanOut(String input);
    Mono<Void> stringInVoidOutIntentionallyThrows(String input);
    Mono<MyData> classInClassOut(MyData input);
    Mono<String> registerBadCallbackName();
    String registerTimerAutoName();
    @ActorMethod(name = "DotNetMethodASync")
    Mono<Void> dotNetMethod();
  }

  @ActorType(name = "MyActor")
  public static class ActorImpl extends AbstractActor implements MyActor {
    private boolean activated;
    private boolean methodReturningVoidInvoked;

    //public MyActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
    public ActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
      this.activated = true;
      this.methodReturningVoidInvoked = false;
    }

    @Override
    public Mono<String> getMyId() {
      return Mono.fromSupplier(() -> super.getId().toString());
    }

    @Override
    public Mono<String> stringInStringOut(String s) {
      return Mono.fromSupplier(() -> {
          return s + s;
        }
      );
    }

    @Override
    public Mono<Boolean> stringInBooleanOut(String s) {
      return Mono.fromSupplier(() -> {
        if (s.equals("true")) {
          return true;
        } else {
          return false;
        }
      });
    }

    @Override
    public Mono<Void> stringInVoidOutIntentionallyThrows(String input) {
      return Mono.fromRunnable(() -> {
        // IllegalMonitorStateException is being thrown only because it's un unusual exception so it's unlikely
        // to collide with something else.
        throw new IllegalMonitorStateException("IntentionalException");
      });
    }

    @Override
    public Mono<MyData> classInClassOut(MyData input) {
      return Mono.fromSupplier(() -> {
        return new MyData(
          input.getName() + input.getName(),
          input.getNum() + input.getNum());
      });
    }

    @Override
    public Mono<String> registerBadCallbackName() {
      return super.registerActorTimer("mytimer", "", "state", Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    @Override
    public String registerTimerAutoName() {
      return super.registerActorTimer("", "anything", "state", Duration.ofSeconds(1), Duration.ofSeconds(1)).block();
    }

    @Override
    public Mono<Void> dotNetMethod() {
      return Mono.empty();
    }
  }

  static class MyData {
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
  public void actorId() {
    ActorProxy proxy = createActorProxy();

    Assert.assertEquals(
            proxy.getActorId().toString(),
            proxy.invokeMethod("getMyId", String.class).block());
  }

  @Test
  public void stringInStringOut() {
    ActorProxy proxy = createActorProxy();

    // these should only call the actor methods for ActorChild.  The implementations in ActorParent will throw.
    Assert.assertEquals(
      "abcabc",
      proxy.invokeMethod("stringInStringOut", "abc", String.class).block());
  }

  @Test
  public void stringInBooleanOut() {
    ActorProxy proxy = createActorProxy();

    // these should only call the actor methods for ActorChild.  The implementations in ActorParent will throw.
    Assert.assertEquals(
      false,
      proxy.invokeMethod("stringInBooleanOut", "hello world", Boolean.class).block());

    Assert.assertEquals(
      true,
      proxy.invokeMethod("stringInBooleanOut", "true", Boolean.class).block());
  }

  @Test(expected = IllegalMonitorStateException.class)
  public void stringInVoidOutIntentionallyThrows() {
    ActorProxy actorProxy = createActorProxy();

    // these should only call the actor methods for ActorChild.  The implementations in ActorParent will throw.
    actorProxy.invokeMethod("stringInVoidOutIntentionallyThrows", "hello world").block();
  }

  @Test
  public void testMethodNameChange() {
    MyActor actor = createActorProxy(MyActor.class);
    actor.dotNetMethod();
  }

  @Test
  public void classInClassOut() {
    ActorProxy actorProxy = createActorProxy();
    MyData d = new MyData("hi", 3);

    // this should only call the actor methods for ActorChild.  The implementations in ActorParent will throw.
    MyData response = actorProxy.invokeMethod("classInClassOut", d, MyData.class).block();

    Assert.assertEquals(
      "hihi",
      response.getName());
    Assert.assertEquals(
      6,
      response.getNum());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadTimerCallbackName() {
    MyActor actor = createActorProxy(MyActor.class);
    actor.registerBadCallbackName().block();
  }

  @Test
  public void testAutoTimerName() {
    MyActor actor = createActorProxy(MyActor.class);
    String firstTimer = actor.registerTimerAutoName();
    Assert.assertTrue((firstTimer != null) && !firstTimer.isEmpty());

    String secondTimer = actor.registerTimerAutoName();
    Assert.assertTrue((secondTimer != null) && !secondTimer.isEmpty());

    Assert.assertNotEquals(firstTimer, secondTimer);
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
      new DefaultObjectSerializer(),
      daprClient);
  }

  private <T> T createActorProxy(Class<T> clazz) {
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

    ActorProxyImplForTests proxy = new ActorProxyImplForTests(
            context.getActorTypeInformation().getName(),
            actorId,
            new DefaultObjectSerializer(),
            daprClient);
    return (T) Proxy.newProxyInstance(
            ActorProxyImplForTests.class.getClassLoader(),
            new Class[]{clazz},
            proxy);
  }

  private static <T extends AbstractActor> ActorRuntimeContext createContext() {
    DaprClient daprClient = mock(DaprClient.class);

    when(daprClient.registerTimer(any(), any(), any(), any())).thenReturn(Mono.empty());
    when(daprClient.registerReminder(any(), any(), any(), any())).thenReturn(Mono.empty());
    when(daprClient.unregisterTimer(any(), any(), any())).thenReturn(Mono.empty());
    when(daprClient.unregisterReminder(any(), any(), any())).thenReturn(Mono.empty());

    return new ActorRuntimeContext(
      mock(ActorRuntime.class),
      new DefaultObjectSerializer(),
      new DefaultActorFactory<T>(),
      ActorTypeInformation.create(ActorImpl.class),
      daprClient,
      mock(DaprStateAsyncProvider.class)
    );
  }
}
