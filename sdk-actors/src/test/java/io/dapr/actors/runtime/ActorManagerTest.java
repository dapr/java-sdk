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
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

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

  interface ActivationActor {
    int increment();
  }

  @ActorType(name = "ActivationActor")
  public static class ActivationActorImpl extends AbstractActor implements ActivationActor {

    private int calls;

    private int activations;

    private int deactivations;

    public ActivationActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
    }

    @Override
    protected Mono<Void> onActivate() {
      return Mono.fromRunnable(() -> this.activations++);
    }

    @Override
    protected Mono<Void> onDeactivate() {
      return Mono.fromRunnable(() -> this.deactivations++);
    }

    @Override
    public int increment() {
      return ++this.calls;
    }
  }

  interface GatedActor {
    Mono<Void> write(String value);

    int increment();
  }

  interface FailingActor {
    Mono<Void> writeThenFail(String value);
  }

  @ActorType(name = "GatedActor")
  public static class GatedActorImpl extends AbstractActor implements GatedActor, FailingActor, Remindable<String> {

    private final Mono<Void> gate;

    private int calls;

    private int deactivations;

    private int postHooks;

    public GatedActorImpl(ActorRuntimeContext runtimeContext, ActorId id, Mono<Void> gate) {
      super(runtimeContext, id);
      this.gate = gate;
    }

    @Override
    protected Mono<Void> onDeactivate() {
      return Mono.fromRunnable(() -> this.deactivations++);
    }

    @Override
    protected Mono<Void> onPostActorMethod(ActorMethodContext actorMethodContext) {
      return Mono.fromRunnable(() -> this.postHooks++);
    }

    @Override
    public Mono<Void> write(String value) {
      return this.gate.then(Mono.defer(() -> this.getActorStateManager().set("value", value)));
    }

    @Override
    public Mono<Void> writeThenFail(String value) {
      return this.write(value).then(Mono.error(new IllegalArgumentException("callback failed")));
    }

    @Override
    public int increment() {
      return ++this.calls;
    }

    @Override
    public TypeRef<String> getStateType() {
      return TypeRef.STRING;
    }

    @Override
    public Mono<Void> receiveReminder(String reminderName, String state, Duration dueTime, Duration period) {
      return this.write(state);
    }
  }

  @ActorType(name = "ActivateWriteActor")
  public static class ActivateWriteActorImpl extends AbstractActor implements GatedActor {

    private final Mono<Void> activateGate;

    private final Mono<Void> callGate;

    private final String tag;

    private final boolean failDeactivate;

    private int calls;

    private int deactivations;

    public ActivateWriteActorImpl(ActorRuntimeContext runtimeContext, ActorId id, Mono<Void> activateGate,
        Mono<Void> callGate, String tag, boolean failDeactivate) {
      super(runtimeContext, id);
      this.activateGate = activateGate;
      this.callGate = callGate;
      this.tag = tag;
      this.failDeactivate = failDeactivate;
    }

    @Override
    protected Mono<Void> onActivate() {
      return this.activateGate.then(Mono.defer(() -> this.getActorStateManager().set("value", this.tag)));
    }

    @Override
    protected Mono<Void> onDeactivate() {
      this.deactivations++;
      return this.failDeactivate ? Mono.error(new IllegalArgumentException("deactivate failed")) : Mono.empty();
    }

    @Override
    public Mono<Void> write(String value) {
      return this.callGate.then(Mono.defer(() -> this.getActorStateManager().set("value", value)));
    }

    @Override
    public int increment() {
      return ++this.calls;
    }
  }

  @ActorType(name = "SaveOnDeactivateActor")
  public static class SaveOnDeactivateActorImpl extends AbstractActor implements GatedActor {

    private final Mono<Void> gate;

    private final String tag;

    public SaveOnDeactivateActorImpl(ActorRuntimeContext runtimeContext, ActorId id, Mono<Void> gate, String tag) {
      super(runtimeContext, id);
      this.gate = gate;
      this.tag = tag;
    }

    @Override
    protected Mono<Void> onDeactivate() {
      return this.getActorStateManager().set("value", this.tag + "-deactivated").then(this.saveState());
    }

    @Override
    public Mono<Void> write(String value) {
      return this.gate.then(Mono.defer(() -> this.getActorStateManager().set("value", value)))
          .then(this.saveState());
    }

    @Override
    public int increment() {
      return 0;
    }
  }

  static class RecordingStateProvider extends DaprStateAsyncProvider {

    private final List<String> saved = Collections.synchronizedList(new ArrayList<>());

    RecordingStateProvider() {
      super(mock(DaprClient.class), new DefaultObjectSerializer());
    }

    @Override
    Mono<Boolean> contains(String actorType, ActorId actorId, String stateName) {
      return Mono.just(false);
    }

    @Override
    Mono<Void> apply(String actorType, ActorId actorId, ActorStateChange... stateChanges) {
      return Mono.fromRunnable(() -> {
        for (ActorStateChange change : stateChanges) {
          this.saved.add(change.getStateName() + "=" + change.getValue());
        }
      });
    }
  }

  static class GetHookMap<K, V> extends HashMap<K, V> {

    private Runnable onGet;

    @Override
    public V get(Object key) {
      V value = super.get(key);
      Runnable hook = this.onGet;
      this.onGet = null;
      if (hook != null) {
        hook.run();
      }
      return value;
    }
  }

  interface SlowActor {
    Mono<Void> hold();

    int getPostCount();
  }

  public static class SlowActorImpl extends AbstractActor implements SlowActor {

    static final AtomicInteger CREATED = new AtomicInteger();

    static final AtomicInteger POST_COUNT = new AtomicInteger();

    static final AtomicInteger HOLDING = new AtomicInteger();

    static volatile Sinks.Empty<Void> release = Sinks.empty();

    public SlowActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
      CREATED.incrementAndGet();
    }

    @Override
    public Mono<Void> hold() {
      HOLDING.incrementAndGet();
      return release.asMono();
    }

    @Override
    public int getPostCount() {
      return POST_COUNT.get();
    }

    @Override
    protected Mono<Void> onActivate() {
      // Widens the window in which two activations of the same actor could both create it.
      return Mono.delay(Duration.ofMillis(50)).then();
    }

    @Override
    protected Mono<Void> onPostActorMethod(ActorMethodContext actorMethodContext) {
      return Mono.fromRunnable(POST_COUNT::incrementAndGet);
    }

    static void reset() {
      CREATED.set(0);
      POST_COUNT.set(0);
      HOLDING.set(0);
      release = Sinks.empty();
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

  @Test
  public void sameActivationIdReusesActor() throws Exception {
    List<ActivationActorImpl> created = new ArrayList<>();
    ActorManager<ActivationActorImpl> manager = createActivationManager(created);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    Assertions.assertEquals(1, increment(manager, actorId));
    manager.activateActor(actorId, "a1").block();
    Assertions.assertEquals(2, increment(manager, actorId));

    Assertions.assertEquals(1, created.size());
    Assertions.assertEquals(1, created.get(0).activations);
    Assertions.assertEquals(0, created.get(0).deactivations);
  }

  @Test
  public void newActivationIdReplacesActor() throws Exception {
    List<ActivationActorImpl> created = new ArrayList<>();
    ActorManager<ActivationActorImpl> manager = createActivationManager(created);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    Assertions.assertEquals(1, increment(manager, actorId));
    Assertions.assertEquals(2, increment(manager, actorId));
    manager.activateActor(actorId, "a2").block();

    Assertions.assertEquals(2, created.size());
    Assertions.assertEquals(1, created.get(0).deactivations);
    Assertions.assertEquals(1, created.get(1).activations);
    Assertions.assertEquals(0, created.get(1).deactivations);
    Assertions.assertEquals(1, increment(manager, actorId));
    Assertions.assertEquals(2, created.get(0).calls);
  }

  @Test
  public void missingActivationIdNeverResets() throws Exception {
    List<ActivationActorImpl> created = new ArrayList<>();
    ActorManager<ActivationActorImpl> manager = createActivationManager(created);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    Assertions.assertEquals(1, increment(manager, actorId));
    manager.activateActor(actorId, "").block();
    Assertions.assertEquals(2, increment(manager, actorId));
    manager.activateActor(actorId, null).block();
    Assertions.assertEquals(3, increment(manager, actorId));
    manager.activateActor(actorId).block();
    Assertions.assertEquals(4, increment(manager, actorId));

    Assertions.assertEquals(1, created.size());
    Assertions.assertEquals(0, created.get(0).deactivations);
  }

  @Test
  public void activationIdAfterMissingActivationIdResets() throws Exception {
    List<ActivationActorImpl> created = new ArrayList<>();
    ActorManager<ActivationActorImpl> manager = createActivationManager(created);
    ActorId actorId = newActorId();

    manager.activateActor(actorId).block();
    Assertions.assertEquals(1, increment(manager, actorId));
    manager.activateActor(actorId, "a1").block();
    Assertions.assertEquals(2, created.size());
    Assertions.assertEquals(1, created.get(0).deactivations);
    Assertions.assertEquals(1, increment(manager, actorId));

    manager.activateActor(actorId, "a1").block();
    Assertions.assertEquals(2, increment(manager, actorId));
    Assertions.assertEquals(2, created.size());
  }

  @Test
  public void activationIdIsKeptPerActor() throws Exception {
    List<ActivationActorImpl> created = new ArrayList<>();
    ActorManager<ActivationActorImpl> manager = createActivationManager(created);
    ActorId first = newActorId();
    ActorId second = newActorId();

    manager.activateActor(first, "a1").block();
    manager.activateActor(second, "b1").block();
    manager.activateActor(first, "a2").block();
    manager.activateActor(second, "b1").block();

    Assertions.assertEquals(3, created.size());
    Assertions.assertEquals(1, created.get(0).deactivations);
    Assertions.assertEquals(0, created.get(1).deactivations);
  }

  @Test
  public void deactivateThenNewActivationIdCreatesFreshActor() throws Exception {
    List<ActivationActorImpl> created = new ArrayList<>();
    ActorManager<ActivationActorImpl> manager = createActivationManager(created);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    increment(manager, actorId);
    manager.deactivateActor(actorId).block();
    manager.activateActor(actorId, "a2").block();

    Assertions.assertEquals(2, created.size());
    Assertions.assertEquals(1, created.get(0).deactivations);
    Assertions.assertEquals(1, increment(manager, actorId));
  }

  @Test
  public void activateWithNewActivationIdIsLazy() throws Exception {
    List<ActivationActorImpl> created = new ArrayList<>();
    ActorManager<ActivationActorImpl> manager = createActivationManager(created);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    Mono<?> reset = manager.activateActor(actorId, "a2");
    Assertions.assertEquals(1, created.size());
    Assertions.assertEquals(0, created.get(0).deactivations);

    reset.block();
    Assertions.assertEquals(2, created.size());
    Assertions.assertEquals(1, created.get(0).deactivations);
  }

  @Test
  public void replacedActivationSkipsStaleSave() throws Exception {
    RecordingStateProvider provider = new RecordingStateProvider();
    List<GatedActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> gate = new CompletableFuture<>();
    ActorManager<GatedActorImpl> manager = createGatedManager(provider, created, gate);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    CompletableFuture<byte[]> stale = write(manager, actorId, "stale").toFuture();
    CompletableFuture<?> reset = manager.activateActor(actorId, "a2").toFuture();
    gate.complete(null);
    reset.get(5, TimeUnit.SECONDS);
    write(manager, actorId, "fresh").block();

    Assertions.assertEquals(List.of("value=fresh"), provider.saved);
    assertCause(IllegalStateException.class, stale);
  }

  @Test
  public void replacedActivationSkipsStaleTimerSave() throws Exception {
    RecordingStateProvider provider = new RecordingStateProvider();
    List<GatedActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> gate = new CompletableFuture<>();
    ActorManager<GatedActorImpl> manager = createGatedManager(provider, created, gate);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    CompletableFuture<Void> stale = manager.invokeTimer(actorId, "t", createTimerParams("write", "stale")).toFuture();
    CompletableFuture<?> reset = manager.activateActor(actorId, "a2").toFuture();
    gate.complete(null);
    reset.get(5, TimeUnit.SECONDS);
    write(manager, actorId, "fresh").block();

    Assertions.assertEquals(List.of("value=fresh"), provider.saved);
    assertCause(IllegalStateException.class, stale);
  }

  @Test
  public void replacedActivationSkipsStaleReminderSave() throws Exception {
    RecordingStateProvider provider = new RecordingStateProvider();
    List<GatedActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> gate = new CompletableFuture<>();
    ActorManager<GatedActorImpl> manager = createGatedManager(provider, created, gate);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    CompletableFuture<Void> stale = manager.invokeReminder(actorId, "r", createReminderParams("stale")).toFuture();
    CompletableFuture<?> reset = manager.activateActor(actorId, "a2").toFuture();
    gate.complete(null);
    reset.get(5, TimeUnit.SECONDS);
    write(manager, actorId, "fresh").block();

    Assertions.assertEquals(List.of("value=fresh"), provider.saved);
    assertCause(IllegalStateException.class, stale);
  }

  @Test
  public void replacedActivationRunsPostHookButSkipsSave() throws Exception {
    RecordingStateProvider provider = new RecordingStateProvider();
    List<GatedActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> gate = new CompletableFuture<>();
    ActorManager<GatedActorImpl> manager = createGatedManager(provider, created, gate);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    CompletableFuture<byte[]> stale = write(manager, actorId, "stale").toFuture();
    CompletableFuture<?> reset = manager.activateActor(actorId, "a2").toFuture();
    gate.complete(null);
    reset.get(5, TimeUnit.SECONDS);

    assertCause(IllegalStateException.class, stale);
    Assertions.assertEquals(1, created.get(0).postHooks);
    Assertions.assertEquals(List.of(), provider.saved);
  }

  @Test
  public void deactivatedActorRunsPostHookButSkipsSave() throws Exception {
    RecordingStateProvider provider = new RecordingStateProvider();
    List<GatedActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> gate = new CompletableFuture<>();
    ActorManager<GatedActorImpl> manager = createGatedManager(provider, created, gate);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    CompletableFuture<Void> stale = manager.invokeTimer(actorId, "t", createTimerParams("write", "stale")).toFuture();
    CompletableFuture<Void> deactivation = manager.deactivateActor(actorId).toFuture();
    gate.complete(null);
    deactivation.get(5, TimeUnit.SECONDS);

    assertCause(IllegalStateException.class, stale);
    Assertions.assertEquals(1, created.get(0).postHooks);
    Assertions.assertEquals(List.of(), provider.saved);
  }

  @Test
  public void failingTimerCallbackSkipsSave() throws Exception {
    RecordingStateProvider provider = new RecordingStateProvider();
    List<GatedActorImpl> created = new ArrayList<>();
    ActorManager<GatedActorImpl> manager =
        createGatedManager(provider, created, CompletableFuture.completedFuture(null));
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    CompletableFuture<Void> call =
        manager.invokeTimer(actorId, "t", createTimerParams("writeThenFail", "lost")).toFuture();

    assertCause(IllegalArgumentException.class, call);
    Assertions.assertEquals(0, created.get(0).postHooks);
    Assertions.assertEquals(List.of(), provider.saved);
  }

  @Test
  public void liveActivationSavesAfterSameOrMissingId() throws Exception {
    RecordingStateProvider provider = new RecordingStateProvider();
    List<GatedActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> gate = new CompletableFuture<>();
    ActorManager<GatedActorImpl> manager = createGatedManager(provider, created, gate);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    CompletableFuture<byte[]> call = write(manager, actorId, "live").toFuture();
    manager.activateActor(actorId, "a1").block();
    manager.activateActor(actorId, null).block();
    manager.activateActor(actorId, "").block();
    gate.complete(null);

    call.get(5, TimeUnit.SECONDS);
    Assertions.assertEquals(List.of("value=live"), provider.saved);
    Assertions.assertEquals(1, created.size());
  }

  @Test
  public void deactivatedActorSkipsSave() throws Exception {
    RecordingStateProvider provider = new RecordingStateProvider();
    List<GatedActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> gate = new CompletableFuture<>();
    ActorManager<GatedActorImpl> manager = createGatedManager(provider, created, gate);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    CompletableFuture<byte[]> call = write(manager, actorId, "stale").toFuture();
    CompletableFuture<Void> deactivation = manager.deactivateActor(actorId).toFuture();
    gate.complete(null);
    deactivation.get(5, TimeUnit.SECONDS);

    Assertions.assertEquals(List.of(), provider.saved);
    assertCause(IllegalStateException.class, call);
  }

  @Test
  public void previousActivationIdCreatesFreshActor() throws Exception {
    List<ActivationActorImpl> created = new ArrayList<>();
    ActorManager<ActivationActorImpl> manager = createActivationManager(created);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    manager.activateActor(actorId, "a2").block();
    Assertions.assertEquals(1, increment(manager, actorId));
    manager.activateActor(actorId, "a1").block();

    Assertions.assertEquals(3, created.size());
    Assertions.assertEquals(1, created.get(1).deactivations);
    Assertions.assertEquals(1, created.get(2).activations);
    Assertions.assertEquals(1, increment(manager, actorId));
  }

  @Test
  public void missingActivationIdAfterReplaceKeepsActor() throws Exception {
    List<ActivationActorImpl> created = new ArrayList<>();
    ActorManager<ActivationActorImpl> manager = createActivationManager(created);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    manager.activateActor(actorId, "a2").block();
    Assertions.assertEquals(1, increment(manager, actorId));
    manager.activateActor(actorId, null).block();
    manager.activateActor(actorId, "").block();
    Assertions.assertEquals(2, increment(manager, actorId));
    Assertions.assertEquals(2, created.size());
  }

  @Test
  public void queuedActivationWithDifferentIdFailsAndKeepsFirst() throws Exception {
    RecordingStateProvider provider = new RecordingStateProvider();
    List<ActivateWriteActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> activateGate = new CompletableFuture<>();
    ActorManager<ActivateWriteActorImpl> manager = createLateActivationManager(
        provider, created, activateGate, CompletableFuture.completedFuture(null), false);
    ActorId actorId = newActorId();

    CompletableFuture<?> first = manager.activateActor(actorId, "a").toFuture();
    CompletableFuture<?> queued = manager.activateActor(actorId, "b").toFuture();
    activateGate.complete(null);

    first.get(5, TimeUnit.SECONDS);
    assertCause(IllegalStateException.class, queued);
    Assertions.assertEquals(1, created.size());

    // Dapr retries the failed call, which then replaces the first activation.
    manager.activateActor(actorId, "b").block();
    manager.invokeMethod(actorId, "increment", null).block();
    Assertions.assertEquals(2, created.size());
    Assertions.assertEquals(1, created.get(0).deactivations);
    Assertions.assertEquals(0, created.get(0).calls);
    Assertions.assertEquals(1, created.get(1).calls);
    Assertions.assertEquals(List.of("value=late", "value=newer"), provider.saved);
  }

  @Test
  public void queuedActivationWithSameIdReusesFirst() throws Exception {
    assertQueuedActivationReusesFirst("a");
  }

  @Test
  public void queuedActivationWithoutIdReusesFirst() throws Exception {
    assertQueuedActivationReusesFirst(null);
  }

  private static void assertQueuedActivationReusesFirst(String queuedId) throws Exception {
    RecordingStateProvider provider = new RecordingStateProvider();
    List<ActivateWriteActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> activateGate = new CompletableFuture<>();
    ActorManager<ActivateWriteActorImpl> manager =
        createLateActivationManager(provider, created, activateGate, new CompletableFuture<>(), false);
    ActorId actorId = newActorId();

    CompletableFuture<ActorManager.ActiveActor<ActivateWriteActorImpl>> first =
        manager.activateActor(actorId, "a").toFuture();
    CompletableFuture<ActorManager.ActiveActor<ActivateWriteActorImpl>> queued =
        manager.activateActor(actorId, queuedId).toFuture();
    activateGate.complete(null);

    Assertions.assertSame(first.get(5, TimeUnit.SECONDS), queued.get(5, TimeUnit.SECONDS));
    Assertions.assertEquals(1, created.size());
    Assertions.assertEquals(List.of("value=late"), provider.saved);
  }

  @Test
  public void replacedActorCannotSaveFromOnDeactivate() throws Exception {
    RecordingStateProvider provider = new RecordingStateProvider();
    List<SaveOnDeactivateActorImpl> created = new ArrayList<>();
    ActorManager<SaveOnDeactivateActorImpl> manager =
        createSaveOnDeactivateManager(provider, created, CompletableFuture.completedFuture(null));
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    manager.activateActor(actorId, "a2").block();
    Assertions.assertEquals(List.of(), provider.saved);

    manager.deactivateActor(actorId).block();
    Assertions.assertEquals(List.of("value=actor1-deactivated"), provider.saved);
  }

  @Test
  public void replacedActorCannotSaveFromRunningCall() throws Exception {
    RecordingStateProvider provider = new RecordingStateProvider();
    List<SaveOnDeactivateActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> gate = new CompletableFuture<>();
    ActorManager<SaveOnDeactivateActorImpl> manager = createSaveOnDeactivateManager(provider, created, gate);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    CompletableFuture<byte[]> stale = manager.invokeMethod(
        actorId, "write", new DefaultObjectSerializer().serialize("stale")).toFuture();
    CompletableFuture<?> reset = manager.activateActor(actorId, "a2").toFuture();
    gate.complete(null);

    assertCause(IllegalStateException.class, stale);
    reset.get(5, TimeUnit.SECONDS);
    Assertions.assertEquals(List.of(), provider.saved);
    Assertions.assertEquals(2, created.size());
  }

  @Test
  public void resetSurvivesFailingDeactivate() throws Exception {
    List<ActivateWriteActorImpl> created = new ArrayList<>();
    ActorManager<ActivateWriteActorImpl> manager = createLateActivationManager(new RecordingStateProvider(), created,
        CompletableFuture.completedFuture(null), new CompletableFuture<>(), true);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    manager.activateActor(actorId, "a2").block();
    manager.invokeMethod(actorId, "increment", null).block();

    Assertions.assertEquals(2, created.size());
    Assertions.assertEquals(1, created.get(0).deactivations);
    Assertions.assertEquals(0, created.get(0).calls);
    Assertions.assertEquals(1, created.get(1).calls);
  }

  @Test
  public void deactivateFailsWhenOnDeactivateThrows() throws Exception {
    List<ActivateWriteActorImpl> created = new ArrayList<>();
    ActorManager<ActivateWriteActorImpl> manager = createLateActivationManager(new RecordingStateProvider(), created,
        CompletableFuture.completedFuture(null), new CompletableFuture<>(), true);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();

    assertThrows(IllegalArgumentException.class, () -> manager.deactivateActor(actorId).block());
    Assertions.assertEquals(1, created.get(0).deactivations);
  }

  @Test
  public void concurrentResetKeepsTheWinner() throws Exception {
    List<ActivationActorImpl> created = new ArrayList<>();
    ActorManager<ActivationActorImpl> manager = createActivationManager(created);
    GetHookMap<Object, Object> activeActors = hookActiveActors(manager);
    ActorId actorId = newActorId();

    manager.activateActor(actorId, "a1").block();
    List<ActorManager.ActiveActor<ActivationActorImpl>> winner = new ArrayList<>();
    activeActors.onGet = () -> winner.add(manager.activateActor(actorId, "a2").block());
    ActorManager.ActiveActor<ActivationActorImpl> loser = manager.activateActor(actorId, "a2").block();

    Assertions.assertSame(winner.get(0), loser);
    Assertions.assertEquals(2, created.size());
    Assertions.assertEquals(1, created.get(0).deactivations);
    Assertions.assertEquals(0, created.get(1).deactivations);
    Assertions.assertEquals(1, increment(manager, actorId));
  }

  private static GetHookMap<Object, Object> hookActiveActors(ActorManager<?> manager) throws Exception {
    Field actors = ActorManager.class.getDeclaredField("activeActors");
    actors.setAccessible(true);
    GetHookMap<Object, Object> map = new GetHookMap<>();
    actors.set(manager, map);
    return map;
  }

  private static ActorManager<ActivateWriteActorImpl> createLateActivationManager(
      RecordingStateProvider provider, List<ActivateWriteActorImpl> created, CompletableFuture<Void> activateGate,
      CompletableFuture<Void> callGate, boolean failLateDeactivate) {
    ActorRuntimeContext<ActivateWriteActorImpl> context = new ActorRuntimeContext(
      mock(ActorRuntime.class),
      new DefaultObjectSerializer(),
      (ActorFactory<ActivateWriteActorImpl>) (ctx, id) -> {
        ActivateWriteActorImpl actor = created.isEmpty()
            ? new ActivateWriteActorImpl(ctx, id, Mono.fromFuture(activateGate), Mono.empty(), "late",
                failLateDeactivate)
            : new ActivateWriteActorImpl(ctx, id, Mono.empty(), Mono.fromFuture(callGate), "newer", false);
        created.add(actor);
        return actor;
      },
      ActorTypeInformation.create(ActivateWriteActorImpl.class),
      mock(DaprClient.class),
      provider
    );
    return new ActorManager<>(context);
  }

  private static ActorManager<SaveOnDeactivateActorImpl> createSaveOnDeactivateManager(
      RecordingStateProvider provider, List<SaveOnDeactivateActorImpl> created, CompletableFuture<Void> gate) {
    ActorRuntimeContext<SaveOnDeactivateActorImpl> context = new ActorRuntimeContext(
      mock(ActorRuntime.class),
      new DefaultObjectSerializer(),
      (ActorFactory<SaveOnDeactivateActorImpl>) (ctx, id) -> {
        Mono<Void> actorGate = created.isEmpty() ? Mono.fromFuture(gate) : Mono.empty();
        SaveOnDeactivateActorImpl actor =
            new SaveOnDeactivateActorImpl(ctx, id, actorGate, "actor" + created.size());
        created.add(actor);
        return actor;
      },
      ActorTypeInformation.create(SaveOnDeactivateActorImpl.class),
      mock(DaprClient.class),
      provider
    );
    return new ActorManager<>(context);
  }

  private static ActorManager<GatedActorImpl> createGatedManager(
      RecordingStateProvider provider, List<GatedActorImpl> created, CompletableFuture<Void> gate) {
    ActorRuntimeContext<GatedActorImpl> context = new ActorRuntimeContext(
      mock(ActorRuntime.class),
      new DefaultObjectSerializer(),
      (ActorFactory<GatedActorImpl>) (ctx, id) -> {
        Mono<Void> actorGate = created.isEmpty() ? Mono.fromFuture(gate) : Mono.empty();
        GatedActorImpl actor = new GatedActorImpl(ctx, id, actorGate);
        created.add(actor);
        return actor;
      },
      ActorTypeInformation.create(GatedActorImpl.class),
      mock(DaprClient.class),
      provider
    );
    return new ActorManager<>(context);
  }

  private static Mono<byte[]> write(ActorManager<GatedActorImpl> manager, ActorId actorId, String value)
      throws IOException {
    return manager.invokeMethod(actorId, "write", new DefaultObjectSerializer().serialize(value));
  }

  private static void assertCause(Class<? extends Throwable> expected, CompletableFuture<?> future) {
    ExecutionException e = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    Assertions.assertTrue(expected.isInstance(e.getCause()), String.valueOf(e.getCause()));
  }

  private static ActorManager<ActivationActorImpl> createActivationManager(List<ActivationActorImpl> created) {
    ActorRuntimeContext<ActivationActorImpl> context = new ActorRuntimeContext(
      mock(ActorRuntime.class),
      new DefaultObjectSerializer(),
      (ActorFactory<ActivationActorImpl>) (ctx, id) -> {
        ActivationActorImpl actor = new ActivationActorImpl(ctx, id);
        created.add(actor);
        return actor;
      },
      ActorTypeInformation.create(ActivationActorImpl.class),
      mock(DaprClient.class),
      mock(DaprStateAsyncProvider.class)
    );
    return new ActorManager<>(context);
  }

  private static int increment(ActorManager<ActivationActorImpl> manager, ActorId actorId) throws IOException {
    byte[] response = manager.invokeMethod(actorId, "increment", null).block();
    return new DefaultObjectSerializer().deserialize(response, Integer.class);
  }

  @Test
  public void cancelledCallKeepsTurnThroughPostMethod() throws Exception {
    SlowActorImpl.reset();
    ActorManager<SlowActorImpl> manager = new ActorManager<>(createContext(SlowActorImpl.class));
    ActorId actorId = newActorId();
    manager.activateActor(actorId).block();

    Disposable first = manager.invokeMethod(actorId, "hold", null).subscribe();
    waitFor(() -> SlowActorImpl.HOLDING.get() == 1);
    // Dapr stops waiting for the call, as when its caller cancels.
    first.dispose();

    CompletableFuture<byte[]> second = manager.invokeMethod(actorId, "getPostCount", null).toFuture();
    Thread.sleep(100);
    Assertions.assertFalse(second.isDone());

    SlowActorImpl.release.tryEmitEmpty();
    // The second call ran after the first call's post-method step.
    Assertions.assertEquals("1", new String(second.get(5, TimeUnit.SECONDS)));
  }

  @Test
  public void concurrentActivationsCreateOneActor() {
    SlowActorImpl.reset();
    ActorManager<SlowActorImpl> manager = new ActorManager<>(createContext(SlowActorImpl.class));
    ActorId actorId = newActorId();

    Mono.when(IntStream.range(0, 10)
        .mapToObj(i -> manager.activateActor(actorId).subscribeOn(Schedulers.boundedElastic()))
        .toArray(Mono[]::new))
        .block(Duration.ofSeconds(5));

    Assertions.assertEquals(1, SlowActorImpl.CREATED.get());
  }

  @Test
  public void deactivationWaitsForRunningCall() throws Exception {
    SlowActorImpl.reset();
    ActorManager<SlowActorImpl> manager = new ActorManager<>(createContext(SlowActorImpl.class));
    ActorId actorId = newActorId();
    manager.activateActor(actorId).block();

    Disposable call = manager.invokeMethod(actorId, "hold", null).subscribe();
    waitFor(() -> SlowActorImpl.HOLDING.get() == 1);
    CompletableFuture<Void> deactivation = manager.deactivateActor(actorId).toFuture();
    Thread.sleep(100);
    Assertions.assertFalse(deactivation.isDone());

    SlowActorImpl.release.tryEmitEmpty();
    deactivation.get(5, TimeUnit.SECONDS);
    Assertions.assertEquals(1, SlowActorImpl.POST_COUNT.get());
    call.dispose();
  }

  @Test
  public void replacedActorDeactivatesAfterRunningCall() throws Exception {
    List<GatedActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> gate = new CompletableFuture<>();
    ActorManager<GatedActorImpl> manager = createGatedManager(new RecordingStateProvider(), created, gate);
    ActorId actorId = newActorId();
    manager.activateActor(actorId, "a1").block();

    CompletableFuture<byte[]> stale = write(manager, actorId, "stale").toFuture();
    CompletableFuture<?> reset = manager.activateActor(actorId, "a2").toFuture();
    Thread.sleep(100);
    Assertions.assertFalse(reset.isDone());
    Assertions.assertEquals(0, created.get(0).deactivations);

    gate.complete(null);
    reset.get(5, TimeUnit.SECONDS);
    Assertions.assertEquals(1, created.get(0).deactivations);
    Assertions.assertEquals(2, created.size());
    assertCause(IllegalStateException.class, stale);
  }

  @Test
  public void cancelledReplacementStillDeactivatesStaleActor() throws Exception {
    List<GatedActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> gate = new CompletableFuture<>();
    ActorManager<GatedActorImpl> manager = createGatedManager(new RecordingStateProvider(), created, gate);
    ActorId actorId = newActorId();
    manager.activateActor(actorId, "a1").block();

    CompletableFuture<byte[]> stale = write(manager, actorId, "stale").toFuture();
    manager.activateActor(actorId, "a2").subscribe().dispose();
    Thread.sleep(100);
    Assertions.assertEquals(0, created.get(0).deactivations);

    gate.complete(null);
    waitFor(() -> created.get(0).deactivations == 1);
    assertCause(IllegalStateException.class, stale);
    Assertions.assertEquals(1, created.size());

    manager.activateActor(actorId, "a2").block(Duration.ofSeconds(5));
    Assertions.assertEquals(2, created.size());
  }

  @Test
  public void cancelledDeactivationStillDeactivates() throws Exception {
    List<GatedActorImpl> created = new ArrayList<>();
    CompletableFuture<Void> gate = new CompletableFuture<>();
    ActorManager<GatedActorImpl> manager = createGatedManager(new RecordingStateProvider(), created, gate);
    ActorId actorId = newActorId();
    manager.activateActor(actorId, "a1").block();

    CompletableFuture<byte[]> running = write(manager, actorId, "value").toFuture();
    manager.deactivateActor(actorId).subscribe().dispose();
    Thread.sleep(100);
    Assertions.assertEquals(0, created.get(0).deactivations);

    gate.complete(null);
    waitFor(() -> created.get(0).deactivations == 1);
    assertCause(IllegalStateException.class, running);
  }

  private static void waitFor(BooleanSupplier condition) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("timed out waiting for condition");
      }
      Thread.sleep(10);
    }
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
