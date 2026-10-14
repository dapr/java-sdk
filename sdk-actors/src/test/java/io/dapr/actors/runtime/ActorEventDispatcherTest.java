/*
 * Copyright 2026 The Dapr Authors
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

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprActorsProtos.ActorEntityConfig;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsRequestAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsRequestInitialAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseDeactivateRequestAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseInvokeRequestAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseReminderRequestAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseTimerRequestAlpha1;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class ActorEventDispatcherTest {

  private static final DefaultObjectSerializer SERIALIZER = new DefaultObjectSerializer();

  private static final AtomicReference<String> LAST_REMINDER = new AtomicReference<>();

  private static final AtomicReference<String> LAST_TIMER_DATA = new AtomicReference<>();

  public interface MyRemindableActor {
    void receiveTimer(String data);
  }

  @ActorType(name = "MyRemindableActor")
  public static class MyRemindableActorImpl extends AbstractActor implements MyRemindableActor, Remindable<String> {

    public MyRemindableActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
    }

    @Override
    public TypeRef<String> getStateType() {
      return TypeRef.STRING;
    }

    @Override
    public Mono<Void> receiveReminder(String reminderName, String state, Duration dueTime, Duration period) {
      return Mono.fromRunnable(() -> LAST_REMINDER.set(reminderName + ":" + state));
    }

    @Override
    public void receiveTimer(String data) {
      LAST_TIMER_DATA.set(data);
    }
  }

  public interface ThrowingActor {
    void lookup() throws NoSuchMethodException;
  }

  @ActorType(name = "ThrowingActor")
  public static class ThrowingActorImpl extends AbstractActor implements ThrowingActor {

    public ThrowingActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
    }

    @Override
    public void lookup() throws NoSuchMethodException {
      throw new NoSuchMethodException("thrown by the actor");
    }
  }

  private ActorRuntime runtime;

  static ActorRuntime newRuntime() throws Exception {
    Constructor<ActorRuntime> constructor = (Constructor<ActorRuntime>) Arrays.stream(
            ActorRuntime.class.getDeclaredConstructors())
        .filter(c -> c.getParameters().length == 2)
        .findFirst()
        .get();
    constructor.setAccessible(true);
    return constructor.newInstance(null, mock(DaprClient.class));
  }

  @BeforeEach
  public void setup() throws Exception {
    runtime = newRuntime();
    runtime.registerActor(ActorRuntimeTest.MyActorImpl.class);
    runtime.registerActor(MyRemindableActorImpl.class);
    runtime.registerActor(ThrowingActorImpl.class);
  }

  @Test
  public void initialRequestCarriesTypesAndConfig() {
    runtime.getConfig()
        .setActorIdleTimeout(Duration.ofMinutes(2))
        .setDrainOngoingCallTimeout(Duration.ofMillis(1500))
        .setDrainBalancedActors(true)
        .addActorTypeConfig(new ActorTypeConfig()
            .setActorTypeName("MyGreatActor")
            .setActorIdleTimeout(Duration.ofSeconds(30)));

    SubscribeActorEventsRequestInitialAlpha1 initial =
        ActorEventDispatcher.buildInitialRequest(runtime.getConfig()).getInitialRequest();

    assertEquals(Arrays.asList("MyGreatActor", "MyRemindableActor", "ThrowingActor"), initial.getEntitiesList());
    assertEquals(120, initial.getActorIdleTimeout().getSeconds());
    assertEquals(1, initial.getDrainOngoingCallTimeout().getSeconds());
    assertEquals(500_000_000, initial.getDrainOngoingCallTimeout().getNanos());
    assertTrue(initial.getDrainRebalancedActors());
    assertFalse(initial.hasReentrancy());
    ActorEntityConfig entityConfig = initial.getEntitiesConfig(0);
    assertEquals(Arrays.asList("MyGreatActor"), entityConfig.getEntitiesList());
    assertEquals(30, entityConfig.getActorIdleTimeout().getSeconds());
    assertFalse(entityConfig.hasDrainOngoingCallTimeout());
  }

  @Test
  public void initialRequestLeavesUnsetConfigUnset() {
    SubscribeActorEventsRequestInitialAlpha1 initial =
        ActorEventDispatcher.buildInitialRequest(new ActorRuntimeConfig()).getInitialRequest();

    assertFalse(initial.hasActorIdleTimeout());
    assertFalse(initial.hasDrainOngoingCallTimeout());
    assertFalse(initial.hasDrainRebalancedActors());
  }

  @Test
  public void invokeReturnsSerializedResult() throws Exception {
    SubscribeActorEventsRequestAlpha1 response = dispatch(invoke("1", "MyGreatActor", "say"));

    assertEquals("1", response.getInvokeResponse().getId());
    assertEquals("Nothing to say.",
        SERIALIZER.deserialize(response.getInvokeResponse().getData().toByteArray(), String.class));
    assertEquals("application/json", response.getInvokeResponse().getMetadataMap().get("content-type"));
    assertFalse(response.getInvokeResponse().getError());
  }

  @Test
  public void invokeUnknownActorTypeIsNotFound() {
    SubscribeActorEventsRequestAlpha1 response = dispatch(invoke("2", "Unknown", "say"));

    assertEquals("2", response.getRequestFailed().getId());
    assertEquals(Status.Code.NOT_FOUND.value(), response.getRequestFailed().getCode());
  }

  @Test
  public void invokeUnknownMethodIsNotFound() {
    SubscribeActorEventsRequestAlpha1 response = dispatch(invoke("3", "MyGreatActor", "missing"));

    assertEquals("3", response.getRequestFailed().getId());
    assertEquals(Status.Code.NOT_FOUND.value(), response.getRequestFailed().getCode());
  }

  @Test
  public void noSuchMethodThrownByActorIsUnknown() {
    SubscribeActorEventsRequestAlpha1 response = dispatch(invoke("11", "ThrowingActor", "lookup"));

    assertEquals("11", response.getRequestFailed().getId());
    assertEquals(Status.Code.UNKNOWN.value(), response.getRequestFailed().getCode());
  }

  @Test
  public void deactivateCallsOnDeactivate() {
    String actorId = UUID.randomUUID().toString();
    dispatch(invoke("4", "MyGreatActor", "say", actorId));

    SubscribeActorEventsRequestAlpha1 response = dispatch(deactivate("5", "MyGreatActor", actorId));

    assertEquals("5", response.getDeactivateResponse().getId());
  }

  @Test
  public void handlerExceptionIsUnknown() {
    // onDeactivate throws when the actor was never called.
    String actorId = UUID.randomUUID().toString();
    dispatch(invoke("6", "MyGreatActor", "count", actorId));

    SubscribeActorEventsRequestAlpha1 response = dispatch(deactivate("7", "MyGreatActor", actorId));

    assertEquals("7", response.getRequestFailed().getId());
    assertEquals(Status.Code.UNKNOWN.value(), response.getRequestFailed().getCode());
    assertTrue(response.getRequestFailed().getMessage().contains("test expects a call before deactivate"));
  }

  @Test
  public void reminderDeliversData() throws Exception {
    SubscribeActorEventsResponseAlpha1 request = SubscribeActorEventsResponseAlpha1.newBuilder()
        .setReminderRequest(SubscribeActorEventsResponseReminderRequestAlpha1.newBuilder()
            .setId("8")
            .setActorType("MyRemindableActor")
            .setActorId(UUID.randomUUID().toString())
            .setName("myreminder")
            .setDueTime("1s")
            .setData(daprData("hello")))
        .build();

    SubscribeActorEventsRequestAlpha1 response = dispatch(request);

    assertEquals("8", response.getReminderResponse().getId());
    assertFalse(response.getReminderResponse().getCancel());
    assertEquals("myreminder:hello", LAST_REMINDER.get());
  }

  @Test
  public void reminderWithoutDataIsDelivered() {
    SubscribeActorEventsResponseAlpha1 request = SubscribeActorEventsResponseAlpha1.newBuilder()
        .setReminderRequest(SubscribeActorEventsResponseReminderRequestAlpha1.newBuilder()
            .setId("9")
            .setActorType("MyRemindableActor")
            .setActorId(UUID.randomUUID().toString())
            .setName("empty"))
        .build();

    SubscribeActorEventsRequestAlpha1 response = dispatch(request);

    assertEquals("9", response.getReminderResponse().getId());
    assertEquals("empty:null", LAST_REMINDER.get());
  }

  @Test
  public void timerInvokesCallback() throws Exception {
    SubscribeActorEventsResponseAlpha1 request = SubscribeActorEventsResponseAlpha1.newBuilder()
        .setTimerRequest(SubscribeActorEventsResponseTimerRequestAlpha1.newBuilder()
            .setId("10")
            .setActorType("MyRemindableActor")
            .setActorId(UUID.randomUUID().toString())
            .setName("mytimer")
            .setDueTime("0s")
            .setPeriod("1s")
            .setCallback("receiveTimer")
            .setData(daprData("tick")))
        .build();

    SubscribeActorEventsRequestAlpha1 response = dispatch(request);

    assertEquals("10", response.getTimerResponse().getId());
    assertEquals("tick", LAST_TIMER_DATA.get());
  }

  /**
   * Builds data the way Dapr stores it: a BytesValue holding the JSON (base64 string) of the actor's bytes.
   */
  private static Any daprData(String value) throws Exception {
    String base64 = Base64.getEncoder().encodeToString(SERIALIZER.serialize(value));
    return Any.pack(BytesValue.of(ByteString.copyFrom("\"" + base64 + "\"", StandardCharsets.UTF_8)));
  }

  private SubscribeActorEventsRequestAlpha1 dispatch(SubscribeActorEventsResponseAlpha1 request) {
    return ActorEventDispatcher.dispatch(runtime, request).block();
  }

  static SubscribeActorEventsResponseAlpha1 invoke(String id, String actorType, String method) {
    return invoke(id, actorType, method, UUID.randomUUID().toString());
  }

  static SubscribeActorEventsResponseAlpha1 invoke(String id, String actorType, String method, String actorId) {
    return SubscribeActorEventsResponseAlpha1.newBuilder()
        .setInvokeRequest(SubscribeActorEventsResponseInvokeRequestAlpha1.newBuilder()
            .setId(id)
            .setActorType(actorType)
            .setActorId(actorId)
            .setMethod(method))
        .build();
  }

  static SubscribeActorEventsResponseAlpha1 deactivate(String id, String actorType, String actorId) {
    return SubscribeActorEventsResponseAlpha1.newBuilder()
        .setDeactivateRequest(SubscribeActorEventsResponseDeactivateRequestAlpha1.newBuilder()
            .setId(id)
            .setActorType(actorType)
            .setActorId(actorId))
        .build();
  }
}
