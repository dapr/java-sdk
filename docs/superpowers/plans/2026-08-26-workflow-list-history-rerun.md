# Workflow List / History / Rerun Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `listInstanceIds`, `getInstanceHistory`, and `rerunWorkflowFromEvent` to the Dapr Java SDK `DaprWorkflowClient`, so users call these workflow-runtime operations without using the gRPC channel directly.

**Architecture:** Three layers, top to bottom: `io.dapr.workflows.client.DaprWorkflowClient` (user-facing, module `sdk-workflows`) → `io.dapr.durabletask.DurableTaskClient` (abstract, module `durabletask-client`) → `io.dapr.durabletask.DurableTaskGrpcClient` (impl, wraps the generated `TaskHubSidecarServiceGrpc` blocking stub). The generated stub already has all three RPCs, so no proto change is needed. The durabletask layer returns raw generated proto; `DaprWorkflowClient` converts proto to new public model types with a package-private converter.

**Tech Stack:** Java 11, Maven, gRPC (grpc-netty), protobuf-java, JUnit 5, Mockito. Reference implementation: dapr/dotnet-sdk PR #1738.

**Spec:** `docs/superpowers/specs/2026-08-26-workflow-list-history-rerun-design.md`

## Global Constraints

- **License header:** Every new `.java` file starts with this exact header (copy verbatim, including the last two lines which are intentionally not indented — the header check requires an exact match):
  ```
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
  ```
- **Checkstyle:** 2-space indentation, 120-column line limit. Every public class, method, and enum constant needs javadoc. Run checkstyle as part of the module build.
- **Proto download caveat:** The durabletask proto is fetched at build time from `raw.githubusercontent.com`, which is blocked in this sandbox. The proto and generated sources are **already present** under `durabletask-client/target/`. The download plugin skips the download when the file already exists. **Do not run `mvn clean` on `durabletask-client`** — that deletes the downloaded proto and the next build will fail on the blocked domain. Use `install`/`test` without `clean`.
- **Commits:** Every commit uses `git commit -s` (sign-off). Do not add any `Co-Authored-By` trailer.
- **Build commands use `-am`:** module builds below use `mvn -pl <module> -am ...` so the `1.19.0-SNAPSHOT` sibling modules (`dapr-sdk`, `durabletask-client`) build/resolve from source. Combined with the no-clean rule above, `-am` is safe. If a command still fails on dependency resolution, run `mvn -q -pl durabletask-client -am install -DskipTests` once first.
- **Method names** mirror the dotnet SDK and the durabletask RPCs. **Enum values** use java-sdk vocabulary in `UPPER_SNAKE_CASE` (matching `WorkflowRuntimeStatus`).
- **Package for new public types:** `io.dapr.workflows.client` (module `sdk-workflows`).
- **Nullability:** use `javax.annotation.Nullable` on optional params/returns (already a `provided` dependency).

---

### Task 1: Public model types

**Files:**
- Create: `sdk-workflows/src/main/java/io/dapr/workflows/client/WorkflowInstancePage.java`
- Create: `sdk-workflows/src/main/java/io/dapr/workflows/client/WorkflowHistoryEventType.java`
- Create: `sdk-workflows/src/main/java/io/dapr/workflows/client/WorkflowHistoryEvent.java`
- Create: `sdk-workflows/src/main/java/io/dapr/workflows/client/RerunWorkflowFromEventOptions.java`
- Test: `sdk-workflows/src/test/java/io/dapr/workflows/client/WorkflowInstancePageTest.java`
- Test: `sdk-workflows/src/test/java/io/dapr/workflows/client/WorkflowHistoryEventTest.java`
- Test: `sdk-workflows/src/test/java/io/dapr/workflows/client/RerunWorkflowFromEventOptionsTest.java`

**Interfaces:**
- Consumes: nothing (leaf value types).
- Produces (later tasks rely on these exact signatures):
  - `WorkflowInstancePage(List<String> instanceIds, @Nullable String continuationToken)`; `List<String> getInstanceIds()`; `@Nullable String getContinuationToken()`.
  - `enum WorkflowHistoryEventType { UNKNOWN, EXECUTION_STARTED, EXECUTION_COMPLETED, EXECUTION_TERMINATED, TASK_SCHEDULED, TASK_COMPLETED, TASK_FAILED, CHILD_WORKFLOW_INSTANCE_CREATED, CHILD_WORKFLOW_INSTANCE_COMPLETED, CHILD_WORKFLOW_INSTANCE_FAILED, TIMER_CREATED, TIMER_FIRED, WORKFLOW_STARTED, WORKFLOW_COMPLETED, EVENT_SENT, EVENT_RAISED, CONTINUE_AS_NEW, EXECUTION_SUSPENDED, EXECUTION_RESUMED, EXECUTION_STALLED }`.
  - `WorkflowHistoryEvent(int eventId, WorkflowHistoryEventType eventType, Instant timestamp)`; `int getEventId()`; `WorkflowHistoryEventType getEventType()`; `Instant getTimestamp()`.
  - `RerunWorkflowFromEventOptions` with fluent `setNewInstanceId(String)`, `setInput(Object)`, `setOverwriteInput(boolean)` (each returns `this`); `@Nullable String getNewInstanceId()`; `@Nullable Object getInput()`; `boolean isOverwriteInput()`.

- [ ] **Step 1: Write the failing tests**

`WorkflowInstancePageTest.java` (add the license header, then):
```java
package io.dapr.workflows.client;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WorkflowInstancePageTest {

  @Test
  public void exposesInstanceIdsAndToken() {
    WorkflowInstancePage page = new WorkflowInstancePage(Arrays.asList("a", "b"), "next");
    assertEquals(Arrays.asList("a", "b"), page.getInstanceIds());
    assertEquals("next", page.getContinuationToken());
  }

  @Test
  public void allowsNullContinuationToken() {
    WorkflowInstancePage page = new WorkflowInstancePage(Arrays.asList("a"), null);
    assertNull(page.getContinuationToken());
  }

  @Test
  public void instanceIdsListIsUnmodifiable() {
    WorkflowInstancePage page = new WorkflowInstancePage(Arrays.asList("a"), null);
    assertThrows(UnsupportedOperationException.class, () -> page.getInstanceIds().add("b"));
  }
}
```

`WorkflowHistoryEventTest.java` (license header, then):
```java
package io.dapr.workflows.client;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkflowHistoryEventTest {

  @Test
  public void exposesFields() {
    Instant now = Instant.ofEpochSecond(1000, 5);
    WorkflowHistoryEvent event = new WorkflowHistoryEvent(3, WorkflowHistoryEventType.TASK_SCHEDULED, now);
    assertEquals(3, event.getEventId());
    assertEquals(WorkflowHistoryEventType.TASK_SCHEDULED, event.getEventType());
    assertEquals(now, event.getTimestamp());
  }
}
```

`RerunWorkflowFromEventOptionsTest.java` (license header, then):
```java
package io.dapr.workflows.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

public class RerunWorkflowFromEventOptionsTest {

  @Test
  public void fluentSettersReturnSameInstanceAndStoreValues() {
    RerunWorkflowFromEventOptions options = new RerunWorkflowFromEventOptions();
    assertSame(options, options.setNewInstanceId("target"));
    assertSame(options, options.setInput("payload"));
    assertSame(options, options.setOverwriteInput(true));

    assertEquals("target", options.getNewInstanceId());
    assertEquals("payload", options.getInput());
    assertEquals(true, options.isOverwriteInput());
  }

  @Test
  public void overwriteInputDefaultsToFalse() {
    assertFalse(new RerunWorkflowFromEventOptions().isOverwriteInput());
  }
}
```

- [ ] **Step 2: Run the tests to verify they fail to compile**

Run: `mvn -q -pl sdk-workflows -am test-compile`
Expected: FAIL — the model classes do not exist yet.

- [ ] **Step 3: Create the model classes**

`WorkflowInstancePage.java` (license header, then):
```java
package io.dapr.workflows.client;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a page of workflow instance IDs returned by a list operation.
 */
public final class WorkflowInstancePage {

  private final List<String> instanceIds;
  @Nullable
  private final String continuationToken;

  /**
   * Constructs a page of workflow instance IDs.
   *
   * @param instanceIds       the workflow instance IDs in this page; must not be null
   * @param continuationToken the token used to retrieve the next page, or null if there are no more pages
   */
  public WorkflowInstancePage(List<String> instanceIds, @Nullable String continuationToken) {
    this.instanceIds = Collections.unmodifiableList(new ArrayList<>(instanceIds));
    this.continuationToken = continuationToken;
  }

  /**
   * Gets the workflow instance IDs in this page.
   *
   * @return an unmodifiable list of instance IDs
   */
  public List<String> getInstanceIds() {
    return this.instanceIds;
  }

  /**
   * Gets the continuation token for the next page.
   *
   * @return the continuation token, or null if there are no more pages
   */
  @Nullable
  public String getContinuationToken() {
    return this.continuationToken;
  }
}
```

`WorkflowHistoryEventType.java` (license header, then):
```java
package io.dapr.workflows.client;

/**
 * Represents the type of a workflow history event.
 */
public enum WorkflowHistoryEventType {
  /** Unknown or unmapped event type. */
  UNKNOWN,
  /** The workflow execution started. */
  EXECUTION_STARTED,
  /** The workflow execution completed. */
  EXECUTION_COMPLETED,
  /** The workflow execution was terminated. */
  EXECUTION_TERMINATED,
  /** An activity task was scheduled. */
  TASK_SCHEDULED,
  /** An activity task completed successfully. */
  TASK_COMPLETED,
  /** An activity task failed. */
  TASK_FAILED,
  /** A child workflow instance was created. */
  CHILD_WORKFLOW_INSTANCE_CREATED,
  /** A child workflow instance completed. */
  CHILD_WORKFLOW_INSTANCE_COMPLETED,
  /** A child workflow instance failed. */
  CHILD_WORKFLOW_INSTANCE_FAILED,
  /** A timer was created. */
  TIMER_CREATED,
  /** A timer fired. */
  TIMER_FIRED,
  /** The workflow started processing a work item. */
  WORKFLOW_STARTED,
  /** The workflow completed processing a work item. */
  WORKFLOW_COMPLETED,
  /** An event was sent to another instance. */
  EVENT_SENT,
  /** An external event was raised. */
  EVENT_RAISED,
  /** The workflow continued as new. */
  CONTINUE_AS_NEW,
  /** The workflow execution was suspended. */
  EXECUTION_SUSPENDED,
  /** The workflow execution was resumed. */
  EXECUTION_RESUMED,
  /** The workflow execution stalled. */
  EXECUTION_STALLED
}
```

`WorkflowHistoryEvent.java` (license header, then):
```java
package io.dapr.workflows.client;

import java.time.Instant;

/**
 * Represents a single event in a workflow instance's execution history.
 */
public final class WorkflowHistoryEvent {

  private final int eventId;
  private final WorkflowHistoryEventType eventType;
  private final Instant timestamp;

  /**
   * Constructs a workflow history event.
   *
   * @param eventId   the event ID within the workflow instance history
   * @param eventType the type of history event
   * @param timestamp the time the event occurred
   */
  public WorkflowHistoryEvent(int eventId, WorkflowHistoryEventType eventType, Instant timestamp) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.timestamp = timestamp;
  }

  /**
   * Gets the event ID within the workflow instance history.
   *
   * @return the event ID
   */
  public int getEventId() {
    return this.eventId;
  }

  /**
   * Gets the type of this history event.
   *
   * @return the event type
   */
  public WorkflowHistoryEventType getEventType() {
    return this.eventType;
  }

  /**
   * Gets the time this event occurred.
   *
   * @return the event timestamp
   */
  public Instant getTimestamp() {
    return this.timestamp;
  }
}
```

`RerunWorkflowFromEventOptions.java` (license header, then):
```java
package io.dapr.workflows.client;

import javax.annotation.Nullable;

/**
 * Options for the {@link DaprWorkflowClient#rerunWorkflowFromEvent(String, int, RerunWorkflowFromEventOptions)}
 * operation.
 */
public final class RerunWorkflowFromEventOptions {

  @Nullable
  private String newInstanceId;
  @Nullable
  private Object input;
  private boolean overwriteInput;

  /**
   * Sets the instance ID to use for the new workflow instance. When not set, a random ID is generated.
   *
   * @param newInstanceId the new instance ID
   * @return this {@link RerunWorkflowFromEventOptions} object
   */
  public RerunWorkflowFromEventOptions setNewInstanceId(String newInstanceId) {
    this.newInstanceId = newInstanceId;
    return this;
  }

  /**
   * Sets the input applied at the next activity event of the rerun instance. When set,
   * {@link #setOverwriteInput(boolean)} must also be set to true.
   *
   * @param input the input to apply
   * @return this {@link RerunWorkflowFromEventOptions} object
   */
  public RerunWorkflowFromEventOptions setInput(Object input) {
    this.input = input;
    return this;
  }

  /**
   * Sets whether the input at the rerun point is overwritten with {@link #setInput(Object)}.
   *
   * @param overwriteInput true to overwrite the input
   * @return this {@link RerunWorkflowFromEventOptions} object
   */
  public RerunWorkflowFromEventOptions setOverwriteInput(boolean overwriteInput) {
    this.overwriteInput = overwriteInput;
    return this;
  }

  /**
   * Gets the new instance ID.
   *
   * @return the new instance ID, or null if not set
   */
  @Nullable
  public String getNewInstanceId() {
    return this.newInstanceId;
  }

  /**
   * Gets the input to apply at the rerun point.
   *
   * @return the input, or null if not set
   */
  @Nullable
  public Object getInput() {
    return this.input;
  }

  /**
   * Gets whether the input at the rerun point is overwritten.
   *
   * @return true if the input is overwritten
   */
  public boolean isOverwriteInput() {
    return this.overwriteInput;
  }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn -q -pl sdk-workflows -am test -Dtest=WorkflowInstancePageTest,WorkflowHistoryEventTest,RerunWorkflowFromEventOptionsTest`
Expected: PASS, all three test classes green, checkstyle clean.

- [ ] **Step 5: Commit**

```bash
git add sdk-workflows/src/main/java/io/dapr/workflows/client/WorkflowInstancePage.java \
        sdk-workflows/src/main/java/io/dapr/workflows/client/WorkflowHistoryEventType.java \
        sdk-workflows/src/main/java/io/dapr/workflows/client/WorkflowHistoryEvent.java \
        sdk-workflows/src/main/java/io/dapr/workflows/client/RerunWorkflowFromEventOptions.java \
        sdk-workflows/src/test/java/io/dapr/workflows/client/WorkflowInstancePageTest.java \
        sdk-workflows/src/test/java/io/dapr/workflows/client/WorkflowHistoryEventTest.java \
        sdk-workflows/src/test/java/io/dapr/workflows/client/RerunWorkflowFromEventOptionsTest.java
git commit -s -m "feat(workflows): add list/history/rerun model types (#1794)"
```

---

### Task 2: Proto-to-model converter

**Files:**
- Create: `sdk-workflows/src/main/java/io/dapr/workflows/client/WorkflowClientConverter.java`
- Test: `sdk-workflows/src/test/java/io/dapr/workflows/client/WorkflowClientConverterTest.java`

**Interfaces:**
- Consumes: `WorkflowInstancePage`, `WorkflowHistoryEvent`, `WorkflowHistoryEventType` (Task 1); generated proto `io.dapr.durabletask.implementation.protobuf.OrchestratorService.ListInstanceIDsResponse`, `io.dapr.durabletask.implementation.protobuf.HistoryEvents.HistoryEvent` (already generated under `durabletask-client/target`).
- Produces (package-private static methods used by Task 4):
  - `static WorkflowInstancePage toWorkflowInstancePage(OrchestratorService.ListInstanceIDsResponse response)`
  - `static List<WorkflowHistoryEvent> toWorkflowHistory(List<HistoryEvent> events)`
  - `static WorkflowHistoryEvent toWorkflowHistoryEvent(HistoryEvent event)`
  - `static WorkflowHistoryEventType toEventType(HistoryEvent.EventTypeCase eventType)`

- [ ] **Step 1: Write the failing test**

`WorkflowClientConverterTest.java` (license header, then):
```java
package io.dapr.workflows.client;

import com.google.protobuf.Timestamp;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents.HistoryEvent;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService.ListInstanceIDsResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class WorkflowClientConverterTest {

  @Test
  public void mapsEventTypeCases() {
    assertEquals(WorkflowHistoryEventType.EXECUTION_STARTED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.EXECUTIONSTARTED));
    assertEquals(WorkflowHistoryEventType.TASK_SCHEDULED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.TASKSCHEDULED));
    assertEquals(WorkflowHistoryEventType.WORKFLOW_STARTED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.WORKFLOWSTARTED));
    assertEquals(WorkflowHistoryEventType.CHILD_WORKFLOW_INSTANCE_CREATED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.DETACHEDWORKFLOWINSTANCECREATED));
    assertEquals(WorkflowHistoryEventType.UNKNOWN,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.EVENTTYPE_NOT_SET));
  }

  @Test
  public void mapsHistoryEvent() {
    HistoryEvent event = HistoryEvent.newBuilder()
        .setEventId(7)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1500).setNanos(500).build())
        .setExecutionStarted(HistoryEvents.ExecutionStartedEvent.getDefaultInstance())
        .build();

    WorkflowHistoryEvent result = WorkflowClientConverter.toWorkflowHistoryEvent(event);

    assertEquals(7, result.getEventId());
    assertEquals(WorkflowHistoryEventType.EXECUTION_STARTED, result.getEventType());
    assertEquals(Instant.ofEpochSecond(1500, 500), result.getTimestamp());
  }

  @Test
  public void mapsHistoryList() {
    HistoryEvent event = HistoryEvent.newBuilder()
        .setEventId(1)
        .setTimerCreated(HistoryEvents.TimerCreatedEvent.getDefaultInstance())
        .build();

    assertEquals(1, WorkflowClientConverter.toWorkflowHistory(Arrays.asList(event)).size());
    assertEquals(WorkflowHistoryEventType.TIMER_CREATED,
        WorkflowClientConverter.toWorkflowHistory(Arrays.asList(event)).get(0).getEventType());
  }

  @Test
  public void mapsInstancePageWithToken() {
    ListInstanceIDsResponse response = ListInstanceIDsResponse.newBuilder()
        .addInstanceIds("a").addInstanceIds("b")
        .setContinuationToken("next")
        .build();

    WorkflowInstancePage page = WorkflowClientConverter.toWorkflowInstancePage(response);

    assertEquals(Arrays.asList("a", "b"), page.getInstanceIds());
    assertEquals("next", page.getContinuationToken());
  }

  @Test
  public void mapsInstancePageWithoutToken() {
    ListInstanceIDsResponse response = ListInstanceIDsResponse.newBuilder().addInstanceIds("a").build();

    WorkflowInstancePage page = WorkflowClientConverter.toWorkflowInstancePage(response);

    assertNull(page.getContinuationToken());
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -pl sdk-workflows -am test-compile`
Expected: FAIL — `WorkflowClientConverter` does not exist.

- [ ] **Step 3: Create the converter**

`WorkflowClientConverter.java` (license header, then):
```java
package io.dapr.workflows.client;

import com.google.protobuf.Timestamp;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents.HistoryEvent;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService.ListInstanceIDsResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts durabletask proto messages to public workflow client model types.
 */
final class WorkflowClientConverter {

  private WorkflowClientConverter() {
  }

  static WorkflowInstancePage toWorkflowInstancePage(ListInstanceIDsResponse response) {
    return new WorkflowInstancePage(
        new ArrayList<>(response.getInstanceIdsList()),
        response.hasContinuationToken() ? response.getContinuationToken() : null);
  }

  static List<WorkflowHistoryEvent> toWorkflowHistory(List<HistoryEvent> events) {
    List<WorkflowHistoryEvent> result = new ArrayList<>(events.size());
    for (HistoryEvent event : events) {
      result.add(toWorkflowHistoryEvent(event));
    }
    return Collections.unmodifiableList(result);
  }

  static WorkflowHistoryEvent toWorkflowHistoryEvent(HistoryEvent event) {
    Instant timestamp = event.hasTimestamp() ? toInstant(event.getTimestamp()) : Instant.EPOCH;
    return new WorkflowHistoryEvent(event.getEventId(), toEventType(event.getEventTypeCase()), timestamp);
  }

  static WorkflowHistoryEventType toEventType(HistoryEvent.EventTypeCase eventType) {
    switch (eventType) {
      case EXECUTIONSTARTED:
        return WorkflowHistoryEventType.EXECUTION_STARTED;
      case EXECUTIONCOMPLETED:
        return WorkflowHistoryEventType.EXECUTION_COMPLETED;
      case EXECUTIONTERMINATED:
        return WorkflowHistoryEventType.EXECUTION_TERMINATED;
      case TASKSCHEDULED:
        return WorkflowHistoryEventType.TASK_SCHEDULED;
      case TASKCOMPLETED:
        return WorkflowHistoryEventType.TASK_COMPLETED;
      case TASKFAILED:
        return WorkflowHistoryEventType.TASK_FAILED;
      case CHILDWORKFLOWINSTANCECREATED:
      case DETACHEDWORKFLOWINSTANCECREATED:
        return WorkflowHistoryEventType.CHILD_WORKFLOW_INSTANCE_CREATED;
      case CHILDWORKFLOWINSTANCECOMPLETED:
        return WorkflowHistoryEventType.CHILD_WORKFLOW_INSTANCE_COMPLETED;
      case CHILDWORKFLOWINSTANCEFAILED:
        return WorkflowHistoryEventType.CHILD_WORKFLOW_INSTANCE_FAILED;
      case TIMERCREATED:
        return WorkflowHistoryEventType.TIMER_CREATED;
      case TIMERFIRED:
        return WorkflowHistoryEventType.TIMER_FIRED;
      case WORKFLOWSTARTED:
        return WorkflowHistoryEventType.WORKFLOW_STARTED;
      case WORKFLOWCOMPLETED:
        return WorkflowHistoryEventType.WORKFLOW_COMPLETED;
      case EVENTSENT:
        return WorkflowHistoryEventType.EVENT_SENT;
      case EVENTRAISED:
        return WorkflowHistoryEventType.EVENT_RAISED;
      case CONTINUEASNEW:
        return WorkflowHistoryEventType.CONTINUE_AS_NEW;
      case EXECUTIONSUSPENDED:
        return WorkflowHistoryEventType.EXECUTION_SUSPENDED;
      case EXECUTIONRESUMED:
        return WorkflowHistoryEventType.EXECUTION_RESUMED;
      case EXECUTIONSTALLED:
        return WorkflowHistoryEventType.EXECUTION_STALLED;
      default:
        return WorkflowHistoryEventType.UNKNOWN;
    }
  }

  private static Instant toInstant(Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
  }
}
```

> Note: if compilation fails on a `case` label because a generated `EventTypeCase` constant name differs, list the exact constants with
> `grep -oE "^\s+[A-Z_]+\(" durabletask-client/target/generated-sources/protobuf/java/io/dapr/durabletask/implementation/protobuf/HistoryEvents.java | tr -d ' ('`
> and match the labels. The confirmed constants are: EXECUTIONSTARTED, EXECUTIONCOMPLETED, EXECUTIONTERMINATED, TASKSCHEDULED, TASKCOMPLETED, TASKFAILED, CHILDWORKFLOWINSTANCECREATED, CHILDWORKFLOWINSTANCECOMPLETED, CHILDWORKFLOWINSTANCEFAILED, TIMERCREATED, TIMERFIRED, WORKFLOWSTARTED, WORKFLOWCOMPLETED, EVENTSENT, EVENTRAISED, CONTINUEASNEW, EXECUTIONSUSPENDED, EXECUTIONRESUMED, EXECUTIONSTALLED, DETACHEDWORKFLOWINSTANCECREATED, EVENTTYPE_NOT_SET.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -q -pl sdk-workflows -am test -Dtest=WorkflowClientConverterTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add sdk-workflows/src/main/java/io/dapr/workflows/client/WorkflowClientConverter.java \
        sdk-workflows/src/test/java/io/dapr/workflows/client/WorkflowClientConverterTest.java
git commit -s -m "feat(workflows): add proto-to-model converter for history/list (#1794)"
```

---

### Task 3: Durabletask layer (abstract + gRPC impl)

**Files:**
- Modify: `durabletask-client/src/main/java/io/dapr/durabletask/DurableTaskClient.java` (add three abstract methods at the end of the class, before the closing brace)
- Modify: `durabletask-client/src/main/java/io/dapr/durabletask/DurableTaskGrpcClient.java` (add three `@Override` methods before the final `toPurgeResult` helper)

**Interfaces:**
- Consumes: generated `OrchestratorService` request/response builders and the `TaskHubSidecarServiceBlockingStub` methods `listInstanceIDs`, `getInstanceHistory`, `rerunWorkflowFromEvent` (already generated); `this.dataConverter` and `Helpers.throwIfArgumentNull` (existing in `DurableTaskGrpcClient`).
- Produces (Task 4 calls these on `DurableTaskClient`):
  - `abstract OrchestratorService.ListInstanceIDsResponse listInstanceIds(@Nullable String continuationToken, @Nullable Integer pageSize)`
  - `abstract List<HistoryEvent> getInstanceHistory(String instanceId)`
  - `abstract String rerunWorkflowFromEvent(String sourceInstanceId, int eventId, @Nullable String newInstanceId, @Nullable Object input, boolean overwriteInput)`

- [ ] **Step 1: Add the abstract methods to `DurableTaskClient.java`**

Add these imports after the existing `import javax.annotation.Nullable;` / `import java.time.Duration;` block:
```java
import io.dapr.durabletask.implementation.protobuf.HistoryEvents.HistoryEvent;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;

import java.util.List;
```

Add these three methods immediately before the final closing brace `}` of the class (after `resumeInstance(String, @Nullable String)`):
```java
  /**
   * Lists workflow instance IDs with optional pagination.
   *
   * @param continuationToken the continuation token from a previous call, or null for the first page
   * @param pageSize          the maximum number of instance IDs to return, or null for no limit
   * @return the raw list-instance-IDs response from the sidecar
   */
  public abstract OrchestratorService.ListInstanceIDsResponse listInstanceIds(
      @Nullable String continuationToken, @Nullable Integer pageSize);

  /**
   * Gets the full execution history of a workflow instance.
   *
   * @param instanceId the ID of the workflow instance to get history for
   * @return the list of history events for the workflow instance
   */
  public abstract List<HistoryEvent> getInstanceHistory(String instanceId);

  /**
   * Reruns a workflow from a specific history event, creating a new workflow instance.
   *
   * @param sourceInstanceId the ID of the source workflow instance to rerun from
   * @param eventId          the history event ID to rerun from
   * @param newInstanceId    the instance ID to use for the new instance, or null for a random ID
   * @param input            the input applied at the next activity event, used only when overwriteInput is true
   * @param overwriteInput   true to overwrite the input at the rerun point with input
   * @return the instance ID of the new workflow instance
   */
  public abstract String rerunWorkflowFromEvent(String sourceInstanceId, int eventId,
      @Nullable String newInstanceId, @Nullable Object input, boolean overwriteInput);
```

- [ ] **Step 2: Add the implementations to `DurableTaskGrpcClient.java`**

Add these imports after the existing `import io.dapr.durabletask.implementation.protobuf.OrchestratorService;` line:
```java
import io.dapr.durabletask.implementation.protobuf.HistoryEvents.HistoryEvent;
```
and add `import java.util.List;` in the `java.util.*` import group.

Add these three methods immediately before the `private PurgeResult toPurgeResult(...)` method:
```java
  @Override
  public OrchestratorService.ListInstanceIDsResponse listInstanceIds(
      @Nullable String continuationToken, @Nullable Integer pageSize) {
    OrchestratorService.ListInstanceIDsRequest.Builder builder =
        OrchestratorService.ListInstanceIDsRequest.newBuilder();
    if (continuationToken != null) {
      builder.setContinuationToken(continuationToken);
    }
    if (pageSize != null) {
      if (pageSize <= 0) {
        throw new IllegalArgumentException("pageSize must be greater than zero.");
      }
      builder.setPageSize(pageSize);
    }
    return this.sidecarClient.listInstanceIDs(builder.build());
  }

  @Override
  public List<HistoryEvent> getInstanceHistory(String instanceId) {
    Helpers.throwIfArgumentNull(instanceId, "instanceId");
    OrchestratorService.GetInstanceHistoryRequest request =
        OrchestratorService.GetInstanceHistoryRequest.newBuilder()
            .setInstanceId(instanceId)
            .build();
    OrchestratorService.GetInstanceHistoryResponse response = this.sidecarClient.getInstanceHistory(request);
    return response.getEventsList();
  }

  @Override
  public String rerunWorkflowFromEvent(String sourceInstanceId, int eventId,
      @Nullable String newInstanceId, @Nullable Object input, boolean overwriteInput) {
    Helpers.throwIfArgumentNull(sourceInstanceId, "sourceInstanceId");
    OrchestratorService.RerunWorkflowFromEventRequest.Builder builder =
        OrchestratorService.RerunWorkflowFromEventRequest.newBuilder()
            .setSourceInstanceID(sourceInstanceId)
            .setEventID(eventId)
            .setOverwriteInput(overwriteInput);
    if (newInstanceId != null) {
      builder.setNewInstanceID(newInstanceId);
    }
    if (overwriteInput) {
      builder.setInput(StringValue.of(this.dataConverter.serialize(input)));
    }
    OrchestratorService.RerunWorkflowFromEventResponse response =
        this.sidecarClient.rerunWorkflowFromEvent(builder.build());
    return response.getNewInstanceID();
  }
```

- [ ] **Step 3: Compile the module (checkstyle runs during build)**

Run: `mvn -q -pl durabletask-client -am install -DskipTests`
Expected: BUILD SUCCESS. (Do not add `clean` — see the proto-download caveat.)

If the build fails trying to download the proto from a blocked domain, the proto is already present; retry the same command — the download plugin skips when the file exists. Behavioral coverage for these methods is the integration test in Task 5.

- [ ] **Step 4: Commit**

```bash
git add durabletask-client/src/main/java/io/dapr/durabletask/DurableTaskClient.java \
        durabletask-client/src/main/java/io/dapr/durabletask/DurableTaskGrpcClient.java
git commit -s -m "feat(durabletask): add list/history/rerun gRPC operations (#1794)"
```

---

### Task 4: DaprWorkflowClient public methods

**Files:**
- Modify: `sdk-workflows/src/main/java/io/dapr/workflows/client/DaprWorkflowClient.java` (add imports + five public methods before the `close()` method)
- Test: `sdk-workflows/src/test/java/io/dapr/workflows/client/DaprWorkflowClientTest.java` (add tests + imports)

**Interfaces:**
- Consumes: `DurableTaskClient.listInstanceIds/getInstanceHistory/rerunWorkflowFromEvent` (Task 3); `WorkflowClientConverter` (Task 2); `WorkflowInstancePage`, `WorkflowHistoryEvent`, `RerunWorkflowFromEventOptions` (Task 1).
- Produces (public API, used by Tasks 5-6 and end users):
  - `WorkflowInstancePage listInstanceIds()`
  - `WorkflowInstancePage listInstanceIds(@Nullable String continuationToken, @Nullable Integer pageSize)`
  - `List<WorkflowHistoryEvent> getInstanceHistory(String instanceId)`
  - `String rerunWorkflowFromEvent(String sourceInstanceId, int eventId)`
  - `String rerunWorkflowFromEvent(String sourceInstanceId, int eventId, @Nullable RerunWorkflowFromEventOptions options)`

- [ ] **Step 1: Write the failing tests**

In `DaprWorkflowClientTest.java`, add these imports (with the existing import groups):
```java
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents.HistoryEvent;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService.ListInstanceIDsResponse;
import com.google.protobuf.Timestamp;
import java.util.List;
```
and these static imports:
```java
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
```

Add these test methods inside the class (e.g. after `purgeInstance()`):
```java
  @Test
  public void listInstanceIds() {
    ListInstanceIDsResponse response = ListInstanceIDsResponse.newBuilder()
        .addInstanceIds("id-1").addInstanceIds("id-2")
        .setContinuationToken("next-token")
        .build();
    when(mockInnerClient.listInstanceIds("tok", 50)).thenReturn(response);

    WorkflowInstancePage page = client.listInstanceIds("tok", 50);

    verify(mockInnerClient, times(1)).listInstanceIds("tok", 50);
    assertEquals(Arrays.asList("id-1", "id-2"), page.getInstanceIds());
    assertEquals("next-token", page.getContinuationToken());
  }

  @Test
  public void listInstanceIdsNoArgs() {
    ListInstanceIDsResponse response = ListInstanceIDsResponse.newBuilder().addInstanceIds("id-1").build();
    when(mockInnerClient.listInstanceIds(null, null)).thenReturn(response);

    WorkflowInstancePage page = client.listInstanceIds();

    verify(mockInnerClient, times(1)).listInstanceIds(null, null);
    assertEquals(Arrays.asList("id-1"), page.getInstanceIds());
    assertNull(page.getContinuationToken());
  }

  @Test
  public void listInstanceIdsRejectsNonPositivePageSize() {
    assertThrows(IllegalArgumentException.class, () -> client.listInstanceIds(null, 0));
    verify(mockInnerClient, never()).listInstanceIds(any(), any());
  }

  @Test
  public void getInstanceHistory() {
    HistoryEvent event = HistoryEvent.newBuilder()
        .setEventId(1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(10).build())
        .setExecutionStarted(HistoryEvents.ExecutionStartedEvent.getDefaultInstance())
        .build();
    when(mockInnerClient.getInstanceHistory("wf-1")).thenReturn(Arrays.asList(event));

    List<WorkflowHistoryEvent> history = client.getInstanceHistory("wf-1");

    verify(mockInnerClient, times(1)).getInstanceHistory("wf-1");
    assertEquals(1, history.size());
    assertEquals(1, history.get(0).getEventId());
    assertEquals(WorkflowHistoryEventType.EXECUTION_STARTED, history.get(0).getEventType());
  }

  @Test
  public void getInstanceHistoryRejectsEmptyId() {
    assertThrows(IllegalArgumentException.class, () -> client.getInstanceHistory(""));
    verify(mockInnerClient, never()).getInstanceHistory(any());
  }

  @Test
  public void rerunWorkflowFromEvent() {
    when(mockInnerClient.rerunWorkflowFromEvent("src", 2, null, null, false)).thenReturn("new-id");

    String newId = client.rerunWorkflowFromEvent("src", 2);

    verify(mockInnerClient, times(1)).rerunWorkflowFromEvent("src", 2, null, null, false);
    assertEquals("new-id", newId);
  }

  @Test
  public void rerunWorkflowFromEventWithOptions() {
    RerunWorkflowFromEventOptions options = new RerunWorkflowFromEventOptions()
        .setNewInstanceId("target").setInput("payload").setOverwriteInput(true);
    when(mockInnerClient.rerunWorkflowFromEvent("src", 3, "target", "payload", true)).thenReturn("target");

    String newId = client.rerunWorkflowFromEvent("src", 3, options);

    verify(mockInnerClient, times(1)).rerunWorkflowFromEvent("src", 3, "target", "payload", true);
    assertEquals("target", newId);
  }

  @Test
  public void rerunWorkflowFromEventRejectsInputWithoutOverwrite() {
    RerunWorkflowFromEventOptions options = new RerunWorkflowFromEventOptions().setInput("payload");
    assertThrows(IllegalArgumentException.class, () -> client.rerunWorkflowFromEvent("src", 1, options));
    verify(mockInnerClient, never()).rerunWorkflowFromEvent(any(), anyInt(), any(), any(), anyBoolean());
  }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q -pl sdk-workflows -am test-compile`
Expected: FAIL — the new `DaprWorkflowClient` methods do not exist.

- [ ] **Step 3: Add the methods to `DaprWorkflowClient.java`**

Add these imports (with the existing import groups):
```java
import io.dapr.durabletask.implementation.protobuf.HistoryEvents.HistoryEvent;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;

import java.util.List;
```

Add these five methods immediately before the `public void close()` method:
```java
  /**
   * Lists workflow instance IDs. Returns the first page with no size limit.
   *
   * @return a page of workflow instance IDs
   */
  public WorkflowInstancePage listInstanceIds() {
    return this.listInstanceIds(null, null);
  }

  /**
   * Lists workflow instance IDs with pagination.
   *
   * @param continuationToken the continuation token from a previous call, or null for the first page
   * @param pageSize          the maximum number of instance IDs to return, or null for no limit; must be
   *                          greater than zero when set
   * @return a page of workflow instance IDs and an optional continuation token for the next page
   */
  public WorkflowInstancePage listInstanceIds(@Nullable String continuationToken, @Nullable Integer pageSize) {
    if (pageSize != null && pageSize <= 0) {
      throw new IllegalArgumentException("pageSize must be greater than zero.");
    }
    OrchestratorService.ListInstanceIDsResponse response =
        this.innerClient.listInstanceIds(continuationToken, pageSize);
    return WorkflowClientConverter.toWorkflowInstancePage(response);
  }

  /**
   * Gets the full execution history of a workflow instance.
   *
   * @param instanceId the unique ID of the workflow instance to get history for
   * @return the list of history events for the workflow instance
   */
  public List<WorkflowHistoryEvent> getInstanceHistory(String instanceId) {
    if (instanceId == null || instanceId.isEmpty()) {
      throw new IllegalArgumentException("instanceId must not be null or empty.");
    }
    List<HistoryEvent> events = this.innerClient.getInstanceHistory(instanceId);
    return WorkflowClientConverter.toWorkflowHistory(events);
  }

  /**
   * Reruns a workflow from a history event, creating a new workflow instance.
   *
   * @param sourceInstanceId the ID of the source workflow instance to rerun from
   * @param eventId          the history event ID to rerun from
   * @return the instance ID of the new workflow instance
   */
  public String rerunWorkflowFromEvent(String sourceInstanceId, int eventId) {
    return this.rerunWorkflowFromEvent(sourceInstanceId, eventId, null);
  }

  /**
   * Reruns a workflow from a history event with options, creating a new workflow instance.
   *
   * @param sourceInstanceId the ID of the source workflow instance to rerun from
   * @param eventId          the history event ID to rerun from
   * @param options          optional rerun configuration; may be null
   * @return the instance ID of the new workflow instance
   * @throws IllegalArgumentException if input is set on options without overwriteInput being true
   */
  public String rerunWorkflowFromEvent(String sourceInstanceId, int eventId,
      @Nullable RerunWorkflowFromEventOptions options) {
    if (sourceInstanceId == null || sourceInstanceId.isEmpty()) {
      throw new IllegalArgumentException("sourceInstanceId must not be null or empty.");
    }
    if (options == null) {
      return this.innerClient.rerunWorkflowFromEvent(sourceInstanceId, eventId, null, null, false);
    }
    if (options.getInput() != null && !options.isOverwriteInput()) {
      throw new IllegalArgumentException("overwriteInput must be true when input is set.");
    }
    return this.innerClient.rerunWorkflowFromEvent(sourceInstanceId, eventId,
        options.getNewInstanceId(), options.getInput(), options.isOverwriteInput());
  }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn -q -pl sdk-workflows -am test -Dtest=DaprWorkflowClientTest`
Expected: PASS (all existing plus new tests), checkstyle clean.

- [ ] **Step 5: Run the full sdk-workflows test suite**

Run: `mvn -q -pl sdk-workflows -am test`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add sdk-workflows/src/main/java/io/dapr/workflows/client/DaprWorkflowClient.java \
        sdk-workflows/src/test/java/io/dapr/workflows/client/DaprWorkflowClientTest.java
git commit -s -m "feat(workflows): expose list/history/rerun on DaprWorkflowClient (#1794)"
```

---

### Task 5: Integration tests (real sidecar)

**Files:**
- Modify: `durabletask-client/src/test/java/io/dapr/durabletask/DurableTaskClientIT.java` (add imports + three `@Test` methods)

**Interfaces:**
- Consumes: `DurableTaskClient.listInstanceIds/getInstanceHistory/rerunWorkflowFromEvent` (Task 3); existing test helpers `createWorkerBuilder()`, `defaultTimeout`, `DurableTaskGrpcClientBuilder`.
- Produces: nothing (test-only).

Note: these tests are tagged `@Tag("integration")` and require a running Dapr sidecar on `localhost:4001` (or the durabletask test harness), the same as the existing `DurableTaskClientIT` tests. They run in CI. Locally, if no sidecar/Docker is available, verify with `test-compile` only.

- [ ] **Step 1: Add imports**

Add to `DurableTaskClientIT.java`:
```java
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
```
(`java.util.List` and `java.util.ArrayList` are already imported.)

- [ ] **Step 2: Add the integration tests**

Add these three methods inside the `DurableTaskClientIT` class:
```java
  @Test
  void getInstanceHistoryReturnsEvents() throws TimeoutException {
    final String orchestratorName = "historyTest";
    DurableTaskGrpcWorker worker = this.createWorkerBuilder()
            .addOrchestrator(orchestratorName, ctx -> ctx.complete(ctx.getInput(String.class)))
            .buildAndStart();

    DurableTaskClient client = new DurableTaskGrpcClientBuilder().build();
    try (worker; client) {
      String instanceId = client.scheduleNewOrchestrationInstance(orchestratorName, "hello");
      client.waitForInstanceCompletion(instanceId, defaultTimeout, true);

      List<HistoryEvents.HistoryEvent> history = client.getInstanceHistory(instanceId);

      assertFalse(history.isEmpty());
    }
  }

  @Test
  void listInstanceIdsReturnsScheduledInstance() throws TimeoutException {
    final String orchestratorName = "listTest";
    DurableTaskGrpcWorker worker = this.createWorkerBuilder()
            .addOrchestrator(orchestratorName, ctx -> ctx.complete(ctx.getInput(String.class)))
            .buildAndStart();

    DurableTaskClient client = new DurableTaskGrpcClientBuilder().build();
    try (worker; client) {
      String instanceId = client.scheduleNewOrchestrationInstance(orchestratorName, "hello");
      client.waitForInstanceCompletion(instanceId, defaultTimeout, true);

      List<String> ids = new ArrayList<>();
      String token = null;
      do {
        OrchestratorService.ListInstanceIDsResponse page = client.listInstanceIds(token, 100);
        ids.addAll(page.getInstanceIdsList());
        token = page.hasContinuationToken() ? page.getContinuationToken() : null;
      } while (token != null);

      assertTrue(ids.contains(instanceId));
    }
  }

  @Test
  void rerunWorkflowFromEventCreatesNewInstance() throws TimeoutException {
    final String orchestratorName = "rerunTest";
    DurableTaskGrpcWorker worker = this.createWorkerBuilder()
            .addOrchestrator(orchestratorName, ctx -> ctx.complete(ctx.getInput(String.class)))
            .buildAndStart();

    DurableTaskClient client = new DurableTaskGrpcClientBuilder().build();
    try (worker; client) {
      String instanceId = client.scheduleNewOrchestrationInstance(orchestratorName, "hello");
      client.waitForInstanceCompletion(instanceId, defaultTimeout, true);

      String newInstanceId = client.rerunWorkflowFromEvent(instanceId, 0, null, null, false);

      assertNotEquals(instanceId, newInstanceId);
      OrchestrationMetadata instance = client.waitForInstanceCompletion(newInstanceId, defaultTimeout, true);
      assertEquals(OrchestrationRuntimeStatus.COMPLETED, instance.getRuntimeStatus());
    }
  }
```

- [ ] **Step 3: Compile the test sources**

Run: `mvn -q -pl durabletask-client -am test-compile`
Expected: BUILD SUCCESS. (Full run: `mvn -q -pl durabletask-client test -Dtest=DurableTaskClientIT` in CI with a sidecar.)

- [ ] **Step 4: Commit**

```bash
git add durabletask-client/src/test/java/io/dapr/durabletask/DurableTaskClientIT.java
git commit -s -m "test(durabletask): integration tests for list/history/rerun (#1794)"
```

---

### Task 6: Runnable example and docs

**Files:**
- Create: `examples/src/main/java/io/dapr/examples/workflows/management/DemoWorkflowManagementWorkflow.java`
- Create: `examples/src/main/java/io/dapr/examples/workflows/management/DemoWorkflowManagementActivity.java`
- Create: `examples/src/main/java/io/dapr/examples/workflows/management/DemoWorkflowManagementWorker.java`
- Create: `examples/src/main/java/io/dapr/examples/workflows/management/DemoWorkflowManagementClient.java`
- Modify: `examples/src/main/java/io/dapr/examples/workflows/README.md` (add a section after the Suspend/Resume Pattern section)

**Interfaces:**
- Consumes: `DaprWorkflowClient.listInstanceIds/getInstanceHistory/rerunWorkflowFromEvent` (Task 4); example utilities `io.dapr.examples.workflows.utils.PropertyUtils`.
- Produces: nothing (example + docs only).

- [ ] **Step 1: Create the workflow**

`DemoWorkflowManagementWorkflow.java` (license header, then):
```java
package io.dapr.examples.workflows.management;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

public class DemoWorkflowManagementWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());
      String result = ctx.callActivity(
          DemoWorkflowManagementActivity.class.getName(), "Tokyo", String.class).await();
      ctx.getLogger().info("Workflow finished with result: " + result);
      ctx.complete(result);
    };
  }
}
```

- [ ] **Step 2: Create the activity**

`DemoWorkflowManagementActivity.java` (license header, then):
```java
package io.dapr.examples.workflows.management;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoWorkflowManagementActivity implements WorkflowActivity {
  @Override
  public Object run(WorkflowActivityContext ctx) {
    Logger logger = LoggerFactory.getLogger(DemoWorkflowManagementActivity.class);
    logger.info("Starting Activity: " + ctx.getName());
    String message = ctx.getInput(String.class);
    String newMessage = message.toUpperCase();
    logger.info("Message Received from input: " + message);
    return newMessage;
  }
}
```

- [ ] **Step 3: Create the worker**

`DemoWorkflowManagementWorker.java` (license header, then):
```java
package io.dapr.examples.workflows.management;

import io.dapr.examples.workflows.utils.PropertyUtils;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

public class DemoWorkflowManagementWorker {
  /**
   * The main method of this app.
   *
   * @param args The port the app will listen on.
   * @throws Exception An Exception.
   */
  public static void main(String[] args) throws Exception {
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder(PropertyUtils.getProperties(args))
        .registerWorkflow(DemoWorkflowManagementWorkflow.class);
    builder.registerActivity(DemoWorkflowManagementActivity.class);

    WorkflowRuntime runtime = builder.build();
    System.out.println("Start workflow runtime");
    runtime.start();
  }
}
```

- [ ] **Step 4: Create the client**

`DemoWorkflowManagementClient.java` (license header, then):
```java
package io.dapr.examples.workflows.management;

import io.dapr.examples.workflows.utils.PropertyUtils;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.RerunWorkflowFromEventOptions;
import io.dapr.workflows.client.WorkflowHistoryEvent;
import io.dapr.workflows.client.WorkflowInstancePage;
import io.dapr.workflows.client.WorkflowState;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class DemoWorkflowManagementClient {
  /**
   * The main method to start the client.
   *
   * @param args Input arguments (unused).
   */
  public static void main(String[] args) {
    try (DaprWorkflowClient client = new DaprWorkflowClient(PropertyUtils.getProperties(args))) {
      String instanceId = client.scheduleNewWorkflow(DemoWorkflowManagementWorkflow.class);
      System.out.printf("Started a new workflow with instance ID: %s%n", instanceId);

      WorkflowState state = client.waitForWorkflowCompletion(instanceId, null, true);
      System.out.printf("Workflow completed with result: %s%n", state.readOutputAs(String.class));

      // Read the full execution history.
      List<WorkflowHistoryEvent> history = client.getInstanceHistory(instanceId);
      System.out.printf("History for %s has %d events:%n", instanceId, history.size());
      for (WorkflowHistoryEvent event : history) {
        System.out.printf("  eventId=%d type=%s at=%s%n",
            event.getEventId(), event.getEventType(), event.getTimestamp());
      }

      // Rerun the workflow from the first history event.
      int firstEventId = history.get(0).getEventId();
      String rerunId = client.rerunWorkflowFromEvent(instanceId, firstEventId,
          new RerunWorkflowFromEventOptions().setInput("Osaka").setOverwriteInput(true));
      System.out.printf("Reran workflow from event %d as new instance: %s%n", firstEventId, rerunId);
      client.waitForWorkflowCompletion(rerunId, null, true);

      // List workflow instance IDs (first page).
      WorkflowInstancePage page = client.listInstanceIds(null, 100);
      System.out.printf("Listed %d instance ID(s); continuationToken=%s%n",
          page.getInstanceIds().size(), page.getContinuationToken());
    } catch (TimeoutException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
```

- [ ] **Step 5: Add the README section**

Append this section to `examples/src/main/java/io/dapr/examples/workflows/README.md` after the Suspend/Resume Pattern section (end of file):
```markdown
### Workflow Management (List, History, Rerun) Pattern

The `DaprWorkflowClient` can list workflow instance IDs, read a workflow instance's full
execution history, and rerun a workflow from a specific history event. This example shows
all three operations.

<!-- STEP
name: Run Workflow Management Worker
match_order: none
expected_stdout_lines:
  - "Start workflow runtime"
background: true
sleep: 20
timeout_seconds: 45
-->

Run the worker:

```sh
dapr run --app-id demoworkflowworker --resources-path ./components/workflows -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.management.DemoWorkflowManagementWorker
```

<!-- END_STEP -->

<!-- STEP
name: Run Workflow Management Client
match_order: none
expected_stdout_lines:
  - "Started a new workflow with instance ID"
  - "Workflow completed with result"
  - "Reran workflow from event"
  - "Listed"
timeout_seconds: 60
-->

In a separate terminal, run the client:

```sh
dapr run --app-id demoworkflowclient --resources-path ./components/workflows -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.management.DemoWorkflowManagementClient
```

<!-- END_STEP -->

The client output shows the started instance ID, the completed result, the list of history
events (each with its event ID, type, and timestamp), the new instance ID from the rerun,
and the count of listed instance IDs.
```

- [ ] **Step 6: Compile the examples module**

Run: `mvn -q -pl examples -am install -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add examples/src/main/java/io/dapr/examples/workflows/management/ \
        examples/src/main/java/io/dapr/examples/workflows/README.md
git commit -s -m "docs(examples): add workflow list/history/rerun example (#1794)"
```

---

## Final Verification

- [ ] Build both core modules together and run their tests:

Run: `mvn -q -pl durabletask-client,sdk-workflows -am install -DskipTests` then `mvn -q -pl sdk-workflows -am test`
Expected: BUILD SUCCESS, all unit tests pass, checkstyle clean.

- [ ] Confirm the public API is present:

Run: `grep -c "public WorkflowInstancePage listInstanceIds\|public List<WorkflowHistoryEvent> getInstanceHistory\|public String rerunWorkflowFromEvent" sdk-workflows/src/main/java/io/dapr/workflows/client/DaprWorkflowClient.java`
Expected: `5` (two `listInstanceIds` overloads, one `getInstanceHistory`, two `rerunWorkflowFromEvent` overloads).

## Acceptance Criteria (from spec)

- `DaprWorkflowClient` exposes `listInstanceIds` (2 overloads), `getInstanceHistory`, and `rerunWorkflowFromEvent` (2 overloads). ✓ Task 4
- Model types `WorkflowInstancePage`, `WorkflowHistoryEvent`, `WorkflowHistoryEventType`, `RerunWorkflowFromEventOptions` exist. ✓ Task 1
- Durabletask layer exposes the three operations. ✓ Task 3
- Unit tests pass; integration tests compile and run against a sidecar. ✓ Tasks 1,2,4,5
- Runnable example and README section exist. ✓ Task 6
- Public API has full javadoc. ✓ Tasks 1,3,4
