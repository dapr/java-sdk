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
import io.dapr.utils.TypeRef;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.IllegalCharsetNameException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

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
      return super.getActorStateManager().add("message", message);
    }

    @Override
    public Mono<String> setMessage(String message) {
      return super.getActorStateManager().set("message", message).thenReturn(executeSayMethod(message));
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
      return super.getActorStateManager().add("message", "anything")
        .then(super.getActorStateManager().add("message", "something else"));
    }

    @Override
    public Mono<Void> forcePartialChange() {
      return super.getActorStateManager().add("message", "first message")
        .then(super.saveState())
        .then(super.getActorStateManager().add("message", "second message"));
    }

    @Override
    public Mono<Void> throwsWithoutSaving() {
      return super.getActorStateManager().add("message", "first message")
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
    Assert.assertEquals(
      proxy.getActorId().toString(), proxy.invokeMethod("getIdString", String.class).block());
    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    proxy.invokeMethod("setMessage", "hello world").block();
    Assert.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());

    Assert.assertEquals(
      "hello world", proxy.invokeMethod("getMessage", String.class).block());

    Assert.assertEquals(
      executeSayMethod("hello world"),
      proxy.invokeMethod("setMessage", "hello world", String.class).block());

    proxy.invokeMethod("deleteMessage").block();
    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test(expected = IllegalStateException.class)
  public void lazyGet() {
    ActorProxy proxy = newActorProxy();
    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());
    proxy.invokeMethod("setMessage", "first message").block();

    // Creates the mono plan but does not call it yet.
    Mono<String> getMessageCall = proxy.invokeMethod("getMessage", String.class);

    proxy.invokeMethod("deleteMessage").block();

    // Call should fail because the message was deleted.
    getMessageCall.block();
  }

  @Test
  public void lazySet() {
    ActorProxy proxy = newActorProxy();
    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // Creates the mono plan but does not call it yet.
    Mono<Void> setMessageCall = proxy.invokeMethod("setMessage", "first message");

    // No call executed yet, so message should not be set.
    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    setMessageCall.block();

    // Now the message has been set.
    Assert.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void lazyContains() {
    ActorProxy proxy = newActorProxy();
    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // Creates the mono plan but does not call it yet.
    Mono<Boolean> hasMessageCall = proxy.invokeMethod("hasMessage", Boolean.class);

    // Sets the message.
    proxy.invokeMethod("setMessage", "hello world").block();

    // Now we check if message is set.
    hasMessageCall.block();

    // Now the message should be set.
    Assert.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void lazyDelete() {
    ActorProxy proxy = newActorProxy();
    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    proxy.invokeMethod("setMessage", "first message").block();

    // Message is set.
    Assert.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // Created the mono plan but does not execute it yet.
    Mono<Void> deleteMessageCall = proxy.invokeMethod("deleteMessage");

    // Message is still set.
    Assert.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());

    deleteMessageCall.block();

    // Now message is not set.
    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void lazyAdd() {
    ActorProxy proxy = newActorProxy();
    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    proxy.invokeMethod("setMessage", "first message").block();

    // Message is set.
    Assert.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // Created the mono plan but does not execute it yet.
    Mono<Void> addMessageCall = proxy.invokeMethod("addMessage", "second message");

    // Message is still set.
    Assert.assertEquals("first message",
      proxy.invokeMethod("getMessage", String.class).block());

    // Delete message
    proxy.invokeMethod("deleteMessage").block();

    // Should work since previous message was deleted.
    addMessageCall.block();

    // New message is still set.
    Assert.assertEquals("second message",
      proxy.invokeMethod("getMessage", String.class).block());
  }

  @Test
  public void onActivateAndOnDeactivate() {
    ActorProxy proxy = newActorProxy();

    Assert.assertTrue(proxy.invokeMethod("isActive", Boolean.class).block());
    Assert.assertFalse(DEACTIVATED_ACTOR_IDS.contains(proxy.getActorId().toString()));

    proxy.invokeMethod("hasMessage", Boolean.class).block();

    this.manager.deactivateActor(proxy.getActorId()).block();

    Assert.assertTrue(DEACTIVATED_ACTOR_IDS.contains(proxy.getActorId().toString()));
  }

  @Test
  public void onPreMethodAndOnPostMethod() {
    ActorProxy proxy = newActorProxy();

    proxy.invokeMethod("hasMessage", Boolean.class).block();

    MyMethodContext preContext =
      proxy.invokeMethod("getPreCallMethodContext", MyMethodContext.class).block();
    Assert.assertEquals("hasMessage", preContext.getName());
    Assert.assertEquals(ActorCallType.ACTOR_INTERFACE_METHOD.toString(), preContext.getType());

    MyMethodContext postContext =
      proxy.invokeMethod("getPostCallMethodContext", MyMethodContext.class).block();
    Assert.assertEquals("hasMessage", postContext.getName());
    Assert.assertEquals(ActorCallType.ACTOR_INTERFACE_METHOD.toString(), postContext.getType());
  }

  @Test
  public void invokeTimer() {
    ActorProxy proxy = newActorProxy();

    this.manager.invokeTimer(proxy.getActorId(), "mytimer", "{ \"callback\": \"hasMessage\" }".getBytes()).block();

    MyMethodContext preContext =
      proxy.invokeMethod("getPreCallMethodContext", MyMethodContext.class).block();
    Assert.assertEquals("mytimer", preContext.getName());
    Assert.assertEquals(ActorCallType.TIMER_METHOD.toString(), preContext.getType());

    MyMethodContext postContext =
      proxy.invokeMethod("getPostCallMethodContext", MyMethodContext.class).block();
    Assert.assertEquals("mytimer", postContext.getName());
    Assert.assertEquals(ActorCallType.TIMER_METHOD.toString(), postContext.getType());
  }

  @Test(expected = IllegalArgumentException.class)
  public void invokeTimerAfterDeactivate() {
    ActorProxy proxy = newActorProxy();

    this.manager.deactivateActor(proxy.getActorId()).block();

    this.manager.invokeTimer(proxy.getActorId(), "mytimer", "{ \"callback\": \"hasMessage\" }".getBytes()).block();
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
    Assert.assertEquals("myreminder", preContext.getName());
    Assert.assertEquals(ActorCallType.REMINDER_METHOD.toString(), preContext.getType());

    MyMethodContext postContext =
      proxy.invokeMethod("getPostCallMethodContext", MyMethodContext.class).block();
    Assert.assertEquals("myreminder", postContext.getName());
    Assert.assertEquals(ActorCallType.REMINDER_METHOD.toString(), postContext.getType());
  }

  @Test(expected = IllegalArgumentException.class)
  public void invokeReminderAfterDeactivate() throws Exception {
    ActorProxy proxy = newActorProxy();

    this.manager.deactivateActor(proxy.getActorId()).block();

    byte[] params = createReminderParams("anything");

    this.manager.invokeReminder(proxy.getActorId(), "myreminder", params).block();
  }

  @Test
  public void classTypeRequestResponseInStateStore() {
    ActorProxy proxy = newActorProxy();

    MyMethodContext expectedContext = new MyMethodContext().setName("MyName").setType("MyType");

    proxy.invokeMethod("setMethodContext", expectedContext).block();
    MyMethodContext context = proxy.invokeMethod("getMethodContext", MyMethodContext.class).block();

    Assert.assertEquals(expectedContext.getName(), context.getName());
    Assert.assertEquals(expectedContext.getType(), context.getType());
  }

  @Test
  public void intTypeRequestResponseInStateStore() {
    ActorProxy proxy = newActorProxy();

    Assert.assertEquals(1, (int)proxy.invokeMethod("incrementAndGetCount", 1, int.class).block());
    Assert.assertEquals(6, (int)proxy.invokeMethod("incrementAndGetCount", 5, int.class).block());
  }

  @Test(expected = NumberFormatException.class)
  public void intTypeWithMethodException() {
    ActorProxy proxy = newActorProxy();

    // Zero is a magic input that will make method throw an exception.
    proxy.invokeMethod("incrementAndGetCount", 0, int.class).block();
  }

  @Test(expected = IllegalStateException.class)
  public void intTypeWithRuntimeException() {
    ActorProxy proxy = newActorProxy();

    proxy.invokeMethod("getCountButThrowsException", int.class).block();
  }

  @Test(expected = IllegalStateException.class)
  public void actorRuntimeException() {
    ActorProxy proxy = newActorProxy();

    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    proxy.invokeMethod("forceDuplicateException").block();
  }

  @Test(expected = IllegalCharsetNameException.class)
  public void actorMethodException() {
    ActorProxy proxy = newActorProxy();

    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    proxy.invokeMethod("throwsWithoutSaving").block();

    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void rollbackChanges() {
    ActorProxy proxy = newActorProxy();

    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // Runs a method that will add one message but fail because tries to add a second one.
    proxy.invokeMethod("forceDuplicateException")
      .onErrorResume(throwable -> Mono.empty())
      .block();

    // No message is set
    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());
  }

  @Test
  public void partialChanges() {
    ActorProxy proxy = newActorProxy();

    Assert.assertFalse(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // Runs a method that will add one message, commit but fail because tries to add a second one.
    proxy.invokeMethod("forcePartialChange")
      .onErrorResume(throwable -> Mono.empty())
      .block();

    // Message is set.
    Assert.assertTrue(proxy.invokeMethod("hasMessage", Boolean.class).block());

    // It is first message and not the second due to a save() in the middle but an exception in the end.
    Assert.assertEquals("first message",
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
