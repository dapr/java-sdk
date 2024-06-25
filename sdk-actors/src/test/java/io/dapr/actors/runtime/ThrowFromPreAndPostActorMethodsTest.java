/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyImplForTests;
import io.dapr.actors.client.DaprClientStub;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ThrowFromPreAndPostActorMethodsTest {

  private static final ActorObjectSerializer INTERNAL_SERIALIZER = new ActorObjectSerializer();

  private static final AtomicInteger ACTOR_ID_COUNT = new AtomicInteger();

  private final ActorRuntimeContext context = createContext();

  private ActorManager<ActorChild> manager = new ActorManager<>(context);

  public interface MyActor {
    Mono<Boolean> stringInBooleanOut(String input);
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
    public Mono<Void> onPreActorMethodInternal(ActorMethodContext actorMethodContext) {
      // IllegalMonitorStateException is being thrown only because it's un unusual exception so it's unlikely
      // to collide with something else.
      throw new IllegalMonitorStateException("Intentional throw from onPreActorMethodInternal");
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
    public Mono<Boolean> stringInBooleanOut(String s) {
      return Mono.fromSupplier(() -> {
        if (s.equals("true")) {
          return true;
        } else {
          return false;
        }
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

  // IllegalMonitorStateException should be intentionally thrown.  This type was chosen for this test just because
  // it is unlikely to collide.
  @Test
  public void stringInBooleanOut1() {
    ActorProxy proxy = createActorProxyForActorChild();

    // these should only call the actor methods for ActorChild.  The implementations in ActorParent will throw.
    assertThrows(IllegalMonitorStateException.class, () ->
      proxy.invokeMethod("stringInBooleanOut", "hello world", Boolean.class).block());
  }

  // IllegalMonitorStateException should be intentionally thrown.  This type was chosen for this test just because
  // it is unlikely to collide.
  @Test
  public void stringInBooleanOut2() {
    ActorProxy proxy = createActorProxyForActorChild();

    // these should only call the actor methods for ActorChild.  The implementations in ActorParent will throw.
    assertThrows(IllegalMonitorStateException.class, () ->
      proxy.invokeMethod("stringInBooleanOut", "true", Boolean.class).block());
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
