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

package io.dapr.springboot;

import io.dapr.actors.runtime.ActorRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DaprControllerTest {

  private static final byte[] BODY = "{}".getBytes();

  private Field instanceField;

  private Object previousInstance;

  private ActorRuntime actorRuntime;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() throws Exception {
    instanceField = ActorRuntime.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    previousInstance = instanceField.get(null);
    actorRuntime = mock(ActorRuntime.class);
    instanceField.set(null, actorRuntime);
    mockMvc = MockMvcBuilders.standaloneSetup(new DaprController()).build();

    when(actorRuntime.invoke(any(), any(), any(), any(), any())).thenReturn(Mono.just(new byte[0]));
    when(actorRuntime.invokeTimer(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
    when(actorRuntime.invokeReminder(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
    when(actorRuntime.deactivate(any(), any())).thenReturn(Mono.empty());
  }

  @AfterEach
  public void tearDown() throws Exception {
    instanceField.set(null, previousInstance);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Dapr-Actor-Activation-Id", "dapr-actor-activation-id", "DAPR-ACTOR-ACTIVATION-ID"})
  public void invokeActorMethodPassesActivationId(String header) throws Exception {
    perform(put("/actors/MyActor/1/method/say").header(header, "act-1").content(BODY));

    verify(actorRuntime).invoke("MyActor", "1", "say", BODY, "act-1");
  }

  @Test
  public void invokeActorMethodWithoutActivationIdPassesNull() throws Exception {
    perform(put("/actors/MyActor/1/method/say").content(BODY));

    verify(actorRuntime).invoke(any(), any(), any(), any(), isNull());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Dapr-Actor-Activation-Id", "dapr-actor-activation-id"})
  public void invokeActorTimerPassesActivationId(String header) throws Exception {
    perform(put("/actors/MyActor/1/method/timer/t1").header(header, "act-2").content(BODY));

    verify(actorRuntime).invokeTimer("MyActor", "1", "t1", BODY, "act-2");
  }

  @Test
  public void invokeActorTimerWithoutActivationIdPassesNull() throws Exception {
    perform(put("/actors/MyActor/1/method/timer/t1").content(BODY));

    verify(actorRuntime).invokeTimer(any(), any(), any(), any(), isNull());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Dapr-Actor-Activation-Id", "dapr-actor-activation-id"})
  public void invokeActorReminderPassesActivationId(String header) throws Exception {
    perform(put("/actors/MyActor/1/method/remind/r1").header(header, "act-3").content(BODY));

    verify(actorRuntime).invokeReminder("MyActor", "1", "r1", BODY, "act-3");
  }

  @Test
  public void invokeActorReminderWithoutActivationIdPassesNull() throws Exception {
    perform(put("/actors/MyActor/1/method/remind/r1").content(BODY));

    verify(actorRuntime).invokeReminder(any(), any(), any(), any(), isNull());
  }

  @Test
  public void deactivateActorIsUnchanged() throws Exception {
    perform(delete("/actors/MyActor/1").header("Dapr-Actor-Activation-Id", "act-4"));

    verify(actorRuntime).deactivate("MyActor", "1");
  }

  @Test
  public void legacyOverloadsPassNullActivationId() {
    DaprController controller = new DaprController();

    controller.invokeActorMethod("MyActor", "1", "say", BODY).block();
    controller.invokeActorTimer("MyActor", "1", "t1", BODY).block();
    controller.invokeActorReminder("MyActor", "1", "r1", BODY).block();

    verify(actorRuntime).invoke("MyActor", "1", "say", BODY, null);
    verify(actorRuntime).invokeTimer("MyActor", "1", "t1", BODY, null);
    verify(actorRuntime).invokeReminder("MyActor", "1", "r1", BODY, null);
  }

  private void perform(RequestBuilder requestBuilder) throws Exception {
    MvcResult result = mockMvc.perform(requestBuilder).andExpect(request().asyncStarted()).andReturn();
    mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
  }
}
