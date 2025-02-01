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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class ActorRuntimeTest {

  private static final String ACTOR_NAME = "MyGreatActor";

  public interface MyActor {
    String say();

    int count();
  }

  @ActorType(name = ACTOR_NAME)
  public static class MyActorImpl extends AbstractActor implements MyActor {

    private int count = 0;

    private Boolean activated;

    public MyActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
    }

    public Mono<Void> onActivate() {
      return Mono.fromRunnable(() -> {
        if (this.activated != null) {
          throw new IllegalStateException("already activated once");
        }

        this.activated = true;
      });
    }

    public Mono<Void> onDeactivate() {
      return Mono.fromRunnable(() -> {
        if (this.activated == null) {
          throw new IllegalStateException("never activated");
        }

        if (this.activated == false) {
          throw new IllegalStateException("already deactivated");
        }

        if (this.count == 0) {
          throw new IllegalStateException("test expects a call before deactivate");
        }

        this.activated = false;
      });
    }

    public String say() {
      if (!this.activated) {
        throw new IllegalStateException("not activated");
      }

      this.count++;
      return "Nothing to say.";
    }

    public int count() {
      return this.count;
    }
  }

  private static final ActorObjectSerializer ACTOR_STATE_SERIALIZER = new ActorObjectSerializer();

  private static Constructor<ActorRuntime> constructor;

  private ActorRuntime runtime;

  @BeforeAll
  public static void beforeAll() throws Exception {
    constructor =
            (Constructor<ActorRuntime>) Arrays.stream(ActorRuntime.class.getDeclaredConstructors())
                    .filter(c -> c.getParameters().length == 2).map(c -> {
                      c.setAccessible(true);
                      return c;
                    }).findFirst().get();
  }

  @BeforeEach
  public void setup() throws Exception {
    this.runtime = constructor.newInstance(null, mock(DaprClient.class));
  }

  @Test
  public void registerActorNullClass() {
    assertThrows(IllegalArgumentException.class, () -> this.runtime.registerActor(null));
  }

  @Test
  public void registerActorNullFactory() {
    assertThrows(IllegalArgumentException.class, () -> this.runtime.registerActor(MyActorImpl.class, null, new DefaultObjectSerializer(),
        new DefaultObjectSerializer()));
  }

  @Test
  public void registerActorNullSerializer() {
    assertThrows(IllegalArgumentException.class, () -> this.runtime.registerActor(MyActorImpl.class, new DefaultActorFactory<>(), null,
        new DefaultObjectSerializer()));
  }

  @Test
  public void registerActorNullStateSerializer() {
    assertThrows(IllegalArgumentException.class, () -> this.runtime.registerActor(MyActorImpl.class, new DefaultActorFactory<>(),
        new DefaultObjectSerializer(), null));
  }

  @Test
  public void setActorIdleTimeout() throws Exception {
    this.runtime.getConfig().setActorIdleTimeout(Duration.ofSeconds(123));
    assertEquals("{\"entities\":[],\"actorIdleTimeout\":\"0h2m3s0ms\"}",
        new String(this.runtime.serializeConfig()));
  }

  @Test
  public void setActorScanInterval() throws Exception {
    this.runtime.getConfig().setActorScanInterval(Duration.ofSeconds(123));
    assertEquals("{\"entities\":[],\"actorScanInterval\":\"0h2m3s0ms\"}",
        new String(this.runtime.serializeConfig()));
  }

  @Test
  public void setDrainBalancedActors() throws Exception {
    this.runtime.getConfig().setDrainBalancedActors(true);
    assertEquals("{\"entities\":[],\"drainBalancedActors\":true}",
        new String(this.runtime.serializeConfig()));
  }

  @Test
  public void addActorTypeConfig() throws Exception {
    ActorTypeConfig actorTypeConfig1 = new ActorTypeConfig();
    actorTypeConfig1.setActorTypeName("actor1");
    actorTypeConfig1.setActorIdleTimeout(Duration.ofSeconds(123));
    actorTypeConfig1.setActorScanInterval(Duration.ofSeconds(123));
    actorTypeConfig1.setDrainOngoingCallTimeout(Duration.ofSeconds(123));
    actorTypeConfig1.setDrainBalancedActors(true);
    actorTypeConfig1.setRemindersStoragePartitions(1);
    this.runtime.getConfig().addActorTypeConfig(actorTypeConfig1);
    this.runtime.getConfig().addRegisteredActorType("actor1");

    ActorTypeConfig actorTypeConfig2 = new ActorTypeConfig();
    actorTypeConfig2.setActorTypeName("actor2");
    actorTypeConfig2.setActorIdleTimeout(Duration.ofSeconds(123));
    actorTypeConfig2.setActorScanInterval(Duration.ofSeconds(123));
    actorTypeConfig2.setDrainOngoingCallTimeout(Duration.ofSeconds(123));
    actorTypeConfig2.setDrainBalancedActors(false);
    actorTypeConfig2.setRemindersStoragePartitions(2);
    this.runtime.getConfig().addActorTypeConfig(actorTypeConfig2);
    this.runtime.getConfig().addRegisteredActorType("actor2");

    assertEquals(
            "{\"entities\":[\"actor1\",\"actor2\"],\"entitiesConfig\":[{\"entities\":[\"actor1\"],\"actorIdleTimeout\":\"0h2m3s0ms\",\"actorScanInterval\":\"0h2m3s0ms\",\"drainOngoingCallTimeout\":\"0h2m3s0ms\",\"drainBalancedActors\":true,\"remindersStoragePartitions\":1},{\"entities\":[\"actor2\"],\"actorIdleTimeout\":\"0h2m3s0ms\",\"actorScanInterval\":\"0h2m3s0ms\",\"drainOngoingCallTimeout\":\"0h2m3s0ms\",\"drainBalancedActors\":false,\"remindersStoragePartitions\":2}]}",
            new String(this.runtime.serializeConfig())
    );
  }

  @Test
  public void addNullActorTypeConfig() throws Exception {
    try {
      this.runtime.getConfig().addActorTypeConfig(null);
    } catch (Exception ex) {
      assertInstanceOf(IllegalArgumentException.class, ex);
      assertTrue(ex.getMessage().contains("Add actor type config failed."));
    }
    try {
      this.runtime.getConfig().addRegisteredActorType(null);
    } catch (Exception ex) {
      assertInstanceOf(IllegalArgumentException.class, ex);
      assertTrue(ex.getMessage().contains("Registered actor must have a type name."));
    }
  }

  @Test
  public void setDrainOngoingCallTimeout() throws Exception {
    this.runtime.getConfig().setDrainOngoingCallTimeout(Duration.ofSeconds(123));
    assertEquals("{\"entities\":[],\"drainOngoingCallTimeout\":\"0h2m3s0ms\"}",
        new String(this.runtime.serializeConfig()));
  }

  @Test
  public void setRemindersStoragePartitions() throws Exception {
    this.runtime.getConfig().setRemindersStoragePartitions(12);
    assertEquals("{\"entities\":[],\"remindersStoragePartitions\":12}",
        new String(this.runtime.serializeConfig()));
  }

  @Test
  public void invokeActor() throws Exception {
    String actorId = UUID.randomUUID().toString();
    this.runtime.registerActor(MyActorImpl.class);

    byte[] response = this.runtime.invoke(ACTOR_NAME, actorId, "say", null).block();
    String message = ACTOR_STATE_SERIALIZER.deserialize(response, String.class);
    assertEquals("Nothing to say.", message);
  }

  @Test
  public void invokeUnknownActor() {
    String actorId = UUID.randomUUID().toString();
    this.runtime.registerActor(MyActorImpl.class);

    assertThrows(IllegalArgumentException.class, () -> this.runtime.invoke("UnknownActor", actorId, "say", null).block());
  }

  @Test
  public void deactivateActor() throws Exception {
    String actorId = UUID.randomUUID().toString();
    this.runtime.registerActor(MyActorImpl.class);
    this.runtime.deactivate(ACTOR_NAME, actorId).block();
  }

  @Test
  public void lazyDeactivate() throws Exception {
    String actorId = UUID.randomUUID().toString();
    this.runtime.registerActor(MyActorImpl.class);

    Mono<Void> deactivateCall = this.runtime.deactivate(ACTOR_NAME, actorId);

    this.runtime.invoke(ACTOR_NAME, actorId, "say", null).block();

    deactivateCall.block();

    this.runtime.invoke(ACTOR_NAME, actorId, "say", null)
        .doOnError(e -> assertTrue(e.getMessage().contains("Could not find actor")))
        .doOnSuccess(s -> fail()).onErrorReturn("".getBytes()).block();
  }

  @Test
  public void lazyInvoke() throws Exception {
    String actorId = UUID.randomUUID().toString();
    this.runtime.registerActor(MyActorImpl.class, new DefaultActorFactory<>());

    Mono<byte[]> invokeCall = this.runtime.invoke(ACTOR_NAME, actorId, "say", null);

    byte[] response = this.runtime.invoke(ACTOR_NAME, actorId, "count", null).block();
    int count = ACTOR_STATE_SERIALIZER.deserialize(response, Integer.class);
    assertEquals(0, count);

    invokeCall.block();

    response = this.runtime.invoke(ACTOR_NAME, actorId, "count", null).block();
    count = ACTOR_STATE_SERIALIZER.deserialize(response, Integer.class);
    assertEquals(1, count);
  }

}
