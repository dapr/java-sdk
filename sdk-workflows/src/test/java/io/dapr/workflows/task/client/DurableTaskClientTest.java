/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.workflows.task.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

/**
 * Covers the convenience overloads {@link DurableTaskClient} implements on top of its abstract
 * methods: the short forms must delegate to the full ones, and the app ID forms must refuse a
 * cross-app target on an implementation that does not support routing.
 */
public class DurableTaskClientTest {

  private static final String INSTANCE_ID = "instance-1";
  private static final String ORCHESTRATOR = "DemoWorkflow";
  private static final String OTHER_APP = "other-app";
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private DurableTaskClient client;

  @BeforeEach
  public void setUp() {
    client = mock(DurableTaskClient.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  public void shouldCloseWithoutDoingAnything() {
    assertDoesNotThrow(() -> client.close());
  }

  @Test
  public void shouldScheduleWithNoInputAsEmptyOptions() {
    client.scheduleNewOrchestrationInstance(ORCHESTRATOR);

    ArgumentCaptor<NewOrchestrationInstanceOptions> captor =
        ArgumentCaptor.forClass(NewOrchestrationInstanceOptions.class);
    verify(client).scheduleNewOrchestrationInstance(eq(ORCHESTRATOR), captor.capture());

    assertNull(captor.getValue().getInput());
    assertNull(captor.getValue().getInstanceId());
  }

  @Test
  public void shouldCarryInputAndInstanceIdIntoTheOptions() {
    client.scheduleNewOrchestrationInstance(ORCHESTRATOR, "the input", INSTANCE_ID);

    ArgumentCaptor<NewOrchestrationInstanceOptions> captor =
        ArgumentCaptor.forClass(NewOrchestrationInstanceOptions.class);
    verify(client).scheduleNewOrchestrationInstance(eq(ORCHESTRATOR), captor.capture());

    assertEquals("the input", captor.getValue().getInput());
    assertEquals(INSTANCE_ID, captor.getValue().getInstanceId());
  }

  @Test
  public void shouldRaiseAnEventWithNoPayloadAsANullPayload() {
    client.raiseEvent(INSTANCE_ID, "Approval");

    verify(client).raiseEvent(INSTANCE_ID, "Approval", null);
  }

  @Test
  public void shouldWaitForStartWithTheInputsAndOutputsAsked() throws TimeoutException {
    client.waitForInstanceStart(INSTANCE_ID, TIMEOUT);

    verify(client).waitForInstanceStart(INSTANCE_ID, TIMEOUT, false);
  }

  @Test
  public void shouldSuspendAndResumeWithNoReasonAsANullReason() {
    client.suspendInstance(INSTANCE_ID);
    client.resumeInstance(INSTANCE_ID);

    verify(client).suspendInstance(INSTANCE_ID, null);
    verify(client).resumeInstance(INSTANCE_ID, null);
  }

  @Test
  public void shouldTargetTheLocalAppWhenTheAppIdIsNullOrEmpty() throws TimeoutException {
    client.getInstanceMetadata(INSTANCE_ID, true, null);
    client.raiseEvent(INSTANCE_ID, "Approval", "payload", "");
    client.terminate(INSTANCE_ID, "output", null);
    client.purgeInstance(INSTANCE_ID, null);
    client.restartInstance(INSTANCE_ID, true, null);
    client.suspendInstance(INSTANCE_ID, "why", null);
    client.resumeInstance(INSTANCE_ID, "why", null);
    client.waitForInstanceStart(INSTANCE_ID, TIMEOUT, true, null);

    verify(client).getInstanceMetadata(INSTANCE_ID, true);
    verify(client).raiseEvent(INSTANCE_ID, "Approval", "payload");
    verify(client).terminate(INSTANCE_ID, "output");
    verify(client).purgeInstance(INSTANCE_ID);
    verify(client).restartInstance(INSTANCE_ID, true);
    verify(client).suspendInstance(INSTANCE_ID, "why");
    verify(client).resumeInstance(INSTANCE_ID, "why");
    verify(client).waitForInstanceStart(INSTANCE_ID, TIMEOUT, true);
  }

  @Test
  public void shouldRefuseACrossAppTargetRatherThanSilentlyUseTheLocalApp() {
    UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
        () -> client.getInstanceMetadata(INSTANCE_ID, true, OTHER_APP));
    assertTrue(thrown.getMessage().contains("cross-app"), thrown.getMessage());

    assertThrows(UnsupportedOperationException.class,
        () -> client.raiseEvent(INSTANCE_ID, "Approval", "payload", OTHER_APP));
    assertThrows(UnsupportedOperationException.class,
        () -> client.terminate(INSTANCE_ID, "output", OTHER_APP));
    assertThrows(UnsupportedOperationException.class,
        () -> client.purgeInstance(INSTANCE_ID, OTHER_APP));
    assertThrows(UnsupportedOperationException.class,
        () -> client.restartInstance(INSTANCE_ID, true, OTHER_APP));
    assertThrows(UnsupportedOperationException.class,
        () -> client.suspendInstance(INSTANCE_ID, "why", OTHER_APP));
    assertThrows(UnsupportedOperationException.class,
        () -> client.resumeInstance(INSTANCE_ID, "why", OTHER_APP));
    assertThrows(UnsupportedOperationException.class,
        () -> client.waitForInstanceStart(INSTANCE_ID, TIMEOUT, true, OTHER_APP));
    assertThrows(UnsupportedOperationException.class,
        () -> client.waitForInstanceCompletion(INSTANCE_ID, TIMEOUT, true, OTHER_APP));
  }
}
