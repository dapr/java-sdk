# Finish sdk-tests Testcontainers Migration + Strip Legacy CI — Design

**Date:** 2026-07-09
**Status:** Draft (pending spec review)
**Author:** Siri Varma Vegiraju (with Claude)
**Scope:** [sdk-tests/](../../../sdk-tests/) module and [.github/workflows/build.yml](../../../.github/workflows/build.yml)
**Issue:** [dapr/java-sdk#1522](https://github.com/dapr/java-sdk/issues/1522) — Optimize CI/CD build times

## Problem

Issue #1522 reports CI timeouts in the integration-test job (the 30-min limit was hit; the
`build` job timeout has since been raised to 45 min). Job-level timing on a recent master run:

| Job | Duration |
|---|---|
| Unit tests | 4.6 min |
| Durable Task build & tests | 5.5 min |
| Build sb:4.0.x (Testcontainers, `spring-boot-4-sdk-tests`) | 11.1 min |
| **Build sb:3.5.x (`sdk-tests`, mixed) | 26.3 min** ← bottleneck |
| publish | 9.3 min |

The `sb:3.5.x` job runs the `sdk-tests` module. After PR #1754 (merged 2026-07-09), **13 of
its 20 integration tests already run on Testcontainers** (`BaseContainerIT`/`DaprContainer`),
but **7 still use the legacy `dapr run` CLI harness** (`BaseIT`/`DaprRun`/`AppRun`). Because
those 7 need the CLI, the `build` job must still install the Dapr CLI, run `dapr init`,
`dapr uninstall`, spin host Kafka via docker-compose, and install a host ToxiProxy binary —
and each legacy test shells out to `dapr run` per app. The Testcontainers-based `sb:4.0.x`
job doing comparable work finishes in less than half the time.

The only way to remove the CLI/`dapr init`/Kafka/ToxiProxy setup from CI — and collapse the
26-min bottleneck — is to migrate the **last 7 tests** off the legacy harness so nothing in
`sdk-tests` needs the Dapr CLI.

## The 7 remaining legacy tests

| # | IT | Legacy mechanism | Group |
|---|---|---|---|
| 1 | [ActorStateIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorStateIT.java) | Restart **app** mid-test; verify actor state persisted in Redis | A |
| 2 | [ActorTimerRecoveryIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorTimerRecoveryIT.java) | Restart **app** quickly (no sleep); verify in-memory timer survives | A |
| 3 | [ActorReminderRecoveryIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorReminderRecoveryIT.java) | Restart **daprd sidecar**; verify persisted reminder resumes | B |
| 4 | [ActorReminderFailoverIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorReminderFailoverIT.java) | Two apps + two sidecars; kill one app; verify reminder fails over | C |
| 5 | [WaitForSidecarIT](../../../sdk-tests/src/test/java/io/dapr/it/resiliency/WaitForSidecarIT.java) | ToxiProxy latency + a not-running sidecar; verify `waitForSidecar` timeout semantics | D |
| 6 | [ActorSdkResiliencyIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorSdkResiliencyIT.java) | ToxiProxy latency/jitter between actor client and sidecar (its one `@Test` is `@Disabled`) | D |
| 7 | [BindingIT](../../../sdk-tests/src/test/java/io/dapr/it/binding/http/BindingIT.java) | Kafka input/output bindings + two HTTP output-binding error tests | E |

## Goals

- Migrate all 7 ITs to extend `BaseContainerIT` and run Dapr in a `DaprContainer`.
- Add the small set of reusable helpers these tests need to `BaseContainerIT` (app restart,
  sidecar restart, shared-placement/scheduler multi-sidecar).
- Add a `KafkaContainer` for `BindingIT` and a `ToxiproxyContainer` pattern for the Group-D tests.
- Remove the Dapr CLI, `dapr init`/`dapr uninstall`, host Kafka (docker-compose), and host
  ToxiProxy steps from the `build` job in [build.yml](../../../.github/workflows/build.yml).
- Delete the now-dead legacy harness (`BaseIT`, `DaprRun`, `DaprRunConfig`, `ToxiProxyRun`) and
  the file-based component/config YAMLs and `deploy/local-test.yml` that only the 7 tests used.
- Land as a single PR (single cutover).

## Non-Goals

- Migrating the two `durabletask-client` ITs. They run in the separate `build-durabletask`
  job, which uses its own sidecar container (not the Dapr CLI) and is untouched.
- **Re-enabling** `ActorSdkResiliencyIT`'s disabled test. It is migrated off the legacy harness
  but stays `@Disabled` — re-enabling a known-flaky test is out of scope and contrary to #1754's
  flakiness cleanup.
- Making `BindingIT`'s two HTTP output-binding tests hermetic. They keep pointing at
  `https://api.github.com/unknown_path` (behavior preserved; a prior switch to httpbin was
  reverted). Removing that external dependency is tracked as future work.
- Replacing `AppRun`'s `mvn exec:java` subprocess model. The app side stays a subprocess; only
  the sidecar/backing-services are containerized. `AppRun`, `DaprPorts`, `Command`, `Stoppable`
  are retained because `BaseContainerIT` depends on them.
- Parallelizing Surefire/Failsafe or sharding the CI matrix (separate, deferred approaches).

## Decisions

| # | Decision | Rationale |
|---|---|---|
| D1 | Migrate all 7 in one PR, then strip CI legacy setup in the same PR | The CI win is only realized when the last legacy test is gone; a partial migration keeps the CLI/init/Kafka/ToxiProxy setup and barely moves CI time. |
| D2 | Put reusable mechanics in `BaseContainerIT` (`restartApp`, `restartSidecar`, shared-placement multi-sidecar helper) | Keeps the 7 tests thin and consistent; mirrors how `BaseContainerIT` already centralizes lifecycle. |
| D3 | Keep `BindingIT` HTTP tests on the external GitHub URL | Preserves behavior, minimal blast radius; hermeticity is orthogonal to removing `dapr run`. |
| D4 | `ActorSdkResiliencyIT` migrated but stays `@Disabled` | Removes the last `ToxiProxyRun` consumer without re-introducing a flaky test into CI. |
| D5 | Reminders survive daprd-container restart via the **scheduler container** | Dapr 1.18 stores actor reminders in the scheduler service; restarting only the daprd container (placement + scheduler + Redis stay up) preserves them. |
| D6 | Failover uses **explicit shared** placement + scheduler containers via `withPlacementContainer`/`withSchedulerContainer` | `withReusablePlacement(true)` relies on Testcontainers reuse, which is disabled on CI hosts; explicit shared containers make the two-sidecar topology deterministic. Pattern already proven in `WorkflowsMultiAppCallActivityIT`. |
| D7 | Add `org.testcontainers:testcontainers-kafka` (BOM-managed, test scope) for `BindingIT` | Kafka Testcontainers is not currently a dependency; Testcontainers 2.0.5 module artifactIds are `testcontainers-`-prefixed and versionless (managed by `testcontainers-bom`). |
| D8 | Single spec + single staged implementation plan | Matches the single-cutover decision; plan tasks are ordered and independently reviewable. |

## Architecture

All new mechanics live in [`BaseContainerIT`](../../../sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java),
which already provides `daprBuilder`, `startAppAndAttach`, `newDaprClient`/`newActorClient`,
`redisStateStore`, `waitForActorsReady`, and LIFO `deferStop`/`deferClose` cleanup. Additions:

### 1. `restartApp(AppRun app)` — Group A

Stops and restarts the app subprocess on its **same** pre-allocated port. daprd stays up; since
`daprBuilder` configures **no** app-health-check, daprd does not deactivate actors during the
gap, so in-memory timers survive (matches the legacy `@DaprRunConfig(enableAppHealthCheck=false)`
on `MyActorService`). For `ActorTimerRecoveryIT` the restart must be "quick" (no sleep between
stop and start) — the helper calls `app.stop()` immediately followed by `app.start()`.

```java
protected static void restartApp(AppRun app) throws Exception {
  app.stop();
  app.start();   // rebinds the same host app port; daprd reconnects via host.testcontainers.internal:appPort
}
```

### 2. `restartSidecar(DaprContainer dapr)` — Group B

Stops and restarts the daprd container, then re-verifies readiness. Placement + scheduler are
instance fields on `DaprContainer` that are **not** recreated on the second `start()` (the
`if (placementContainer == null)` / `if (schedulerContainer == null)` guards in
`DaprContainer.configure()`), so they persist across the restart — and so does the reminder,
which the scheduler owns. Pinned `setPortBindings` re-bind the same host ports, so the app's
`DAPR_HTTP_PORT`/`DAPR_GRPC_PORT` and any `DaprClient` remain valid.

```java
protected static void restartSidecar(DaprContainer dapr) {
  dapr.stop();
  dapr.start();                       // reuses existing placement + scheduler; re-binds pinned ports
  try (DaprClient c = newDaprClient(dapr)) { c.waitForSidecar(30_000).block(); }
  waitForActorsReady(dapr);
}
```

`ActorReminderRecoveryIT` uses this in place of the legacy `DaprRun.stop()`/`start()`, keeping
its "pause 10s then 7s" waits so placement/health settle.

### 3. Shared-placement multi-sidecar helper — Group C

A helper (and/or a documented pattern) that builds two `DaprContainer` app-sidecars sharing one
`DaprPlacementContainer` and one `DaprSchedulerContainer` on `SharedTestInfra.network()`, each
attached to its own `AppRun` via the existing two-phase startup. Modeled directly on
[`WorkflowsMultiAppCallActivityIT`](../../../sdk-tests/src/test/java/io/dapr/it/testcontainers/workflows/multiapp/WorkflowsMultiAppCallActivityIT.java)
(lines 57-111): `new DaprPlacementContainer(...).withNetworkAliases("placement")`, ditto
scheduler, then each daprd `.withPlacementContainer(shared).withSchedulerContainer(shared)`.

`ActorReminderFailoverIT` needs each sidecar's pinned HTTP port so it can match the actor host
returned by `MyActorBase.getIdentifier()` (which returns `DAPR_HTTP_PORT`) and kill the correct
`AppRun`. The helper therefore exposes both `(DaprContainer, AppRun)` pairs and their pinned ports.

### 4. ToxiProxy wiring — Group D

Copy the proven pattern from the already-migrated
[`SdkResiliencyIT`](../../../sdk-tests/src/test/java/io/dapr/it/resiliency/SdkResiliencyIT.java):
a `ToxiproxyContainer` (image `ContainerConstants.TOXI_PROXY_IMAGE_TAG` =
`ghcr.io/shopify/toxiproxy:2.5.0`) on the shared network, `DaprContainer` with
`.withNetworkAliases("dapr")`, a proxy `dapr:3500`/`dapr:50001`, and a `DaprClient`/`ActorClient`
pointed at `TOXIPROXY.getMappedPort(...)`. Toxics (latency/timeout) are added/removed per test
via the `ToxiproxyClient` API. `WaitForSidecarIT` needs no app; `ActorSdkResiliencyIT` combines
this with `startAppAndAttach` for its actor app. For `WaitForSidecarIT`'s "not running" case,
point the client at a stopped container's former port (or an unbound port) so the connection fails.

### 5. `KafkaContainer` for BindingIT — Group E

Add `org.testcontainers:testcontainers-kafka` (test scope, BOM-managed). Start a `KafkaContainer`
on `SharedTestInfra.network()` with alias `kafka`; build the `bindings.kafka` component in-code
with `brokers` = the internal listener (`kafka:9092`), `topics`/`publishTopic`/`consumerGroup`
using the `{appID}` placeholder as today, `authRequired=false`, `initialOffset=oldest`. The
`InputBindingService` app is started via `startAppAndAttach`. The two HTTP output-binding
components (`github-http-binding-404`, `github-http-binding-404-success`) are declared in-code as
`bindings.http` components (URL unchanged), replacing the file-based `http_binding.yaml`.

### Coexistence during migration

`AppRun`, `DaprPorts`, `Command`, `Stoppable` stay (consumed by `BaseContainerIT`). Once all 7
tests are migrated, `BaseIT`, `DaprRun`, `DaprRunConfig`, and `ToxiProxyRun` have no remaining
references in `sdk-tests` and are deleted (verified by grep per file before deletion).

## Per-IT migration matrix

| # | IT | App service class / actor type | Components (in-code) | New mechanics |
|---|---|---|---|---|
| 1 | ActorStateIT | `StatefulActorService` / `StatefulActorTest` | `redisStateStore("statestore")` | `startAppAndAttach` + `restartApp` (verify Redis-persisted state after app restart) |
| 2 | ActorTimerRecoveryIT | `MyActorService` / `MyActorTest` | `redisStateStore` | `startAppAndAttach` + `restartApp` (quick, no-sleep) |
| 3 | ActorReminderRecoveryIT | `MyActorService` (+ separate client app) | `redisStateStore` | `startAppAndAttach` + `restartSidecar` |
| 4 | ActorReminderFailoverIT | 2× `MyActorService` + 1 client | `redisStateStore` (shared) | shared-placement multi-sidecar helper; kill one `AppRun` |
| 5 | WaitForSidecarIT | none | none (in-memory kvstore ok) | ToxiProxy pattern; one stopped/unavailable endpoint |
| 6 | ActorSdkResiliencyIT | `DemoActorService` / `DemoActorTest` | default | ToxiProxy pattern + `startAppAndAttach`; test stays `@Disabled` |
| 7 | BindingIT | `InputBindingService` | `bindings.kafka` (Kafka container), 2× `bindings.http` | `KafkaContainer` + `startAppAndAttach` |

Each migrated IT drops `extends BaseIT`, all `startDaprApp`/`DaprRun`/`DaprPorts`/`DaprRunConfig`
imports, and file-based component lookups; it defines components in-code via the `Component`
model and uses the `BaseContainerIT` client/actor factories.

## CI changes ([.github/workflows/build.yml](../../../.github/workflows/build.yml), `build` job)

| Step | Disposition |
|---|---|
| Set up Dapr CLI (`wget … install.sh`) | **Remove** |
| Set up Go + checkout `dapr/cli` + `dapr/dapr` overrides + build daprd/placement | **Remove** (all `DAPR_REF`/`DAPR_CLI_REF` conditional blocks) |
| `dapr uninstall --all` + "Ensure Dapr runtime uninstalled" wait loop | **Remove** |
| `dapr init --runtime-version …` | **Remove** |
| `docker compose -f ./sdk-tests/deploy/local-test.yml up -d kafka` | **Remove** (Kafka now via Testcontainers) |
| Install host ToxiProxy (`wget toxiproxy-server`) | **Remove** (ToxiProxy now via Testcontainers) |
| `docker version` check | Keep (Testcontainers needs Docker) |
| Checkout / setup-java / `./mvnw clean install -DskipTests` | Keep |
| Integration-test run (sb 3.x / sb 4.x) + failsafe/surefire report uploads | Keep (test discovery surface unchanged) |
| `DAPR_CLI_VER` / `DAPR_RUNTIME_VER` / `DAPR_INSTALL_URL` / `TOXIPROXY_URL` / `GOVER…` env | **Remove** (no longer referenced) |

Also delete `sdk-tests/deploy/local-test.yml` (Kafka/Zookeeper), unless another consumer is found.
`build-durabletask`, `test` (unit), and `publish` jobs are untouched.

## Dead-code & resource cleanup

After migration, delete (verifying zero remaining references first):
- `sdk-tests/.../io/dapr/it/BaseIT.java`, `DaprRun.java`, `DaprRunConfig.java`, `ToxiProxyRun.java`
- The file-based YAMLs only these tests loaded: Kafka binding, HTTP binding, and any
  `components/`/`configurations/` files with no remaining `withComponent(Path)` or CLI consumer.
- `sdk-tests/deploy/local-test.yml`

Retain: `AppRun`, `DaprPorts`, `Command`, `Stoppable`, `SharedTestInfra`, `BaseContainerIT`.

## Risks & mitigations

| Risk | Mitigation |
|---|---|
| **Timer/reminder tests are timing-sensitive and could flake in containers** (cf. the known `ActorTurnBasedConcurrencyIT` timer-count flakiness) | Preserve the legacy waits; prefer `Awaitility`/`callWithRetry` with generous timeouts over fixed sleeps for assertions; validate each locally across repeated runs before finalizing. |
| Reminder does **not** survive daprd restart (if scheduler assumption is wrong) | Verify on Dapr 1.18 that scheduler owns reminders and stays up across restart; if not, fall back to keeping the reminder in Redis-backed state and reloading. Prove `ActorReminderRecoveryIT` locally first. |
| Testcontainers **reuse disabled on CI** breaks shared placement for failover | Use explicit shared placement/scheduler containers (D6), not `withReusable*`. |
| Killing one app in failover doesn't migrate the actor within the window | Match the legacy 10s failover wait; assert via `getIdentifier()` host change with retry. |
| More containers per class (2 sidecars + placement + scheduler + Kafka/ToxiProxy) lengthen cold runs | Acceptable: removes host CLI/init/uninstall overhead and per-test `dapr run`; net expected win is large (target the ~11 min the sb:4.0.x job already hits). |
| Kafka container startup adds time to `BindingIT` | Single Kafka container per class; `KafkaContainer` boots in a few seconds; still far cheaper than host Kafka + CLI. |
| `host.testcontainers.internal` differences dev vs CI | Handled by `Testcontainers.exposeHostPorts` (already used by `startAppAndAttach`); CI is Linux. |

## Testing strategy

- Each migrated IT runs locally via `cd sdk-tests && ../mvnw verify -Dit.test=<ClassName>` and
  passes; the timing-sensitive ones (Timer/Reminder recovery, Failover) are run repeatedly to
  check for flakiness before finalizing.
- Full `sdk-tests` `verify` passes locally and on CI with the trimmed `build.yml`.
- The 13 already-migrated ITs continue to pass unchanged.
- No new unit tests for the `BaseContainerIT` helpers; they are exercised by the migrated ITs.
- CI acceptance: `build sb:3.5.x` completes well under the 45-min timeout and materially faster
  than 26 min (target: comparable to the sb:4.0.x job).

## Open questions

None at spec-approval time. The implementation plan will pin: the exact `testcontainers-kafka`
coordinates/version from the BOM, the `KafkaContainer` image and internal-listener config, the
`ToxiproxyContainer` proxy wiring for the actor gRPC channel in `ActorSdkResiliencyIT`, and the
precise `restartSidecar` wait sequence.

## Out of scope (future work)

- Making `BindingIT` HTTP tests hermetic (local stub instead of GitHub).
- Re-enabling `ActorSdkResiliencyIT`.
- Migrating the two `durabletask-client` ITs.
- Surefire/Failsafe parallelization or CI matrix sharding.
