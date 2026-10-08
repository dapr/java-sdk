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

import io.dapr.actors.ActorId;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractActorScheduleTest {

  public interface ScheduleActor {
  }

  public static class ScheduleActorImpl extends AbstractActor implements ScheduleActor {
    public ScheduleActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
    }
  }

  private DaprClient daprClient;

  private ScheduleActorImpl actor;

  @BeforeEach
  public void setUp() {
    daprClient = mock(DaprClient.class);
    when(daprClient.registerTimer(any(), any(), any(), any())).thenReturn(Mono.empty());
    when(daprClient.registerReminder(any(), any(), any(), any())).thenReturn(Mono.empty());

    actor = createActor(new DefaultObjectSerializer());
  }

  private ScheduleActorImpl createActor(DaprObjectSerializer serializer) {
    ActorRuntimeContext<ScheduleActorImpl> context = new ActorRuntimeContext<>(
        mock(ActorRuntime.class),
        serializer,
        new DefaultActorFactory<>(),
        ActorTypeInformation.create(ScheduleActorImpl.class),
        daprClient,
        mock(DaprStateAsyncProvider.class));
    return new ScheduleActorImpl(context, new ActorId("1"));
  }

  @Test
  public void registerReminderWithOptions() {
    actor.registerReminder("myreminder", "state", ActorReminderOptions.builder()
        .setPeriod(Duration.ofSeconds(10))
        .setRepetitions(5)
        .setTtl(Duration.ofMinutes(1))
        .build()).block();

    ArgumentCaptor<ActorReminderParams> captor = ArgumentCaptor.forClass(ActorReminderParams.class);
    verify(daprClient).registerReminder(any(), eq("1"), eq("myreminder"), captor.capture());
    ActorReminderParams params = captor.getValue();
    assertNull(params.getDueTime());
    assertEquals(Duration.ofSeconds(10), params.getPeriod());
    assertEquals(5, params.getRepetitions());
    assertEquals(Duration.ofMinutes(1), params.getTtl());
  }

  @Test
  public void registerReminderWithDurationsKeepsBehavior() {
    actor.registerReminder("myreminder", "state", Duration.ofSeconds(1), Duration.ofSeconds(2)).block();

    ArgumentCaptor<ActorReminderParams> captor = ArgumentCaptor.forClass(ActorReminderParams.class);
    verify(daprClient).registerReminder(any(), eq("1"), eq("myreminder"), captor.capture());
    ActorReminderParams params = captor.getValue();
    assertEquals(Duration.ofSeconds(1), params.getDueTime());
    assertEquals(Duration.ofSeconds(2), params.getPeriod());
    assertNull(params.getRepetitions());
    assertNull(params.getTtl());
  }

  @Test
  public void registerReminderWithInvalidRepetitions() {
    assertThrows(IllegalArgumentException.class, () -> actor.registerReminder("myreminder", "state",
        ActorReminderOptions.builder().setRepetitions(5).build()));
  }

  @Test
  public void registerReminderWithTtlWithoutPeriod() {
    assertThrows(IllegalArgumentException.class, () -> actor.registerReminder("myreminder", "state",
        ActorReminderOptions.builder().setTtl(Duration.ofMinutes(1)).build()));

    Mono<String> timer = actor.registerActorTimer("mytimer", "callback", "state",
        ActorTimerOptions.builder().setTtl(Duration.ofMinutes(1)).build());
    assertThrows(IllegalArgumentException.class, timer::block);
  }

  @Test
  public void registerReminderWithoutOptions() {
    assertThrows(IllegalArgumentException.class,
        () -> actor.registerReminder("myreminder", "state", (ActorReminderOptions) null));
  }

  @Test
  public void registerTimerWithoutCallback() {
    Mono<String> result = actor.registerActorTimer("mytimer", "", "state", ActorTimerOptions.builder().build());
    assertThrows(IllegalArgumentException.class, result::block);
  }

  @Test
  public void registerReminderWithSerializationError() throws Exception {
    DaprObjectSerializer serializer = mock(DaprObjectSerializer.class);
    when(serializer.serialize(any())).thenThrow(new IOException("boom"));
    ScheduleActorImpl failingActor = createActor(serializer);

    Mono<Void> result = failingActor.registerReminder("myreminder", "state", ActorReminderOptions.builder().build());
    assertThrows(IOException.class, () -> {
      try {
        result.block();
      } catch (RuntimeException e) {
        throw e.getCause();
      }
    });
  }

  @Test
  public void registerTimerWithGeneratedName() {
    String emptyName = actor.registerActorTimer("", "callback", "state", ActorTimerOptions.builder().build()).block();
    assertTrue(emptyName.startsWith("1_Timer_"));

    String name = actor.registerActorTimer(null, "callback", "state", ActorTimerOptions.builder().build()).block();

    assertTrue(name.startsWith("1_Timer_"));
    ArgumentCaptor<ActorTimerParams> captor = ArgumentCaptor.forClass(ActorTimerParams.class);
    verify(daprClient).registerTimer(any(), eq("1"), eq(name), captor.capture());
    assertNotEquals(emptyName, name);
    assertNull(captor.getValue().getDueTime());
    assertNull(captor.getValue().getPeriod());
  }

  @Test
  public void registerTimerWithOptions() {
    String name = actor.registerActorTimer("mytimer", "callback", "state", ActorTimerOptions.builder()
        .setDueTime(Duration.ofSeconds(1))
        .setPeriod(Duration.ofSeconds(2))
        .setRepetitions(3)
        .setTtl(Duration.ofSeconds(30))
        .build()).block();

    assertEquals("mytimer", name);
    ArgumentCaptor<ActorTimerParams> captor = ArgumentCaptor.forClass(ActorTimerParams.class);
    verify(daprClient).registerTimer(any(), eq("1"), eq("mytimer"), captor.capture());
    ActorTimerParams params = captor.getValue();
    assertEquals("callback", params.getCallback());
    assertEquals(Duration.ofSeconds(1), params.getDueTime());
    assertEquals(Duration.ofSeconds(2), params.getPeriod());
    assertEquals(3, params.getRepetitions());
    assertEquals(Duration.ofSeconds(30), params.getTtl());
  }

  @Test
  public void registerTimerWithInvalidOptions() {
    Mono<String> result = actor.registerActorTimer("mytimer", "callback", "state",
        ActorTimerOptions.builder().setRepetitions(0).setPeriod(Duration.ofSeconds(1)).build());
    assertThrows(IllegalArgumentException.class, result::block);
    assertThrows(IllegalArgumentException.class,
        () -> actor.registerActorTimer("mytimer", "callback", "state", (ActorTimerOptions) null).block());
  }
}
