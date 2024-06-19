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
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Actor Manager
 */
public class ActorManagerTest {

  private static final ActorObjectSerializer INTERNAL_SERIALIZER = new ActorObjectSerializer();

  private static final AtomicInteger ACTOR_ID_COUNT = new AtomicInteger();

  interface MyActor {
    String say(String something);

    int getCount();

    void incrementCount(int delta);

    void throwsException();

    Mono<Void> throwsExceptionHotMono();

    Mono<Void> throwsExceptionMono();
  }

  public static class NotRemindableActor extends AbstractActor {
    public NotRemindableActor(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
    }
  }

  @ActorType(name = "MyActor")
  public static class MyActorImpl extends AbstractActor implements MyActor, Remindable<String> {

    private int timeCount = 0;

    @Override
    public String say(String something) {
      return executeSayMethod(something);
    }

    @Override
    public int getCount() {
      return this.timeCount;
    }

    @Override
    public void incrementCount(int delta) {
      this.timeCount = timeCount + delta;
    }

    @Override
    public void throwsException() {
      throw new IllegalArgumentException();
    }

    @Override
    public Mono<Void> throwsExceptionHotMono() {
      throw new IllegalArgumentException();
    }

    @Override
    public Mono<Void> throwsExceptionMono() {
      return Mono.error(new IllegalArgumentException());
    }

    public MyActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
      super.registerActorTimer(
        "count",
        "incrementCount",
        2,
        Duration.ofSeconds(1),
        Duration.ofSeconds(1)
      ).block();
    }

    @Override
    public TypeRef<String> getStateType() {
      return TypeRef.STRING;
    }

    @Override
    public Mono<Void> receiveReminder(String reminderName, String state, Duration dueTime, Duration period) {
      return Mono.empty();
    }
  }

  private ActorRuntimeContext<MyActorImpl> context = createContext(MyActorImpl.class);

  private ActorManager<MyActorImpl> manager = new ActorManager<>(context);

  @Test
  public void invokeBeforeActivate() throws Exception {
    ActorId actorId = newActorId();
    String message = "something";

    assertThrows(IllegalArgumentException.class, () ->
    this.manager.invokeMethod(actorId, "say", message.getBytes()).block());
  }

  @Test
  public void activateThenInvoke() throws Exception {
    ActorId actorId = newActorId();
    byte[] message = this.context.getObjectSerializer().serialize("something");
    this.manager.activateActor(actorId).block();
    byte[] response = this.manager.invokeMethod(actorId, "say", message).block();
    Assertions.assertEquals(executeSayMethod(
      this.context.getObjectSerializer().deserialize(message, TypeRef.STRING)),
      this.context.getObjectSerializer().deserialize(response, TypeRef.STRING));
  }

  @Test
  public void activateThenInvokeWithActorImplException() throws Exception {
    ActorId actorId = newActorId();
    this.manager.activateActor(actorId).block();

    assertThrows(RuntimeException.class, () -> {
      this.manager.invokeMethod(actorId, "throwsException", null).block();
    });
  }

  @Test
  public void activateThenInvokeWithActorImplExceptionButNotSubscribed() throws Exception {
    ActorId actorId = newActorId();
    this.manager.activateActor(actorId).block();

    // Nothing happens because we don't call block().
    this.manager.invokeMethod(actorId, "throwsException", null);
  }

  @Test
  public void activateThenInvokeWithActorImplHotMonoException() throws Exception {
    ActorId actorId = newActorId();
    this.manager.activateActor(actorId).block();

    assertThrows(RuntimeException.class, () -> {
      this.manager.invokeMethod(actorId, "throwsExceptionHotMono", null).block();
    });
  }

  @Test
  public void activateThenInvokeWithActorImplHotMonoExceptionNotSubscribed() throws Exception {
    ActorId actorId = newActorId();
    this.manager.activateActor(actorId).block();

    // Nothing happens because we don't call block().
    this.manager.invokeMethod(actorId, "throwsExceptionHotMono", null);
  }

  @Test
  public void activateThenInvokeWithActorImplMonoException() throws Exception {
    ActorId actorId = newActorId();
    this.manager.activateActor(actorId).block();

    assertThrows(RuntimeException.class, () -> {
      this.manager.invokeMethod(actorId, "throwsExceptionMono", null).block();
    });
  }

  @Test
  public void activateThenInvokeWithActorImplMonoExceptionNotSubscribed() throws Exception {
    ActorId actorId = newActorId();
    this.manager.activateActor(actorId).block();

    // Nothing happens because we don't call block().
    this.manager.invokeMethod(actorId, "throwsExceptionMono", null);
  }

  @Test
  public void activateInvokeDeactivateThenInvoke() throws Exception {
    ActorId actorId = newActorId();
    byte[] message = this.context.getObjectSerializer().serialize("something");
    this.manager.activateActor(actorId).block();
    byte[] response = this.manager.invokeMethod(actorId, "say", message).block();
    Assertions.assertEquals(executeSayMethod(
      this.context.getObjectSerializer().deserialize(message, TypeRef.STRING)),
      this.context.getObjectSerializer().deserialize(response, TypeRef.STRING));

    this.manager.deactivateActor(actorId).block();
    assertThrows(IllegalArgumentException.class, () ->
      this.manager.invokeMethod(actorId, "say", message).block());
  }

  @Test
  public void invokeReminderNotRemindable() throws Exception {
    ActorId actorId = newActorId();
    ActorRuntimeContext<NotRemindableActor> context = createContext(NotRemindableActor.class);
    ActorManager<NotRemindableActor> manager = new ActorManager<>(context);
    manager.invokeReminder(actorId, "myremind", createReminderParams("hello")).block();
  }

  @Test
  public void invokeReminderBeforeActivate() throws Exception {
    ActorId actorId = newActorId();
    assertThrows(IllegalArgumentException.class, () ->
      this.manager.invokeReminder(actorId, "myremind", createReminderParams("hello")).block());
  }

  @Test
  public void activateThenInvokeReminder() throws Exception {
    ActorId actorId = newActorId();
    this.manager.activateActor(actorId).block();
    this.manager.invokeReminder(actorId, "myremind", createReminderParams("hello")).block();
  }

  @Test
  public void activateDeactivateThenInvokeReminder() throws Exception {
    ActorId actorId = newActorId();
    this.manager.activateActor(actorId).block();
    this.manager.deactivateActor(actorId).block();;

    assertThrows(IllegalArgumentException.class, () -> this.manager.invokeReminder(actorId, "myremind", createReminderParams("hello")).block());
  }

  @Test
  public void invokeTimerBeforeActivate() throws IOException {
    ActorId actorId = newActorId();

    assertThrows(IllegalArgumentException.class, () ->
    this.manager.invokeTimer(actorId, "count", createTimerParams("incrementCount", 2)).block());
  }

  @Test
  public void activateThenInvokeTimerBeforeRegister() throws IOException {
    ActorId actorId = newActorId();
    this.manager.activateActor(actorId).block();
    this.manager.invokeTimer(actorId, "unknown", createTimerParams("incrementCount", 2)).block();
  }

  @Test
  public void activateThenInvokeTimer() throws IOException {
    ActorId actorId = newActorId();
    this.manager.activateActor(actorId).block();
    this.manager.invokeTimer(actorId, "count", createTimerParams("incrementCount", 2)).block();
    byte[] response = this.manager.invokeMethod(actorId, "getCount", null).block();
    Assertions.assertEquals("2", new String(response));
  }

  @Test
  public void activateInvokeTimerDeactivateThenInvokeTimer() throws IOException {
    ActorId actorId = newActorId();
    this.manager.activateActor(actorId).block();
    this.manager.invokeTimer(actorId, "count", createTimerParams("incrementCount", 2)).block();
    byte[] response = this.manager.invokeMethod(actorId, "getCount", null).block();
    Assertions.assertEquals("2", new String(response));

    this.manager.deactivateActor(actorId).block();
    assertThrows(IllegalArgumentException.class, () -> this.manager.invokeTimer(actorId, "count", createTimerParams("incrementCount", 2)).block());
  }

  private byte[] createReminderParams(String data) throws IOException {
    byte[] serializedData = this.context.getObjectSerializer().serialize(data);
    ActorReminderParams p = new ActorReminderParams(serializedData, Duration.ofSeconds(1), Duration.ofSeconds(1));
    return INTERNAL_SERIALIZER.serialize(p);
  }

  private byte[] createTimerParams(String callback, Object data) throws IOException {
    byte[] serializedData = this.context.getObjectSerializer().serialize(data);
    ActorTimerParams p = new ActorTimerParams(callback, serializedData, Duration.ofSeconds(1), Duration.ofSeconds(1));
    return INTERNAL_SERIALIZER.serialize(p);
  }

  private static ActorId newActorId() {
    return new ActorId(Integer.toString(ACTOR_ID_COUNT.incrementAndGet()));
  }

  private static String executeSayMethod(String something) {
    return "Said: " + (something == null ? "" : something);
  }

  private static <T extends AbstractActor> ActorRuntimeContext createContext(Class<T> clazz) {
    DaprClient daprClient = mock(DaprClient.class);

    when(daprClient.registerTimer(any(), any(), any(), any())).thenReturn(Mono.empty());
    when(daprClient.registerReminder(any(), any(), any(), any())).thenReturn(Mono.empty());
    when(daprClient.unregisterTimer(any(), any(), any())).thenReturn(Mono.empty());
    when(daprClient.unregisterReminder(any(), any(), any())).thenReturn(Mono.empty());

    return new ActorRuntimeContext(
      mock(ActorRuntime.class),
      new DefaultObjectSerializer(),
      new DefaultActorFactory<T>(),
      ActorTypeInformation.create(clazz),
      daprClient,
      mock(DaprStateAsyncProvider.class)
    );
  }
}
