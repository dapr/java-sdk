# Design: List, history, and rerun workflow operations in the Java SDK

- **Issue:** dapr/java-sdk#1794
- **Date:** 2026-08-26
- **Status:** Approved design (pending user review)

## 1. Goal

Add three workflow-runtime operations to the Dapr Java SDK so users can call them
without using the gRPC channel directly:

1. `ListInstanceIDs` — list workflow instance IDs with pagination.
2. `GetInstanceHistory` — get the full execution history of a workflow instance.
3. `RerunWorkflowFromEvent` — start a new workflow instance from a history event of
   an existing instance.

The dotnet SDK already added the same three operations in PR dapr/dotnet-sdk#1738.
This design mirrors that public API so all SDKs stay consistent.

## 2. Background

The three operations are RPCs on the durabletask backend service
`TaskHubSidecarService`. The Dapr sidecar (daprd) serves this service on the same
gRPC endpoint as the main Dapr API. The operations are **not** part of the
`dapr.proto.runtime.v1.Dapr` service.

The Java SDK already downloads the durabletask proto at build time and generates the
gRPC stub. The generated blocking stub
(`io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc`) already has
all three methods:

- `listInstanceIDs(ListInstanceIDsRequest) → ListInstanceIDsResponse`
- `getInstanceHistory(GetInstanceHistoryRequest) → GetInstanceHistoryResponse`
- `rerunWorkflowFromEvent(RerunWorkflowFromEventRequest) → RerunWorkflowFromEventResponse`

**No proto change is needed.** The work is wrapper code only.

### Proto message fields (confirmed in generated code)

- `ListInstanceIDsRequest`: `optional string continuationToken = 1`, `optional uint32 pageSize = 2`.
- `ListInstanceIDsResponse`: `repeated string instanceIds = 1`, `optional string continuationToken = 2`.
  Accessors: `getInstanceIdsList()`, `getInstanceIdsCount()`, `hasContinuationToken()`, `getContinuationToken()`.
- `GetInstanceHistoryRequest`: `string instanceId = 1`.
- `GetInstanceHistoryResponse`: `repeated HistoryEvent events = 1`. Accessor: `getEventsList()`.
- `RerunWorkflowFromEventRequest`: `sourceInstanceID` (string), `eventID` (uint32),
  `newInstanceID` (optional string), `input` (google.protobuf.StringValue),
  `overwriteInput` (bool), `newChildWorkflowInstanceID` (optional string).
  Setters: `setSourceInstanceID`, `setEventID(int)`, `setNewInstanceID`,
  `setInput(StringValue)`, `setOverwriteInput(boolean)`, `setNewChildWorkflowInstanceID`.
- `RerunWorkflowFromEventResponse`: `string newInstanceID = 1`. Accessor: `getNewInstanceID()`.
- `HistoryEvent` (in `HistoryEvents`): `getEventId()` (int), `hasTimestamp()`,
  `getTimestamp()` (google.protobuf.Timestamp), `getEventTypeCase()` (enum `EventTypeCase`).

## 3. Architecture

The SDK uses a three-layer path for every workflow operation. This design adds the
three operations to all three layers, so the client stays mockable and the existing
unit-test pattern keeps working.

```
io.dapr.workflows.client.DaprWorkflowClient          (module sdk-workflows, user-facing)
  └─ delegates to io.dapr.durabletask.DurableTaskClient        (module durabletask-client, abstract)
        └─ impl DurableTaskGrpcClient wraps TaskHubSidecarServiceGrpc blocking stub
```

**Layering decision (confirmed): the durabletask layer returns raw proto.**
`sdk-workflows` depends on `durabletask-client`, not the reverse. So the durabletask
layer cannot return `io.dapr.workflows.client` types. The new durabletask methods
return raw proto (`List<HistoryEvent>`, `ListInstanceIDsResponse`), and
`DaprWorkflowClient` converts proto to the public model types. This keeps the
**user-facing** API free of proto (the issue requirement) with the least code. It does
introduce generated proto types into the `durabletask-client` public API for the first
time; this is accepted because that module is the low-level gRPC layer.

## 4. Public API (module `sdk-workflows`, package `io.dapr.workflows.client`)

The Java client is synchronous (existing methods block). So the design drops dotnet's
`Async` suffix and `CancellationToken`, and keeps the same names, parameters, and model
shapes.

### 4.1 New methods on `DaprWorkflowClient`

```java
/** Lists workflow instance IDs. Returns the first page with no size limit. */
public WorkflowInstancePage listInstanceIds();

/** Lists workflow instance IDs with pagination. */
public WorkflowInstancePage listInstanceIds(@Nullable String continuationToken, @Nullable Integer pageSize);

/** Gets the full execution history of a workflow instance. */
public List<WorkflowHistoryEvent> getInstanceHistory(String instanceId);

/** Reruns a workflow from a history event. Returns the new instance ID. */
public String rerunWorkflowFromEvent(String sourceInstanceId, int eventId);

/** Reruns a workflow from a history event with options. Returns the new instance ID. */
public String rerunWorkflowFromEvent(String sourceInstanceId, int eventId,
    @Nullable RerunWorkflowFromEventOptions options);
```

Rules:
- `listInstanceIds(token, pageSize)`: `pageSize`, when not null, must be greater than
  zero, else throw `IllegalArgumentException`.
- `getInstanceHistory(instanceId)`: `instanceId` must not be null or empty.
- `rerunWorkflowFromEvent(...)`: `sourceInstanceId` must not be null or empty. If
  `options.getInput()` is set and `options.isOverwriteInput()` is false, throw
  `IllegalArgumentException` (mirrors dotnet).
- Method names mirror the dotnet SDK and the durabletask RPCs for cross-SDK parity.

### 4.2 New model types

**`WorkflowInstancePage`** — immutable value type.
```java
public final class WorkflowInstancePage {
  public WorkflowInstancePage(List<String> instanceIds, @Nullable String continuationToken);
  public List<String> getInstanceIds();            // never null; unmodifiable
  @Nullable public String getContinuationToken();  // null when there are no more pages
}
```

**`WorkflowHistoryEvent`** — immutable value type.
```java
public final class WorkflowHistoryEvent {
  public WorkflowHistoryEvent(int eventId, WorkflowHistoryEventType eventType, Instant timestamp);
  public int getEventId();
  public WorkflowHistoryEventType getEventType();
  public Instant getTimestamp();
}
```

**`WorkflowHistoryEventType`** — enum. Values use java-sdk vocabulary (they follow the
proto's own workflow-centric oneof names, not dotnet's durabletask-classic names).

Values:
`Unknown`, `ExecutionStarted`, `ExecutionCompleted`, `ExecutionTerminated`,
`TaskScheduled`, `TaskCompleted`, `TaskFailed`, `ChildWorkflowInstanceCreated`,
`ChildWorkflowInstanceCompleted`, `ChildWorkflowInstanceFailed`, `TimerCreated`,
`TimerFired`, `WorkflowStarted`, `WorkflowCompleted`, `EventSent`, `EventRaised`,
`ContinueAsNew`, `ExecutionSuspended`, `ExecutionResumed`, `ExecutionStalled`.

**`RerunWorkflowFromEventOptions`** — fluent setters, matching `NewWorkflowOptions` style.
```java
public final class RerunWorkflowFromEventOptions {
  public RerunWorkflowFromEventOptions setNewInstanceId(String newInstanceId);  // returns this
  public RerunWorkflowFromEventOptions setInput(Object input);                  // returns this
  public RerunWorkflowFromEventOptions setOverwriteInput(boolean overwriteInput);// returns this
  @Nullable public String getNewInstanceId();
  @Nullable public Object getInput();
  public boolean isOverwriteInput();
}
```
This type does not expose `newChildWorkflowInstanceID`. The dotnet SDK omits it too, so
the SDKs stay consistent. It can be added later without breaking the API.

### 4.3 Proto-to-model converter

A package-private helper class in `io.dapr.workflows.client` (name:
`WorkflowClientConverter`) isolates the proto imports and holds the conversion logic
(the direct analog of dotnet's `ProtoConverters`):

- `toWorkflowInstancePage(ListInstanceIDsResponse)` — copy `getInstanceIdsList()`; set
  `continuationToken` to `hasContinuationToken() ? getContinuationToken() : null`.
- `toWorkflowHistory(List<HistoryEvent>)` — map each event.
- `toWorkflowHistoryEvent(HistoryEvent)` — `getEventId()`, `toEventType(getEventTypeCase())`,
  and timestamp `Instant.ofEpochSecond(seconds, nanos)` (or `Instant.EPOCH` when
  `hasTimestamp()` is false).
- `toEventType(HistoryEvent.EventTypeCase)` — switch mapping below.

`EventTypeCase` to `WorkflowHistoryEventType` mapping:

| Proto EventTypeCase              | WorkflowHistoryEventType         |
|----------------------------------|----------------------------------|
| EXECUTIONSTARTED                 | ExecutionStarted                 |
| EXECUTIONCOMPLETED               | ExecutionCompleted               |
| EXECUTIONTERMINATED              | ExecutionTerminated              |
| TASKSCHEDULED                    | TaskScheduled                    |
| TASKCOMPLETED                    | TaskCompleted                    |
| TASKFAILED                       | TaskFailed                       |
| CHILDWORKFLOWINSTANCECREATED     | ChildWorkflowInstanceCreated     |
| CHILDWORKFLOWINSTANCECOMPLETED   | ChildWorkflowInstanceCompleted   |
| CHILDWORKFLOWINSTANCEFAILED      | ChildWorkflowInstanceFailed      |
| TIMERCREATED                     | TimerCreated                     |
| TIMERFIRED                       | TimerFired                       |
| WORKFLOWSTARTED                  | WorkflowStarted                  |
| WORKFLOWCOMPLETED                | WorkflowCompleted                |
| EVENTSENT                        | EventSent                        |
| EVENTRAISED                      | EventRaised                      |
| CONTINUEASNEW                    | ContinueAsNew                    |
| EXECUTIONSUSPENDED               | ExecutionSuspended               |
| EXECUTIONRESUMED                 | ExecutionResumed                 |
| EXECUTIONSTALLED                 | ExecutionStalled                 |
| DETACHEDWORKFLOWINSTANCECREATED  | ChildWorkflowInstanceCreated     |
| EVENTTYPE_NOT_SET / other        | Unknown                          |

## 5. Durabletask layer (module `durabletask-client`, package `io.dapr.durabletask`)

### 5.1 `DurableTaskClient` (abstract) — new abstract methods

```java
public abstract ListInstanceIDsResponse listInstanceIds(
    @Nullable String continuationToken, @Nullable Integer pageSize);

public abstract List<HistoryEvent> getInstanceHistory(String instanceId);

public abstract String rerunWorkflowFromEvent(String sourceInstanceId, int eventId,
    @Nullable String newInstanceId, @Nullable Object input, boolean overwriteInput);
```
(Return types are the generated proto types
`io.dapr.durabletask.implementation.protobuf.OrchestratorService.ListInstanceIDsResponse`
and `io.dapr.durabletask.implementation.protobuf.HistoryEvents.HistoryEvent`.)

### 5.2 `DurableTaskGrpcClient` (impl)

- `listInstanceIds`: build `ListInstanceIDsRequest`; set `continuationToken` when not
  null; set `pageSize` when not null (validate greater than zero); call the stub; return
  the response.
- `getInstanceHistory`: validate `instanceId`; build `GetInstanceHistoryRequest`; call
  the stub; return `response.getEventsList()`.
- `rerunWorkflowFromEvent`: build `RerunWorkflowFromEventRequest` with
  `setSourceInstanceID`, `setEventID(eventId)`, `setOverwriteInput(overwriteInput)`; set
  `newInstanceID` when not null; when `overwriteInput` is true, set
  `input` to `StringValue.of(this.dataConverter.serialize(input))` (same pattern as
  `scheduleNewOrchestrationInstance`); call the stub; return `response.getNewInstanceID()`.

## 6. Observation subclass

`io.dapr.spring.observation.client.ObservationDaprWorkflowClient` extends
`DaprWorkflowClient`. It inherits the three new methods automatically. No override is
needed unless span wrapping is wanted; this design does not add spans for these
operations (dotnet does not either).

## 7. Tests

### 7.1 Unit tests (module `sdk-workflows`)
- Extend `DaprWorkflowClientTest`
  (`sdk-workflows/src/test/java/io/dapr/workflows/client/DaprWorkflowClientTest.java`):
  mock `DurableTaskClient`, inject via the private `(DurableTaskClient, ManagedChannel)`
  constructor (existing reflection pattern), and for each new method:
  - `listInstanceIds`: stub returns a built `ListInstanceIDsResponse`; assert the
    returned `WorkflowInstancePage` values; verify delegation; test pageSize validation.
  - `getInstanceHistory`: stub returns a built `List<HistoryEvent>`; assert the mapped
    `List<WorkflowHistoryEvent>`; verify delegation; test null/empty instanceId.
  - `rerunWorkflowFromEvent`: stub returns a new ID; verify args passed; test the
    input/overwriteInput validation throws.
- New `WorkflowClientConverterTest`: build proto `HistoryEvent` values with different
  oneof cases and timestamps; assert the mapping and `WorkflowInstancePage` conversion
  (mirrors dotnet `ProtoConvertersTests`).
- Small value-type tests for `WorkflowInstancePage`, `WorkflowHistoryEvent`, and
  `RerunWorkflowFromEventOptions` (constructor, getters, unmodifiable list, fluent
  setters).

### 7.2 Integration tests (module `durabletask-client`)
- Extend `DurableTaskClientIT`
  (`durabletask-client/src/test/java/io/dapr/durabletask/DurableTaskClientIT.java`),
  which already runs a real sidecar with testcontainers:
  - Schedule and complete a workflow, then `getInstanceHistory` and assert the event
    list is non-empty and event IDs are present.
  - `listInstanceIds` and assert the scheduled instance ID is returned; exercise
    `pageSize` and `continuationToken` paging.
  - `rerunWorkflowFromEvent` from an early event and assert a new instance ID is
    returned and its run completes.

The `sdk-tests` module test `DaprWorkflowsIT` is a possible second home for a
client-layer end-to-end test. It is optional here because `sdk-tests` is under an
in-progress migration on another branch. Keep the primary integration coverage in
`DurableTaskClientIT`.

## 8. Example

Add a runnable example under
`examples/src/main/java/io/dapr/examples/workflows/management/` that:
1. schedules a simple workflow (reuse an existing example workflow/activity),
2. reads its history with `getInstanceHistory`,
3. reruns from an event with `rerunWorkflowFromEvent`,
4. lists instance IDs with `listInstanceIds`.

Update `examples/src/main/java/io/dapr/examples/workflows/README.md` with a section that
shows how to run it.

## 9. Out of scope

- Proto changes (none needed).
- `newChildWorkflowInstanceID` in the rerun options (dotnet omits it; add later if needed).
- Auto-paginating list helper (single-page + continuation token only, matching dotnet).
- OpenTelemetry spans for the new operations.

## 10. Acceptance criteria

- `DaprWorkflowClient` exposes the three methods with the signatures in section 4.1.
- The four model types exist with the shapes in section 4.2.
- The durabletask layer exposes the three methods in section 5.
- `mvn -pl durabletask-client,sdk-workflows -am install` builds and passes checkstyle.
- New unit tests pass. Integration tests pass against a real sidecar.
- A runnable example and README section exist.
- Public API has full javadoc.
