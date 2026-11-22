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
import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.IllegalCharsetNameException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActorStatefulTest {

  private static final ActorObjectSerializer INTERNAL_SERIALIZER = new ActorObjectSerializer();

  private static final AtomicInteger ACTOR_ID_COUNT = new AtomicInteger();

  private static final Collection<String> DEACTIVATED_ACTOR_IDS = Collections.synchronizedList(new ArrayList<>());

  private final ActorRuntimeContext context = createContext();

  private ActorManager<MyActorImpl> manager = new ActorManager<>(context);

  public interface MyActor {
    Mono<Boolean> isActive();

    MyMethodContext getPreCallMethodContext();

    MyMethodContext getPostCallMethodContext();

    Mono<Void> unregisterTimerAndReminder();

    Mono<Integer> incrementAndGetCount(int increment) throws Exception;

    Mono<Integer> getCountButThrowsException();

    Mono<Void> addMessage(String message);

    Mono<String> setMessage(String message);

    Mono<Boolean> setMessageFor1s(String message);

    Mono<Boolean> setMessageAndWait(String message);

    Mono<String> getMessage();

    Mono<Boolean> hasMessage();

    Mono<Void> deleteMessage();

    Mono<Void> forceDuplicateException();

    Mono<Void> forcePartialChange();

    Mono<Void> throwsWithoutSaving();

    Mono<Void> setMethodContext(MyMethodContext context);

    Mono<MyMethodContext> getMethodContext();

    String getIdString();
  }

  @ActorType(name = "MyActor")
  public static class MyActorImpl extends AbstractActor implements MyActor, Remindable<String> {

    private final ActorId id;

    private boolean activated;

    private MyMethodContext preMethodCalled;

    private MyMethodContext postMethodCalled;

    public MyActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
      this.id = id;
      this.activated = true;
    }

    @Override
    public Mono<Boolean> isActive() {
      return Mono.fromSupplier(() -> this.activated);
    }

    @Override
    public Mono<Void> onActivate() {
      return Mono
        .fromRunnable(() -> this.activated = true)
        .then(super.registerActorTimer(
          "mytimer",
          "hasMessage",
          null,
          Duration.ofSeconds(1),
          Duration.ofSeconds(1)))
        .then(super.registerReminder(
          "myreminder",
          null,
          Duration.ofSeconds(1),
          Duration.ofSeconds(1)
        ));
    }

    @Override
    public Mono<Void> onDeactivate() {
      return Mono.fromRunnable(() -> DEACTIVATED_ACTOR_IDS.add(this.id.toString()));
    }

    @Override
    public Mono<Void> onPreActorMethod(ActorMethodContext context) {
      // Only keep the first one to make sure we can validate it via another method invocation.
      return Mono.fromRunnable(() -> {
        this.preMethodCalled = this.preMethodCalled != null ? this.preMethodCalled : new MyMethodContext()
          .setName(context.getMethodName())
          .setType(context.getCallType().toString());
      });
    }

    @Override
    public Mono<Void> onPostActorMethod(ActorMethodContext context) {
      // Only keep the first one to make sure we can validate it via another method invocation.
      return Mono.fromRunnable(() -> {
        this.postMethodCalled = this.postMethodCalled != null ? this.postMethodCalled : new MyMethodContext()
          .setName(context.getMethodName())
          .setType(context.getCallType().toString());
      });
    }

    @Override
    public MyMethodContext getPreCallMethodContext() {
      return this.preMethodCalled;
    }

    @Override
    public MyMethodContext getPostCallMethodContext() {
      return this.postMethodCalled;
    }

    @Override
    public Mono<Void> unregisterTimerAndReminder() {
      return super.unregisterReminder("UnknownReminder")
        .then(super.unregisterTimer("UnknownTimer"))
        .then(super.unregisterReminder("myreminder"))
        .then(super.unregisterTimer("mytimer"));
    }

    @Override
    public Mono<Integer> incrementAndGetCount(int increment) {
      return Mono.fromRunnable(() -> {
        if (increment == 0) {
          // Artificial exception case for testing.
          throw new NumberFormatException("increment cannot be zero.");
        }
      })
      .then(super.getActorStateManager().contains("counter"))
        .flatMap(contains -> {
          if (!contains) {
            return Mono.just(0);
          }

          return super.getActorStateManager().get("counter", int.class);
        })
        .map(count -> count + increment)
        .flatMap(count -> super.getActorStateManager().set("counter", count).thenReturn(count));
    }

    @Override
    public Mono<Integer> getCountButThrowsException() {
      return super.getActorStateManager().get("counter_WRONG_NAME", int.class);
    }

    @Override
    public Mono<Void> addMessage(String message) {
      return super.getActorStateManager().add("message", message, null);
    }

    @Override
    public Mono<String> setMessage(String message) {
      return super.getActorStateManager().set("message", message).thenReturn(executeSayMethod(message));
    }

    @Override
    public Mono<Boolean> setMessageFor1s(String message) {
      return super
          .getActorStateManager().set("message", message, Duration.ofSeconds(1))
          .then(super.getActorStateManager().contains("message"));
    }

    @Override
    public Mono<Boolean> setMessageAndWait(String message) {
      return super.getActorStateManager().set("message", message, Duration.ofSeconds(1))
          .then(Mono.delay(Duration.ofMillis(1100)))
          .then(super.getActorStateManager().contains("message"));
    }

    @Override
    public Mono<String> getMessage() {
      return super.getActorStateManager().get("message", String.class);
    }

    @Override
    public Mono<Boolean> hasMessage() {
      return super.getActorStateManager().contains("message");
    }

    @Override
    public Mono<Void> deleteMessage() {
      return super.getActorStateManager().remove("message");
    }

    @Override
    public Mono<Void> forceDuplicateException() {
      // Second add should throw exception.
      return super.getActorStateManager().add("message", "anything", null)
        .then(super.getActorStateManager().add("message", "something else", null));
    }

    @Override
    public Mono<Void> forcePartialChange() {
      return super.getActorStateManager().add("message", "first message", null)
        .then(super.saveState())
        .then(super.getActorStateManager().add("message", "second message", null));
    }

    @Override
    public Mono<Void> throwsWithoutSaving() {
      return super.getActorStateManager().add("message", "first message", null)
        .then(Mono.error(new IllegalCharsetNameException("random")));
    }

    @Override
    public Mono<Void> setMethodContext(MyMethodContext context) {
      return super.getActorStateManager().set("context", context);
    }

    @Override
    public Mono<MyMethodContext> getMethodContext() {
      return super.getActorStateManager().get("context", MyMethodContext.class);
    }

    // Blocking methods are also supported for Actors. Mono is not required.
    @Override
    public String getIdString() {
      return this.id.toString();
    }

    @Override
    public TypeRef<String> getStateType() {
      // Remindable type.
      return TypeRef.STRING;
    }

    @Override
    public Mono<Void> receiveReminder(String reminderName, String state, Duration dueTime, Duration period) {
      return Mono.empty();
    }
  }

  // Class used to validate serialization/deserialization
  public static class MyMethodContext implements Serializable {

    private String type;

    private String name;

    public String getType() {
      return type;
    }

    public MyMethodContext setType(String type) {
      this.type = type;
      return this;
    }

    public String getName() {
      return name;
    }

    public MyMethodContext setName(String name) {
      this.name = name;
      return this;
    }
  }

  @Test
  public void happyGetSetDeleteContains() {
    ActorProxy proxy = newActorProxy();
    Assertions.assertEquals(
        proxy.getActorId().toString(), proxy.invokeMethod("getIdString", String.class).block());
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    proxy.invokeMethod("setMessage", "hello world").block();
    Assertions.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());

    Assertions.assertEquals(
        "hello world", proxy.invokeMethod("getMessage", String.class).block());

    Assertions.assertEquals(
        executeSayMethod("hello world"),
        proxy.invokeMethod("setMessage", "hello world", String.class).block());

    proxy.invokeMethod("deleteMessage").block();
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void actorStateTTL() throws Exception {
    ActorProxy proxy = newActorProxy();
    Assertions.assertEquals(
        proxy.getActorId().toString(), proxy.invokeMethod("getIdString", String.class).block());
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    Assertions.assertTrue(
        proxy.invokeMethod("setMessageFor1s", "hello world expires in 1s", Boolean.class).block());
    Assertions.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());

    Assertions.assertEquals(
        "hello world expires in 1s", proxy.invokeMethod("getMessage", String.class).block());

    Assertions.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());

    Thread.sleep(1100);

    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void actorStateTTLExpiresInLocalCache() throws Exception {
    ActorProxy proxy = newActorProxy();
    Assertions.assertEquals(
        proxy.getActorId().toString(), proxy.invokeMethod("getIdString", String.class).block());
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    //First, sets a message without TTL and checks it is saved.
    proxy.invokeMethod("setMessage", "hello world").block();
    Assertions.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());
    Assertions.assertEquals(
        "hello world", proxy.invokeMethod("getMessage", String.class).block());

    // Now, sets a message that expires still in local cache, before it is sent to state store.
    Assertions.assertFalse(
        proxy.invokeMethod("setMessageAndWait", "expires while still in cache", Boolean.class).block());
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    Thread.sleep(1100);

    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void lazyGet() {
    ActorProxy proxy = newActorProxy();
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());
    proxy.invokeMethod("setMessage", "first message").block();

    // Creates the mono plan but does not call it yet.
    Mono<String> getMessageCall = proxy.invokeMethod("getMessage", String.class);

    proxy.invokeMethod("deleteMessage").block();

    // Call should fail because the message was deleted.
    assertThrows(IllegalStateException.class, () -> getMessageCall.block());
  }

  @Test
  public void lazySet() {
    ActorProxy proxy = newActorProxy();
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // Creates the mono plan but does not call it yet.
    Mono<Void> setMessageCall = proxy.invokeMethod("setMessage", "first message");

    // No call executed yet, so message should not be set.
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    setMessageCall.block();

    // Now the message has been set.
    Assertions.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void lazyContains() {
    ActorProxy proxy = newActorProxy();
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // Creates the mono plan but does not call it yet.
    Mono<Boolean> hasMessageCall = proxy.invokeMethod("hasMessage", Boolean.class);

    // Sets the message.
    proxy.invokeMethod("setMessage", "hello world").block();

    // Now we check if message is set.
    hasMessageCall.block();

    // Now the message should be set.
    Assertions.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void lazyDelete() {
    ActorProxy proxy = newActorProxy();
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    proxy.invokeMethod("setMessage", "first message").block();

    // Message is set.
    Assertions.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // Created the mono plan but does not execute it yet.
    Mono<Void> deleteMessageCall = proxy.invokeMethod("deleteMessage");

    // Message is still set.
    Assertions.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());

    deleteMessageCall.block();

    // Now message is not set.
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void lazyAdd() {
    ActorProxy proxy = newActorProxy();
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    proxy.invokeMethod("setMessage", "first message").block();

    // Message is set.
    Assertions.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // Created the mono plan but does not execute it yet.
    Mono<Void> addMessageCall = proxy.invokeMethod("addMessage", "second message");

    // Message is still set.
    Assertions.assertEquals("first message",
      proxy.invokeMethod("getMessage", String.class).block());

    // Delete message
    proxy.invokeMethod("deleteMessage").block();

    // Should work since previous message was deleted.
    addMessageCall.block();

    // New message is still set.
    Assertions.assertEquals("second message",
      proxy.invokeMethod("getMessage", String.class).block());
  }

  @Test
  public void onActivateAndOnDeactivate() {
    ActorProxy proxy = newActorProxy();

    Assertions.assertTrue(proxy.invokeMethod("isActive", Boolean.class).block());
    Assertions.assertFalse(DEACTIVATED_ACTOR_IDS.contains(proxy.getActorId().toString()));

    proxy.invokeMethod("hasMessage", Boolean.class).block();

    this.manager.deactivateActor(proxy.getActorId()).block();

    Assertions.assertTrue(DEACTIVATED_ACTOR_IDS.contains(proxy.getActorId().toString()));
  }

  @Test
  public void onPreMethodAndOnPostMethod() {
    ActorProxy proxy = newActorProxy();

    proxy.invokeMethod("hasMessage", Boolean.class).block();

    MyMethodContext preContext =
      proxy.invokeMethod("getPreCallMethodContext", MyMethodContext.class).block();
    Assertions.assertEquals("hasMessage", preContext.getName());
    Assertions.assertEquals(ActorCallType.ACTOR_INTERFACE_METHOD.toString(), preContext.getType());

    MyMethodContext postContext =
      proxy.invokeMethod("getPostCallMethodContext", MyMethodContext.class).block();
    Assertions.assertEquals("hasMessage", postContext.getName());
    Assertions.assertEquals(ActorCallType.ACTOR_INTERFACE_METHOD.toString(), postContext.getType());
  }

  @Test
  public void invokeTimer() {
    ActorProxy proxy = newActorProxy();

    this.manager.invokeTimer(proxy.getActorId(), "mytimer", "{ \"callback\": \"hasMessage\" }".getBytes()).block();

    MyMethodContext preContext =
      proxy.invokeMethod("getPreCallMethodContext", MyMethodContext.class).block();
    Assertions.assertEquals("mytimer", preContext.getName());
    Assertions.assertEquals(ActorCallType.TIMER_METHOD.toString(), preContext.getType());

    MyMethodContext postContext =
      proxy.invokeMethod("getPostCallMethodContext", MyMethodContext.class).block();
    Assertions.assertEquals("mytimer", postContext.getName());
    Assertions.assertEquals(ActorCallType.TIMER_METHOD.toString(), postContext.getType());
  }

  @Test
  public void invokeTimerAfterDeactivate() {
    ActorProxy proxy = newActorProxy();

    this.manager.deactivateActor(proxy.getActorId()).block();

    assertThrows(IllegalArgumentException.class, () ->
    this.manager.invokeTimer(proxy.getActorId(), "mytimer", "{ \"callback\": \"hasMessage\" }".getBytes()).block());
  }

  @Test
  public void invokeTimerAfterUnregister() {
    ActorProxy proxy = newActorProxy();

    proxy.invokeMethod("unregisterTimerAndReminder").block();

    // This call succeeds because the SDK does not control register/unregister timer, the Dapr runtime does.
    this.manager.invokeTimer(proxy.getActorId(), "mytimer", "{ \"callback\": \"hasMessage\" }".getBytes()).block();
  }

  @Test
  public void invokeUnknownTimer() {
    ActorProxy proxy = newActorProxy();

    // SDK does not control timers, Dapr runtime does - so an "unknown" timer can still be triggered.
    this.manager.invokeTimer(proxy.getActorId(), "unknown", "{ \"callback\": \"hasMessage\" }".getBytes()).block();
  }

  @Test
  public void invokeReminder() throws Exception {
    ActorProxy proxy = newActorProxy();

    byte[] params = createReminderParams("anything");

    this.manager.invokeReminder(proxy.getActorId(), "myreminder", params).block();

    MyMethodContext preContext =
      proxy.invokeMethod("getPreCallMethodContext", MyMethodContext.class).block();
    Assertions.assertEquals("myreminder", preContext.getName());
    Assertions.assertEquals(ActorCallType.REMINDER_METHOD.toString(), preContext.getType());

    MyMethodContext postContext =
      proxy.invokeMethod("getPostCallMethodContext", MyMethodContext.class).block();
    Assertions.assertEquals("myreminder", postContext.getName());
    Assertions.assertEquals(ActorCallType.REMINDER_METHOD.toString(), postContext.getType());
  }

  @Test
  public void invokeReminderAfterDeactivate() throws Exception {
    ActorProxy proxy = newActorProxy();

    this.manager.deactivateActor(proxy.getActorId()).block();

    byte[] params = createReminderParams("anything");

    assertThrows(IllegalArgumentException.class, () -> this.manager.invokeReminder(proxy.getActorId(), "myreminder", params).block());
  }

  @Test
  public void classTypeRequestResponseInStateStore() {
    ActorProxy proxy = newActorProxy();

    MyMethodContext expectedContext = new MyMethodContext().setName("MyName").setType("MyType");

    proxy.invokeMethod("setMethodContext", expectedContext).block();
    MyMethodContext context = proxy.invokeMethod("getMethodContext", MyMethodContext.class).block();

    Assertions.assertEquals(expectedContext.getName(), context.getName());
    Assertions.assertEquals(expectedContext.getType(), context.getType());
  }

  @Test
  public void intTypeRequestResponseInStateStore() {
    ActorProxy proxy = newActorProxy();

    Assertions.assertEquals(1, (int)proxy.invokeMethod("incrementAndGetCount", 1, int.class).block());
    Assertions.assertEquals(6, (int)proxy.invokeMethod("incrementAndGetCount", 5, int.class).block());
  }

  @Test
  public void intTypeWithMethodException() {
    ActorProxy proxy = newActorProxy();

    // Zero is a magic input that will make method throw an exception.
    assertThrows(NumberFormatException.class, () -> proxy.invokeMethod("incrementAndGetCount", 0, int.class).block());
  }

  @Test
  public void intTypeWithRuntimeException() {
    ActorProxy proxy = newActorProxy();

    assertThrows(RuntimeException.class, () ->
      proxy.invokeMethod("getCountButThrowsException", int.class).block());
  }

  @Test
  public void actorRuntimeException() {
    ActorProxy proxy = newActorProxy();

    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    assertThrows(RuntimeException.class, () ->
      proxy.invokeMethod("forceDuplicateException").block());
  }

  @Test
  public void actorMethodException() {
    ActorProxy proxy = newActorProxy();

    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    assertThrows(IllegalCharsetNameException.class, () -> proxy.invokeMethod("throwsWithoutSaving").block());

    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void rollbackChanges() {
    ActorProxy proxy = newActorProxy();

    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // Runs a method that will add one message but fail because tries to add a second one.
    proxy.invokeMethod("forceDuplicateException")
      .onErrorResume(throwable -> Mono.empty())
      .block();

    // No message is set
    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void partialChanges() {
    ActorProxy proxy = newActorProxy();

    Assertions.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // Runs a method that will add one message, commit but fail because tries to add a second one.
    proxy.invokeMethod("forcePartialChange")
      .onErrorResume(throwable -> Mono.empty())
      .block();

    // Message is set.
    Assertions.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // It is first message and not the second due to a save() in the middle but an exception in the end.
    Assertions.assertEquals("first message",
      proxy.invokeMethod("getMessage", String.class).block());
  }

  private ActorProxy newActorProxy() {
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

  private byte[] createReminderParams(String data) throws IOException {
    byte[] serialized = this.context.getObjectSerializer().serialize(data);
    ActorReminderParams params = new ActorReminderParams(serialized, Duration.ofSeconds(1), Duration.ofSeconds(1));
    return INTERNAL_SERIALIZER.serialize(params);
  }

  private static ActorId newActorId() {
    return new ActorId(Integer.toString(ACTOR_ID_COUNT.incrementAndGet()));
  }

  private static String executeSayMethod(String something) {
    return "Said: " + (something == null ? "" : something);
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
      ActorTypeInformation.create(MyActorImpl.class),
      daprClient,
      new DaprInMemoryStateProvider(new JavaSerializer())
    );
  }
}
