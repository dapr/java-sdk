# Migrating workflows to the unified `dapr-sdk-workflows`

The durable task client has been folded into the workflows SDK. `io.dapr:durabletask-client` no
longer exists as a separate artifact, and everything it contained now ships inside
`io.dapr:dapr-sdk-workflows` under `io.dapr.workflows.*`.

This is a **breaking change**. Most of it is mechanical: change a dependency, change some imports.
A smaller part needs real attention, and one item affects workflows that are already running when
you upgrade. Read [Behaviour changes](#4-behaviour-changes) and [Workflows already
running](#5-workflows-already-running) even if the rest applies cleanly.

---

## 1. Dependencies

Remove `durabletask-client`. It is gone, and its classes are in `dapr-sdk-workflows`.

```xml
<!-- before -->
<dependency>
  <groupId>io.dapr</groupId>
  <artifactId>dapr-sdk-workflows</artifactId>
</dependency>
<dependency>
  <groupId>io.dapr</groupId>
  <artifactId>durabletask-client</artifactId>
</dependency>

<!-- after -->
<dependency>
  <groupId>io.dapr</groupId>
  <artifactId>dapr-sdk-workflows</artifactId>
</dependency>
```

If you import `dapr-sdk-bom`, nothing changes beyond removing any explicit `durabletask-client`
dependency: the BOM no longer manages that artifact.

> **Do not keep the old `durabletask-client` on the classpath alongside the new
> `dapr-sdk-workflows`.** Both jars contain the generated protobuf classes under
> `io.dapr.durabletask.implementation.protobuf`, so the two will collide. Which copy wins is
> whichever the classloader reaches first.

---

## 2. What most codebases actually have to change

These are the types application code normally names. If you only use these, the migration is an
import rewrite.

| Was | Now |
|---|---|
| `io.dapr.durabletask.Task` | `io.dapr.workflows.task.Task` |
| `io.dapr.durabletask.TaskFailedException` | `io.dapr.workflows.task.exception.TaskFailedException` |
| `io.dapr.durabletask.TaskCanceledException` | `io.dapr.workflows.task.exception.TaskCanceledException` |
| `io.dapr.durabletask.CompositeTaskFailedException` | `io.dapr.workflows.task.exception.CompositeTaskFailedException` |
| `io.dapr.durabletask.interruption.OrchestratorBlockedException` | `io.dapr.workflows.task.interruption.OrchestratorBlockedException` |
| `io.dapr.durabletask.interruption.ContinueAsNewInterruption` | `io.dapr.workflows.task.interruption.ContinueAsNewInterruption` |
| `io.dapr.durabletask.PropagatedHistory` | `io.dapr.workflows.task.history.PropagatedHistory` |
| `io.dapr.durabletask.HistoryPropagationScope` | `io.dapr.workflows.task.history.HistoryPropagationScope` |
| `io.dapr.durabletask.WorkflowResult` | `io.dapr.workflows.task.history.WorkflowResult` |
| `io.dapr.durabletask.ActivityResult` | `io.dapr.workflows.task.history.ActivityResult` |
| `io.dapr.durabletask.ChildWorkflowResult` | `io.dapr.workflows.task.history.ChildWorkflowResult` |

**Unchanged.** `Workflow`, `WorkflowActivity`, `WorkflowActivityContext`, `WorkflowState`,
`WorkflowRuntime`, `WorkflowRuntimeBuilder` and `DaprWorkflowClient` keep their names and packages.
If your workflow code only touches those plus `WorkflowContext`, it compiles untouched apart from
the imports above.

---

## 3. Renamed types

Seven types were renamed. Each had a near-duplicate twin on the workflows side, and unifying the two
artifacts removed the duplication — that was the point of the merge, and it is not something an
alias can paper over.

| Was | Now | Note |
|---|---|---|
| `io.dapr.durabletask.TaskOrchestrationContext` | `io.dapr.workflows.WorkflowContext` | `callSubOrchestrator` → `callChildWorkflow`; `getIsReplaying` → `isReplaying` |
| `io.dapr.durabletask.TaskOptions` | `io.dapr.workflows.WorkflowTaskOptions` | keeps both the builder and the constructors |
| `io.dapr.durabletask.RetryPolicy` | `io.dapr.workflows.WorkflowTaskRetryPolicy` | see [Behaviour changes](#4-behaviour-changes) |
| `io.dapr.durabletask.RetryHandler` | `io.dapr.workflows.WorkflowTaskRetryHandler` | |
| `io.dapr.durabletask.RetryContext` | `io.dapr.workflows.WorkflowTaskRetryContext` | `getOrchestrationContext()` → `getWorkflowContext()` |
| `io.dapr.durabletask.FailureDetails` | `io.dapr.workflows.task.exception.WorkflowFailureDetails` | was an interface on the workflows side, is now a final class |
| `io.dapr.durabletask.OrchestrationRuntimeStatus` | `io.dapr.workflows.client.WorkflowRuntimeStatus` | gains `STALLED` |

### Removed

| Removed | Use instead |
|---|---|
| `DaprWorkflowClient.getInstanceState(…)` | `getWorkflowState(…)` |
| `DaprWorkflowClient.waitForInstanceStart(…)` | `waitForWorkflowStart(…)` |
| `DaprWorkflowClient.waitForInstanceCompletion(…)` | `waitForWorkflowCompletion(…)` |
| `DaprWorkflowClient.purgeInstance(…)` | `purgeWorkflow(…)` |
| `io.dapr.workflows.client.WorkflowInstanceStatus` | `io.dapr.workflows.client.WorkflowState` |
| `WorkflowRuntimeStatusConverter` | folded into `WorkflowRuntimeStatus` |
| `DefaultWorkflowContext`, `DefaultWorkflowFailureDetails`, `DefaultWorkflowInstanceStatus` | internal adapters, no longer needed |

All four client methods were already `@Deprecated(forRemoval = true)`.

`WorkflowState` is the same shape as `WorkflowInstanceStatus` with one difference: the id accessor
is **`getWorkflowId()`**, not `getInstanceId()`.

### Types that became internal

These were public but are implementation detail. They now live under `io.dapr.workflows.task.internal`
and are not supported API: `Helpers`, `TaskOrchestrationExecutor`, `TaskActivityExecutor`,
`TaskOrchestratorResult`, `UuidGenerator`, and the runners (`DurableRunner`, `ActivityRunner`,
`OrchestratorRunner`, `OrchestrationRunner`). If you depend on any of these, open an issue describing
what you needed them for.

---

## 4. Behaviour changes

Three changes are not visible to the compiler.

**Retry policy defaults are now explicit.** `WorkflowTaskRetryPolicy.getMaxRetryInterval()` and
`getRetryTimeout()` return `Duration.ZERO` when unset, where the workflows-side builder previously
returned `null`. Code doing `if (policy.getMaxRetryInterval() != null)` will now always take the
non-null branch. The value the runtime sees is unchanged — an adapter used to perform this
`null` → `ZERO` conversion, and it now happens in the constructor.

**`WorkflowContext` gained methods.** `getAppId()`, `sendEvent(...)`, `clearCustomStatus()` and two
`createTimer(String, …)` overloads are now on the interface. Calling code is unaffected. **Anything
that *implements* `WorkflowContext` must implement them** — mocks and test doubles included.

**Error type strings changed.** `WorkflowFailureDetails.getErrorType()` returns the exception's fully
qualified name, so the strings moved with the classes. If you compare error types as strings, update
the literals — or better, compare against `SomeException.class.getName()`.

---

## 5. Workflows already running

This is the one that bites silently.

The error type is **persisted into workflow history**. A workflow that started before the upgrade and
resumes afterwards carries `io.dapr.durabletask.TaskFailedException` in its history, while the class
is now `io.dapr.workflows.task.exception.TaskFailedException`.

`WorkflowFailureDetails.isCausedBy(...)` resolves the persisted name reflectively. To keep those
instances working, the SDK maps the exception names that shipped under `io.dapr.durabletask` onto
their current types, so `isCausedBy` answers correctly across the upgrade:

```
TaskFailedException            TaskCanceledException
CompositeTaskFailedException   NonDeterministicOrchestratorException
PropagatedHistoryException     orchestration.exception.VersionNotRegisteredException
interruption.OrchestratorBlockedException
interruption.ContinueAsNewInterruption
DataConverter$DataConverterException
```

**What this covers:** `isCausedBy` on in-flight workflows, including compensation logic that branches
on the failure type. If your workflows only ever see SDK exception types, no action is needed.

Note that an error type the SDK cannot resolve answers `false` to *every* query — including broad
ones like `isCausedBy(RuntimeException.class)` — not just to an exact-type check. If you have
persisted history whose error type is neither an SDK exception nor a class on your own classpath,
expect `false` and a `WARN` line rather than a match.

**What it does not cover:** your own comparisons. If you do
`"io.dapr.durabletask.TaskFailedException".equals(details.getErrorType())`, that is now false for new
failures and true for old ones. Replace such comparisons with `isCausedBy(...)`.

An error type that cannot be resolved at all now logs at `WARN` and returns `false`, rather than
returning `false` silently.

Replay determinism is unaffected: the rethrown exception type is chosen by code, not by the persisted
string, so history written before the upgrade replays normally.

---

## 6. Java version

`dapr-sdk-workflows` is still compiled for **Java 17** and runs on Java 17 or later. **Java 21 or
later is now recommended.**

On Java 21+ the runtime's default executor is a virtual-thread-per-task executor; on 17 through 20 it
is a cached thread pool, exactly as before. You can always supply your own with
`WorkflowRuntimeBuilder.withExecutorService(...)` — an executor you supply is never shut down by the
runtime.

Virtual threads are on by default there. To opt out and keep the cached thread pool, set
`dapr.workflows.virtual.threads.enabled=false` (or `DAPR_WORKFLOWS_VIRTUAL_THREADS_ENABLED=false`).
The setting only affects the executor the runtime creates for itself; it has no effect on Java 17
through 20, or on a runtime given an executor via `withExecutorService(...)`.

Virtual threads help **activities**, which run your code and typically block on I/O. Workflow code
itself is replay-based and does not block, so it sees little benefit. See `SUPPORT.md` for the caveats
around pinning and unbounded concurrency.

---

## 7. Mechanical rewrite

Most of section 2 can be applied with a script. Review the diff afterwards; this only rewrites
imports and fully qualified references, and it does not handle the renames in section 3.

```bash
#!/usr/bin/env bash
# Rewrites v1 durable task imports to their unified homes.
# Usage: ./rewrite.sh [source-dir]   (defaults to src). Works on bash 3.2+.
set -eu

ROOT="${1:-src}"

# "<SimpleName> <new subpackage>", one per line.
MAP='
Task task
TaskActivity task
TaskActivityContext task
TaskActivityFactory task
TaskOrchestration task
OrchestratorFunction task
TaskFailedException task.exception
TaskCanceledException task.exception
CompositeTaskFailedException task.exception
NonDeterministicOrchestratorException task.exception
PropagatedHistory task.history
PropagatedHistoryException task.history
HistoryPropagationScope task.history
WorkflowResult task.history
ActivityResult task.history
ChildWorkflowResult task.history
DurableTaskClient task.client
DurableTaskGrpcClient task.client
DurableTaskGrpcClientBuilder task.client
OrchestrationMetadata task.client
NewOrchestrationInstanceOptions task.client
PurgeInstanceCriteria task.client
PurgeResult task.client
DurableTaskGrpcWorker task.worker
DurableTaskGrpcWorkerBuilder task.worker
DataConverter task.serialization
JacksonDataConverter task.serialization
'

count=0
while IFS= read -r file; do
  [ -n "$file" ] || continue
  count=$((count + 1))

  # Subpackages that keep their leaf name. Do these first: the type loop below
  # would otherwise rewrite the leaf and leave a stale parent package behind.
  perl -0pi -e '
    s/\bio\.dapr\.durabletask\.orchestration\.exception\.VersionNotRegisteredException\b/io.dapr.workflows.task.exception.VersionNotRegisteredException/g;
    s/\bio\.dapr\.durabletask\.interruption\./io.dapr.workflows.task.interruption./g;
    s/\bio\.dapr\.durabletask\.orchestration\./io.dapr.workflows.task.orchestration./g;
  ' "$file"

  # Longest names first, so PropagatedHistory does not clip PropagatedHistoryException.
  echo "$MAP" | grep -v '^$' | awk '{ print length($1), $1, $2 }' | sort -rn |
  while read -r _len type pkg; do
    perl -0pi -e "s/\\bio\\.dapr\\.durabletask\\.${type}\\b/io.dapr.workflows.${pkg}.${type}/g" "$file"
  done
done <<EOF
$(grep -rl 'io\.dapr\.durabletask' --include='*.java' "$ROOT" || true)
EOF

echo "Rewrote $count file(s). Remaining references to review by hand:"
grep -rn 'io\.dapr\.durabletask' --include='*.java' "$ROOT" || echo "  none"
```

Anything the script leaves behind is either a renamed type from section 3 or a reference to something
that is now internal.

---

## 8. Checklist

- [ ] Drop `io.dapr:durabletask-client` from every pom and build file
- [ ] Confirm no stale `durabletask-client` jar remains on the classpath (`mvn dependency:tree`)
- [ ] Rewrite `io.dapr.durabletask.*` imports (section 2 / section 7)
- [ ] Apply the renames in section 3
- [ ] Replace the four removed `DaprWorkflowClient` methods
- [ ] Swap `WorkflowInstanceStatus` for `WorkflowState`, and `getInstanceId()` for `getWorkflowId()`
- [ ] Implement the new `WorkflowContext` methods in any hand-written implementation or mock
- [ ] Replace string comparisons on `getErrorType()` with `isCausedBy(...)`
- [ ] Review `null` checks on `getMaxRetryInterval()` / `getRetryTimeout()`
- [ ] Consider moving to Java 21 (see `SUPPORT.md`)
