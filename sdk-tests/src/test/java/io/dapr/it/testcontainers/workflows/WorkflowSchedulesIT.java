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
 * limitations under the License.
 */

package io.dapr.it.testcontainers.workflows;

import io.dapr.config.Properties;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.dapr.workflows.client.WorkflowSchedule;
import io.dapr.workflows.client.WorkflowScheduleOptions;
import io.dapr.workflows.client.WorkflowSchedulePage;
import io.dapr.workflows.client.WorkflowScheduleState;
import io.dapr.workflows.client.WorkflowScheduleStatus;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_PLACEMENT_EDGE_IMAGE_TAG;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_PLACEMENT_IMAGE_TAG;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_EDGE_IMAGE_TAG;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_SCHEDULER_EDGE_IMAGE_TAG;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_SCHEDULER_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Workflow schedules integration test.
 *
 * <p>Workflow schedules are not in any Dapr release yet, so the whole stack runs the edge images built from
 * dapr/dapr master. The worker runs in the test JVM and the sidecar reaches it through the Testcontainers
 * host bridge, so every run of a schedule executes {@link ScheduleTestWorkflow} here.
 */
@Testcontainers
@Tag("testcontainers")
@Tag("dapr-head")
public class WorkflowSchedulesIT {

  private static final Duration POLL_TIMEOUT = Duration.ofSeconds(60);

  private static final DockerImageName EDGE_DAPRD_IMAGE = DockerImageName.parse(DAPR_RUNTIME_EDGE_IMAGE_TAG)
      .asCompatibleSubstituteFor(DAPR_RUNTIME_IMAGE_TAG);
  private static final DockerImageName EDGE_PLACEMENT_IMAGE = DockerImageName.parse(DAPR_PLACEMENT_EDGE_IMAGE_TAG)
      .asCompatibleSubstituteFor(DAPR_PLACEMENT_IMAGE_TAG);
  private static final DockerImageName EDGE_SCHEDULER_IMAGE = DockerImageName.parse(DAPR_SCHEDULER_EDGE_IMAGE_TAG)
      .asCompatibleSubstituteFor(DAPR_SCHEDULER_IMAGE_TAG);

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(EDGE_DAPRD_IMAGE)
      .withAppName("workflow-schedules-app")
      .withPlacementImage(EDGE_PLACEMENT_IMAGE)
      .withSchedulerImage(EDGE_SCHEDULER_IMAGE)
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal");

  private static DaprWorkflowClient client;
  private static WorkflowRuntime runtime;

  /**
   * Starts the client and the worker, then runs one instance so the worker is known to be connected before any
   * schedule fires.
   */
  @BeforeAll
  static void start() throws Exception {
    Properties properties = new Properties(Map.of(
        "dapr.grpc.endpoint", DAPR_CONTAINER.getGrpcEndpoint(),
        "dapr.http.endpoint", DAPR_CONTAINER.getHttpEndpoint()));
    client = new DaprWorkflowClient(properties);
    runtime = new WorkflowRuntimeBuilder(properties).registerWorkflow(ScheduleTestWorkflow.class).build();
    runtime.start(false);

    String instanceId = client.scheduleNewWorkflow(ScheduleTestWorkflow.class, "warm-up");
    client.waitForWorkflowCompletion(instanceId, POLL_TIMEOUT, false);
    client.purgeWorkflow(instanceId);
  }

  @AfterAll
  static void stop() throws Exception {
    client.close();
    runtime.close();
  }

  @Test
  void scheduledRunsCompleteTheSchedule() throws Exception {
    String name = scheduleName();
    client.createWorkflowSchedule(name, ScheduleTestWorkflow.class, "@every 1s",
        new WorkflowScheduleOptions().setInput("hello").setRepeats(2));

    WorkflowSchedule schedule = awaitSchedule(name, s -> s.getStatus().getState() == WorkflowScheduleState.COMPLETED
        && s.getStatus().getLastRun() != null
        && s.getStatus().getLastRun().getRuntimeStatus() == WorkflowRuntimeStatus.COMPLETED);

    WorkflowScheduleStatus status = schedule.getStatus();
    assertEquals(2, status.getRunCount());
    assertNull(status.getNextRunTime());
    assertNotNull(status.getLastRunTime());
    assertTrue(schedule.getSerializedInput().contains("hello"));
    assertTrue(status.getLastRun().getInstanceId().startsWith(name + "_"));
    assertTrue(status.getLastRun().getSerializedOutput().contains("hello"));

    List<String> runs = client.listInstanceIds(null, null, name + "_").getInstanceIds();
    assertEquals(2, runs.size());

    assertStatus(Status.Code.FAILED_PRECONDITION, () -> client.triggerWorkflowSchedule(name));

    runs.forEach(client::purgeWorkflow);
  }

  @Test
  void triggerStartsManualRun() throws Exception {
    String name = scheduleName();
    client.createWorkflowSchedule(name, ScheduleTestWorkflow.class, "@every 1h",
        new WorkflowScheduleOptions().setInput("manual"));

    String instanceId = client.triggerWorkflowSchedule(name);
    assertTrue(instanceId.startsWith(name + "_"));
    assertTrue(instanceId.endsWith("_manual"));
    client.waitForWorkflowCompletion(instanceId, POLL_TIMEOUT, false);

    WorkflowScheduleStatus status = awaitSchedule(name, s -> s.getStatus().getRunCount() == 1).getStatus();
    assertEquals(WorkflowScheduleState.ACTIVE, status.getState());

    client.createWorkflowSchedule(name, ScheduleTestWorkflow.class, "@every 1h",
        new WorkflowScheduleOptions().setInput("manual").setOverwrite(true));
    assertEquals(0, client.getWorkflowSchedule(name).getStatus().getRunCount());

    client.deleteWorkflowSchedule(name);
    client.purgeWorkflow(instanceId);
  }

  @Test
  void createOverwriteDeleteAndErrors() {
    String name = scheduleName();
    client.createWorkflowSchedule(name, ScheduleTestWorkflow.class, "@every 1h", null);

    assertStatus(Status.Code.ALREADY_EXISTS,
        () -> client.createWorkflowSchedule(name, ScheduleTestWorkflow.class, "@every 1h", null));

    client.createWorkflowSchedule(name, ScheduleTestWorkflow.class, "@every 2h",
        new WorkflowScheduleOptions().setOverwrite(true));
    assertEquals("@every 2h", client.getWorkflowSchedule(name).getSchedule());

    client.deleteWorkflowSchedule(name);
    assertStatus(Status.Code.NOT_FOUND, () -> client.getWorkflowSchedule(name));
    assertStatus(Status.Code.NOT_FOUND, () -> client.deleteWorkflowSchedule(name));

    assertStatus(Status.Code.INVALID_ARGUMENT,
        () -> client.createWorkflowSchedule(name, ScheduleTestWorkflow.class, "not a cron", null));
    assertStatus(Status.Code.INVALID_ARGUMENT,
        () -> client.createWorkflowSchedule("dapr-internal-schedule-x", ScheduleTestWorkflow.class, "@every 1h", null));
  }

  @Test
  void listWorkflowSchedulesPaginates() {
    List<String> names = List.of(scheduleName(), scheduleName(), scheduleName());
    for (String name : names) {
      client.createWorkflowSchedule(name, ScheduleTestWorkflow.class, "@every 1h", null);
    }

    Map<String, WorkflowSchedule> listed = new HashMap<>();
    String token = null;
    do {
      WorkflowSchedulePage page = client.listWorkflowSchedules(token, 2);
      page.getSchedules().forEach(s -> listed.put(s.getName(), s));
      token = page.getContinuationToken();
    } while (token != null);

    for (String name : names) {
      WorkflowSchedule schedule = listed.get(name);
      assertNotNull(schedule, name + " should be listed");
      assertEquals(WorkflowScheduleState.ACTIVE, schedule.getStatus().getState());
      assertNotNull(schedule.getStatus().getCreatedAt());
      assertNotNull(schedule.getStatus().getNextRunTime());
      client.deleteWorkflowSchedule(name);
    }
  }

  private static String scheduleName() {
    return "it-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private static WorkflowSchedule awaitSchedule(String name, Predicate<WorkflowSchedule> condition)
      throws InterruptedException {
    Instant deadline = Instant.now().plus(POLL_TIMEOUT);
    WorkflowSchedule schedule = client.getWorkflowSchedule(name);
    while (!condition.test(schedule) && Instant.now().isBefore(deadline)) {
      Thread.sleep(500);
      schedule = client.getWorkflowSchedule(name);
    }
    assertTrue(condition.test(schedule), "Timed out waiting on schedule " + name
        + ", last state: " + schedule.getStatus().getState() + ", runs: " + schedule.getStatus().getRunCount());
    return schedule;
  }

  private static void assertStatus(Status.Code code, Executable call) {
    StatusRuntimeException e = assertThrows(StatusRuntimeException.class, call);
    assertEquals(code, e.getStatus().getCode());
  }
}
