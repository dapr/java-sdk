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
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DerivedActorTest {
  private static final ActorObjectSerializer INTERNAL_SERIALIZER = new ActorObjectSerializer();

  private static final AtomicInteger ACTOR_ID_COUNT = new AtomicInteger();

  private final ActorRuntimeContext context = createContext();

  private ActorManager<ActorChild> manager = new ActorManager<>(context);

  public interface MyActor {

    // These 4 will be implemented in the user code class that extends AbstractActor, but it
    // will not be implemented in another class that will inherit that.
    Mono<String> onlyImplementedInParentStringInStringOut(String input);
    Mono<Boolean> onlyImplementedInParentStringInBooleanOut(String input);
    Mono<Void> onlyImplementedInParentStringInVoidOut(String input);
    Mono<MyData> onlyImplementedInParentClassInClassOut(MyData input);

    // used to validate onlyImplementedInParentStringInVoidOut() was called
    boolean methodReturningVoidInvoked();

    // The test will only call the versions of this in a derived class to the user code base class.
    // The user code base class version will throw.
    Mono<String> stringInStringOut(String input);
    Mono<Boolean> stringInBooleanOut(String input);
    Mono<Void> stringInVoidOut(String input);
    Mono<Void> stringInVoidOutIntentionallyThrows(String input);
    Mono<MyData> classInClassOut(MyData input);
  }

  @ActorType(name = "MyActor")
  public static class ActorParent extends AbstractActor implements MyActor {
    private final ActorId id;
    private boolean activated;
    private boolean methodReturningVoidInvoked;

    public ActorParent(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
      this.id = id;
      this.activated = true;
      this.methodReturningVoidInvoked = false;
    }

    @Override
    public Mono<String> onlyImplementedInParentStringInStringOut(String input) {
      return Mono.fromSupplier(() -> {
        return input + input + input;
      });
    }

    @Override
    public Mono<Boolean> onlyImplementedInParentStringInBooleanOut(String input) {
      return Mono.fromSupplier(() -> {
        if (input.equals("icecream")) {
          return true;
        } else {
          return false;
        }
      });
    }

    @Override
    public Mono<Void> onlyImplementedInParentStringInVoidOut(String input) {
      return Mono.fromRunnable(() -> {
        this.methodReturningVoidInvoked = true;
        System.out.println("Received " + input);
      });
    }

    @Override
    public Mono<MyData> onlyImplementedInParentClassInClassOut(MyData input) {
      return Mono.fromSupplier(() -> {
        return new MyData(
          input.getName() + input.getName() + input.getName(),
          input.getNum() + input.getNum() + input.getNum());
      });
    }

    @Override
    public boolean methodReturningVoidInvoked() {
      return this.methodReturningVoidInvoked;
    }

    @Override
    public Mono<String> stringInStringOut(String s) {
      return Mono.fromSupplier(() -> {
          // In the cases below we intentionally only call the derived version of this.
          // ArithmeticException is being thrown only because it's un unusual exception so it's unlikely
          // to collide with something else.
          throw new ArithmeticException("This method should not have been called");
        }
      );
    }

    @Override
    public Mono<Boolean> stringInBooleanOut(String s) {
      return Mono.fromSupplier(() -> {
        // In the cases below we intentionally only call the derived version of this.
        // ArithmeticException is being thrown only because it's un unusual exception so it's unlikely
        // to collide with something else.
        throw new ArithmeticException("This method should not have been called");
      });
    }

    @Override
    public Mono<Void> stringInVoidOut(String input) {
      return Mono.fromRunnable(() -> {
        this.methodReturningVoidInvoked = true;
        System.out.println("Received " + input);
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
        // In the cases below we intentionally only call the derived version of this.
        // ArithmeticException is being thrown only because it's un unusual exception so it's unlikely
        // to collide with something else.
        throw new ArithmeticException("This method should not have been called");
      });
    }
  }

  public static class ActorChild extends ActorParent implements MyActor {
    private final ActorId id;
    private boolean activated;

    public ActorChild(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
      this.id = id;
      this.activated = true;
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
    public Mono<MyData> classInClassOut(MyData input) {
      return Mono.fromSupplier(() -> {
        return new MyData(
          input.getName() + input.getName(),
          input.getNum() + input.getNum());
      });
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
  public void stringInStringOut() {
    ActorProxy proxy = createActorProxyForActorChild();

    // these should only call the actor methods for ActorChild.  The implementations in ActorParent will throw.
    Assert.assertEquals(
      "abcabc",
      proxy.invokeMethod("stringInStringOut", "abc", String.class).block());
  }

  @Test
  public void stringInBooleanOut() {
    ActorProxy proxy = createActorProxyForActorChild();

    // these should only call the actor methods for ActorChild.  The implementations in ActorParent will throw.
    Assert.assertEquals(
      false,
      proxy.invokeMethod("stringInBooleanOut", "hello world", Boolean.class).block());

    Assert.assertEquals(
      true,
      proxy.invokeMethod("stringInBooleanOut", "true", Boolean.class).block());
  }

  @Test
  public void stringInVoidOut() {
    ActorProxy actorProxy = createActorProxyForActorChild();

    // stringInVoidOut() has not been invoked so this is false
    Assert.assertEquals(
      false,
      actorProxy.invokeMethod("methodReturningVoidInvoked", Boolean.class).block());

    // these should only call the actor methods for ActorChild.  The implementations in ActorParent will throw.
    actorProxy.invokeMethod("stringInVoidOut", "hello world").block();

    Assert.assertEquals(
      true,
      actorProxy.invokeMethod("methodReturningVoidInvoked", Boolean.class).block());
  }

  @Test(expected = IllegalMonitorStateException.class)
  public void stringInVoidOutIntentionallyThrows() {
    ActorProxy actorProxy = createActorProxyForActorChild();

    // these should only call the actor methods for ActorChild.  The implementations in ActorParent will throw.
    actorProxy.invokeMethod("stringInVoidOutIntentionallyThrows", "hello world").block();
  }

  @Test
  public void classInClassOut() {
    ActorProxy actorProxy = createActorProxyForActorChild();
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

  // The actor methods this test invokes are all implemented in ActorParent only.  We're asserting it's callable when the actor proxy is for an ActorChild.
  @Test
  public void testInheritedActorMethods() {
    ActorProxy actorProxy = createActorProxyForActorChild();

    Assert.assertEquals(
      "www",
      actorProxy.invokeMethod("onlyImplementedInParentStringInStringOut", "w", String.class).block());

    Assert.assertEquals(
      true,
      actorProxy.invokeMethod("onlyImplementedInParentStringInBooleanOut", "icecream", Boolean.class).block());

    // onlyImplementedInParentStringInVoidOut() has not been invoked so this is false
    Assert.assertEquals(
      false,
      actorProxy.invokeMethod("methodReturningVoidInvoked", Boolean.class).block());

    actorProxy.invokeMethod("onlyImplementedInParentStringInVoidOut", "icecream", Boolean.class).block();

    // now it should return true.
    Assert.assertEquals(
      true,
      actorProxy.invokeMethod("methodReturningVoidInvoked", Boolean.class).block());

    MyData d = new MyData("hi", 3);
    MyData response = actorProxy.invokeMethod("onlyImplementedInParentClassInClassOut", d, MyData.class).block();

    Assert.assertEquals(
      "hihihi",
      response.getName());
    Assert.assertEquals(
      9,
      response.getNum());
  }

  private static ActorId newActorId() {
    return new ActorId(Integer.toString(ACTOR_ID_COUNT.incrementAndGet()));
  }

  private ActorProxy createActorProxyForActorChild() {
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
      ActorTypeInformation.create(ActorChild.class),
      daprClient,
      mock(DaprStateAsyncProvider.class)
    );
  }
}
