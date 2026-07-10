# Finish sdk-tests Testcontainers Migration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the last 7 `sdk-tests` integration tests off the legacy `dapr run` CLI harness onto `BaseContainerIT`/`DaprContainer`, then strip the Dapr CLI / `dapr init` / host-Kafka / host-ToxiProxy steps from the CI `build` job.

**Architecture:** Each test drops `extends BaseIT` and its `DaprRun`/`startDaprApp` usage, defining Dapr components in-code and running the sidecar in a `DaprContainer`. New reusable mechanics (`restartApp`, `restartSidecar`, shared placement/scheduler, ToxiProxy, Kafka) are added to `BaseContainerIT`/`SharedTestInfra`, each introduced in the first task that consumes it so it is exercised immediately. Once all 7 are migrated, the dead legacy harness and CI setup are deleted.

**Tech Stack:** Java 17, JUnit 5, Testcontainers 2.0.5 (`org.testcontainers:testcontainers-*`, BOM-managed), `io.dapr:testcontainers-dapr` (`DaprContainer`, `DaprPlacementContainer`, `DaprSchedulerContainer`), Dapr runtime 1.18.0, Maven Failsafe (`integration-tests` profile).

**Spec:** [docs/superpowers/specs/2026-07-09-finish-sdk-tests-testcontainers-migration-design.md](../specs/2026-07-09-finish-sdk-tests-testcontainers-migration-design.md)

---

## Conventions for every task

- **Reference patterns to copy** (already-migrated, working):
  - Actor + app: [ActivationDeactivationIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActivationDeactivationIT.java)
  - ToxiProxy + `DaprContainer`: [SdkResiliencyIT.java](../../../sdk-tests/src/test/java/io/dapr/it/resiliency/SdkResiliencyIT.java)
  - Shared placement/scheduler multi-sidecar: [WorkflowsMultiAppCallActivityIT.java](../../../sdk-tests/src/test/java/io/dapr/it/testcontainers/workflows/multiapp/WorkflowsMultiAppCallActivityIT.java)
- **Base class API to use:** [BaseContainerIT.java](../../../sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java) — `daprBuilder(appName)`, `startAppAndAttach(name, serviceClass, protocol, daprFactory)`, `newDaprClient(dapr)`, `newActorClient(dapr)`, `redisStateStore(name)`, `waitForActorsReady(dapr)`, `deferStop(...)`, `deferClose(...)`, `DAPR_IMAGE`, `STATE_STORE_NAME`.
- **When rewriting a test:** preserve every assertion, `Thread.sleep`/`callWithRetry`/`Awaitility` wait and its duration, and the actor type / service class exactly as in the legacy file. Only the *setup/teardown* and *restart mechanics* change from `DaprRun` to `DaprContainer`.
- **Per-test verification command** (requires a running Docker daemon; Testcontainers uses it):
  ```bash
  cd /Users/svegiraju/Git/java-sdk && \
  ./mvnw -B -pl sdk-tests -Pintegration-tests -Dit.test=<ClassName> \
    dependency:copy-dependencies verify
  ```
  Expected: `BUILD SUCCESS`, the named IT runs (not skipped, except `ActorSdkResiliencyIT` whose one test is `@Disabled`). If the local Docker daemon is unavailable, state that the check must run in CI and do not claim local success.
- **Commits:** sign off every commit (`git commit -s`). Do NOT add a Co-Authored-By trailer.
- **Timing-sensitive tests** (Tasks 2, 3, 4): after the first pass, run the verification command **3 times** and confirm all 3 pass before committing; if any flakes, escalate (do not loosen assertions without re-planning).

---

## Task 1: Migrate ActorStateIT + add `restartApp` helper (Group A)

**Files:**
- Modify: `sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java` (add `restartApp`)
- Rewrite: `sdk-tests/src/test/java/io/dapr/it/actors/ActorStateIT.java`

**Legacy behavior to preserve** (read [ActorStateIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorStateIT.java)): single `writeReadState()` test. Phase 1 — build an `ActorProxy` for actor type `StatefulActorTest` (service class `StatefulActorService`), write string/`MyData`/name/empty-name/bytes state, reading each back. Phase 2 — **restart the app**, rebuild the proxy with the **same** `actorId`, and assert every previously-written value is still readable (proves state persisted to Redis). Keep all `callWithRetry` timeouts and the 5s/10s/2s waits.

- [ ] **Step 1: Add `restartApp` to `BaseContainerIT`** (place after `startAppAndAttach`, before the client factories):

```java
  /**
   * Restarts the app subprocess on its same pre-allocated port. The daprd
   * container stays up and reconnects to the app via
   * {@code host.testcontainers.internal:appPort}. Because {@link #daprBuilder}
   * configures NO app-health-check, daprd does not deactivate actors during the
   * gap, so in-memory timers survive (matching the legacy
   * {@code @DaprRunConfig(enableAppHealthCheck=false)}). There is intentionally
   * no sleep between stop and start — {@code ActorTimerRecoveryIT} relies on a
   * quick restart.
   */
  protected static void restartApp(AppRun app) throws Exception {
    app.stop();
    app.start();
  }
```

- [ ] **Step 2: Rewrite `ActorStateIT`** to `extends BaseContainerIT`. Use the `ActivationDeactivationIT` `@BeforeAll` shape but with `StatefulActorService.class`:

```java
var pair = startAppAndAttach(
    "actor-state-it",
    io.dapr.it.actors.services.springboot.StatefulActorService.class,
    AppRun.AppProtocol.HTTP,
    appPort -> daprBuilder("actor-state-it")
        .withAppPort(appPort)
        .withAppChannelAddress("host.testcontainers.internal")
        .withComponent(redisStateStore(STATE_STORE_NAME)));
dapr = pair.dapr();
app = pair.app();
waitForActorsReady(dapr);
```
  Build the proxy via `new ActorProxyBuilder("StatefulActorTest", ActorProxy.class, newActorClient(dapr))`. Replace the legacy "stop `DaprRun`, `startDaprApp` again" restart with `restartApp(app)`, then **rebuild the proxy off a fresh `newActorClient(dapr)`** with the same `actorId`. Remove all `BaseIT`/`DaprRun`/`startDaprApp` imports and usage.

- [ ] **Step 3: Verify** — run the per-test command with `-Dit.test=ActorStateIT`. Expected: PASS, `writeReadState` runs and green.

- [ ] **Step 4: Commit**
```bash
git add sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java \
        sdk-tests/src/test/java/io/dapr/it/actors/ActorStateIT.java
git commit -s -m "test: migrate ActorStateIT to Testcontainers (+ restartApp helper)"
```

---

## Task 2: Migrate ActorTimerRecoveryIT (Group A)

**Files:**
- Rewrite: `sdk-tests/src/test/java/io/dapr/it/actors/ActorTimerRecoveryIT.java`

**Legacy behavior to preserve** (read [ActorTimerRecoveryIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorTimerRecoveryIT.java)): single `timerRecoveryTest()`. Actor type `MyActorTest`, service class `MyActorService`. Register a timer (`startTimer("myTimer")`, callback `clock`, ~2s delay / 3s period, message `"ping!"`); wait for ≥3 fires (30s `callWithRetry`); **restart the app with NO sleep between stop and start**; assert the timer keeps firing (≥3 more, 30s `callWithRetry`); assert none of the new log entries equal old entries (proves real restart); `stopTimer("myTimer")` at the end.

- [ ] **Step 1: Rewrite `ActorTimerRecoveryIT`** to `extends BaseContainerIT`, mirroring Task 1's `startAppAndAttach` setup but with `MyActorService.class` and app name `"actor-timer-recovery-it"`, actor type `MyActorTest`. Replace `runs.left.stop(); runs.left.start();` with `restartApp(app);` (the helper already omits any inter-step sleep). Keep every wait/assertion.

- [ ] **Step 2: Verify (×3)** — run `-Dit.test=ActorTimerRecoveryIT` three times; all PASS. The timer must survive the restart (relies on daprBuilder setting no health-check — see the invariant note in the spec). If the timer is lost, escalate.

- [ ] **Step 3: Commit**
```bash
git add sdk-tests/src/test/java/io/dapr/it/actors/ActorTimerRecoveryIT.java
git commit -s -m "test: migrate ActorTimerRecoveryIT to Testcontainers"
```

---

## Task 3: Migrate ActorReminderRecoveryIT + add `restartSidecar` helper (Group B)

**Files:**
- Modify: `sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java` (add `restartSidecar`)
- Rewrite: `sdk-tests/src/test/java/io/dapr/it/actors/ActorReminderRecoveryIT.java`

**Legacy behavior to preserve** (read [ActorReminderRecoveryIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorReminderRecoveryIT.java)): `@ParameterizedTest` over 4 data variants (String `"36"`, String `"\"my_text\""`, `byte[]{0,1}`, object `{"name":"abc","age":30}`) with actor types `MyActorTest`/`MyActorBinaryTest`/`MyActorObjectImpl`. Register reminder (fires ~every 2s); wait for ≥3 fires; **stop the daprd sidecar**; sleep 10s; **start the sidecar**; sleep 7s; assert the reminder resumed and fired ≥3 more times. Per the spec, the reminder lives in the scheduler container which survives the daprd restart. Keep the separate actor-client (build via `newActorClient(dapr)`, NOT a second app — see spec note).

- [ ] **Step 1: Add `restartSidecar` to `BaseContainerIT`** (after `restartApp`):

```java
  /**
   * Restarts the daprd container in place and re-waits for readiness. Placement
   * and scheduler are NOT recreated on the second start (their DaprContainer
   * fields are non-null), so a persisted actor reminder survives. Pinned host
   * ports re-bind, so the app's DAPR_HTTP_PORT/DAPR_GRPC_PORT and any DaprClient
   * remain valid.
   */
  protected static void restartSidecar(DaprContainer dapr) {
    dapr.stop();
    dapr.start();
    try (DaprClient client = newDaprClient(dapr)) {
      client.waitForSidecar(30_000).block();
    }
    waitForActorsReady(dapr);
  }
```

- [ ] **Step 2: Rewrite `ActorReminderRecoveryIT`** to `extends BaseContainerIT`. Use `startAppAndAttach` with `MyActorService.class` (app name `"actor-reminder-recovery-it"`, `redisStateStore`). Build the reminder proxy(ies) via `newActorClient(dapr)`. Replace the legacy `runs.right.stop()` / `runs.right.start()` sidecar restart with `restartSidecar(dapr)`, keeping the surrounding 10s and 7s sleeps. Preserve the parameterized data and all assertions.

- [ ] **Step 3: Verify (×3)** — run `-Dit.test=ActorReminderRecoveryIT` three times; all PASS across all 4 parameterized variants. If reminders do not survive the restart, escalate (fallback in spec: reminder in Redis-backed state).

- [ ] **Step 4: Commit**
```bash
git add sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java \
        sdk-tests/src/test/java/io/dapr/it/actors/ActorReminderRecoveryIT.java
git commit -s -m "test: migrate ActorReminderRecoveryIT to Testcontainers (+ restartSidecar helper)"
```

---

## Task 4: Migrate ActorReminderFailoverIT + add shared placement/scheduler helpers (Group C)

**Files:**
- Modify: `sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java` (add shared placement/scheduler helpers)
- Rewrite: `sdk-tests/src/test/java/io/dapr/it/actors/ActorReminderFailoverIT.java`

**Legacy behavior to preserve** (source shown above): `reminderRecoveryTest()` — two `MyActorService` app-sidecars (`One`, `Two`) hosting actor type `MyActorTest`, plus a **client-only** sidecar (no app) whose `ActorClient` builds the proxy. Register reminder `myReminder`; wait 7s; assert ≥3 fires; read the actor host via `getIdentifier()` (returns the host sidecar's `DAPR_HTTP_PORT`); **stop the `AppRun` whose sidecar port matches**; sleep 10s; sleep 10s more; assert ≥4 additional fires; assert `getIdentifier()` now returns a **different** host. `@AfterEach` calls `stopReminder`.

**Topology:** failover is keyed on **actor type**, not app-id — placement distributes `MyActorTest` across every sidecar that registered it and rebalances when one host's app dies. So all sidecars must share ONE placement + ONE scheduler + Redis.

- [ ] **Step 1: Add shared control-plane helpers to `BaseContainerIT`** (import `io.dapr.testcontainers.DaprPlacementContainer`, `io.dapr.testcontainers.DaprSchedulerContainer`, `io.dapr.testcontainers.DaprContainerConstants`):

```java
  /** Shared placement for multi-sidecar ITs. Explicit (not reuse-based) so it is
   *  deterministic on CI where Testcontainers reuse is disabled. */
  protected static DaprPlacementContainer startSharedPlacement() {
    DaprPlacementContainer placement =
        new DaprPlacementContainer(DaprContainerConstants.DAPR_PLACEMENT_IMAGE_TAG)
            .withNetwork(SharedTestInfra.network())
            .withNetworkAliases("placement")
            .withReuse(false);
    placement.start();
    deferStop(placement);
    return placement;
  }

  /** Shared scheduler for multi-sidecar ITs (owns actor reminders). */
  protected static DaprSchedulerContainer startSharedScheduler() {
    DaprSchedulerContainer scheduler =
        new DaprSchedulerContainer(DaprContainerConstants.DAPR_SCHEDULER_IMAGE_TAG)
            .withNetwork(SharedTestInfra.network())
            .withNetworkAliases("scheduler")
            .withReuse(false);
    scheduler.start();
    deferStop(scheduler);
    return scheduler;
  }
```

- [ ] **Step 2: Rewrite `ActorReminderFailoverIT`** to `extends BaseContainerIT`. In `@BeforeEach`:
  1. `DaprPlacementContainer placement = startSharedPlacement(); DaprSchedulerContainer scheduler = startSharedScheduler();`
  2. Start two actor hosts with `startAppAndAttach("failover-one" / "failover-two", MyActorService.class, HTTP, factory)` where each `factory` is:
     ```java
     appPort -> daprBuilder(name)
         .withPlacementContainer(placement)
         .withSchedulerContainer(scheduler)
         .withAppPort(appPort)
         .withAppChannelAddress("host.testcontainers.internal")
         .withComponent(redisStateStore(STATE_STORE_NAME))
     ```
     (Passing an explicit placement/scheduler makes `DaprContainer.configure()` skip creating its own — the shared ones are used.)
  3. Start a **client-only** sidecar (no app): build `new DaprContainer(DAPR_IMAGE).withAppName("failover-client").withNetwork(SharedTestInfra.network()).withPlacementContainer(placement).withSchedulerContainer(scheduler).withComponent(redisStateStore(STATE_STORE_NAME)).withDaprLogLevel(DaprLogLevel.DEBUG)`, `.start()`, `deferStop(...)`. Build the proxy via `newActorClient(clientSidecar)`.
  4. `waitForActorsReady` on both actor-host sidecars.
  5. Keep the `Thread.sleep(3000)` and actor id / type (`MyActorTest`).
  In the test body, replace `firstAppRun.getHttpPort()` / `secondAppRun.getHttpPort()` comparisons with the two host sidecars' `dapr.getHttpPort()`, and `firstAppRun.stop()` with the matching `AppRun.stop()` (the `AppRun` from the corresponding `startAppAndAttach` pair). Preserve every wait/assertion.

- [ ] **Step 3: Verify (×3)** — run `-Dit.test=ActorReminderFailoverIT` three times; all PASS (actor migrates to the surviving host and reminder continues). This is the highest-risk migration; if placement does not fail over across sidecars, STOP and escalate to re-plan rather than adjusting assertions.

- [ ] **Step 4: Commit**
```bash
git add sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java \
        sdk-tests/src/test/java/io/dapr/it/actors/ActorReminderFailoverIT.java
git commit -s -m "test: migrate ActorReminderFailoverIT to Testcontainers (+ shared placement/scheduler)"
```

---

## Task 5: Migrate WaitForSidecarIT + add `newToxiproxy` helper (Group D)

**Files:**
- Modify: `sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java` (add `newToxiproxy`)
- Rewrite: `sdk-tests/src/test/java/io/dapr/it/resiliency/WaitForSidecarIT.java`

**Legacy behavior to preserve** (read [WaitForSidecarIT.java](../../../sdk-tests/src/test/java/io/dapr/it/resiliency/WaitForSidecarIT.java) and copy the ToxiProxy wiring from [SdkResiliencyIT.java](../../../sdk-tests/src/test/java/io/dapr/it/resiliency/SdkResiliencyIT.java)): four tests — `waitSucceeds()` (direct sidecar, `waitForSidecar(5000)` OK), `waitTimeout()` (5s latency toxic, timeout 4900ms → `RuntimeException`, duration ≥ timeout), `waitSlow()` (timeout 5100ms → OK, duration ≥ 5s), `waitNotRunningTimeout()` (unavailable sidecar, timeout 5000ms → `RuntimeException`). Latency constant = 5s. No app needed.

- [ ] **Step 1: Add `newToxiproxy` to `BaseContainerIT`** (import `org.testcontainers.containers.ToxiproxyContainer` and `io.dapr.it.testcontainers.ContainerConstants`):

```java
  /** Starts a Toxiproxy container on the shared network and registers it for
   *  @AfterAll cleanup. Callers create proxies via a ToxiproxyClient against
   *  {@code getHost()}/{@code getControlPort()} and point clients at the mapped
   *  proxy listen port. Mirrors SdkResiliencyIT. */
  protected static ToxiproxyContainer newToxiproxy() {
    ToxiproxyContainer toxiproxy =
        new ToxiproxyContainer(ContainerConstants.TOXI_PROXY_IMAGE_TAG)
            .withNetwork(SharedTestInfra.network());
    toxiproxy.start();
    deferStop(toxiproxy);
    return toxiproxy;
  }
```

- [ ] **Step 2: Rewrite `WaitForSidecarIT`** to `extends BaseContainerIT`. In `@BeforeAll`: start a `DaprContainer` via `daprBuilder("wait-for-sidecar-it").withNetworkAliases("dapr")` (in-memory kvstore is fine, no app), `deferStop`; `newToxiproxy()`; create a proxy `dapr:3500`/`dapr:50001` via `ToxiproxyClient` (see `SdkResiliencyIT` lines 122-123). Build the "latency" client pointed at `toxiproxy.getMappedPort(<listen>)` and the direct client via `newDaprClient(dapr)`. For `waitNotRunningTimeout`, point a client at an unbound/stopped endpoint (e.g. a proxy with no upstream, or a stopped container's former mapped port). Add/remove the 5s latency toxic per test as in `SdkResiliencyIT`. Preserve all four tests, their timeouts, and duration assertions.

- [ ] **Step 3: Verify** — run `-Dit.test=WaitForSidecarIT`. Expected: PASS, all 4 tests run.

- [ ] **Step 4: Commit**
```bash
git add sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java \
        sdk-tests/src/test/java/io/dapr/it/resiliency/WaitForSidecarIT.java
git commit -s -m "test: migrate WaitForSidecarIT to Testcontainers (+ newToxiproxy helper)"
```

---

## Task 6: Migrate ActorSdkResiliencyIT (Group D — stays @Disabled)

**Files:**
- Rewrite: `sdk-tests/src/test/java/io/dapr/it/actors/ActorSdkResiliencyIT.java`

**Legacy behavior to preserve** (read [ActorSdkResiliencyIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorSdkResiliencyIT.java)): one `@Test retryAndTimeout()` that is `@Disabled("Flaky when running on GitHub actions")`. Actor type `DemoActorTest`, service class `DemoActorService`. It routes an `ActorClient` (with `ResiliencyOptions`) through ToxiProxy latency/jitter and compares error counts across retry configs. **Keep the test `@Disabled`** — the goal is only to remove the `ToxiProxyRun` dependency, not to re-enable it.

- [ ] **Step 1: Rewrite `ActorSdkResiliencyIT`** to `extends BaseContainerIT`. Combine the actor pattern (`startAppAndAttach` with `DemoActorService.class`, app name `"actor-sdk-resiliency-it"`) with the ToxiProxy wiring from Task 5 (`newToxiproxy()` + a proxy to `dapr:50001`). Build the resilient `ActorClient`s against the ToxiProxy mapped gRPC port using `new ActorClient(new Properties(overrides), resiliencyOptions)` where `overrides` map `GRPC_ENDPOINT`/`GRPC_PORT` to the proxy. Remove `BaseIT`/`DaprRun`/`ToxiProxyRun` imports and usage. Preserve the `@Disabled` annotation and the test body's assertions.

- [ ] **Step 2: Verify** — run `-Dit.test=ActorSdkResiliencyIT`. Expected: `BUILD SUCCESS`; the class compiles and its one test is reported **skipped** (still `@Disabled`). This confirms the migration compiles and no longer references the legacy harness.

- [ ] **Step 3: Commit**
```bash
git add sdk-tests/src/test/java/io/dapr/it/actors/ActorSdkResiliencyIT.java
git commit -s -m "test: migrate ActorSdkResiliencyIT off ToxiProxyRun (kept @Disabled)"
```

---

## Task 7: Migrate BindingIT + Kafka container (Group E)

**Files:**
- Modify: `sdk-tests/pom.xml` (add `testcontainers-kafka`)
- Modify: `sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfra.java` (add `kafka()`)
- Modify: `sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java` (add `kafkaBinding` + `httpBinding` component helpers)
- Rewrite: `sdk-tests/src/test/java/io/dapr/it/binding/http/BindingIT.java`

**Legacy behavior to preserve** (read [BindingIT.java](../../../sdk-tests/src/test/java/io/dapr/it/binding/http/BindingIT.java)): three tests. `httpOutputBindingError()` — invoke `github-http-binding-404` → expect `DaprException` 404. `httpOutputBindingErrorIgnoredByComponent()` — invoke `github-http-binding-404-success` (`errorIfNot2XX=false`) → expect the GitHub 404 JSON payload. `inputOutputBinding()` — Kafka component `sample123`, service class `InputBindingService`: health-check the input binding, publish a `MyClass{message="hello"}` and a string `"cat"` via the output binding, then GET `/messages` and assert both arrive. Keep the GitHub URLs unchanged (spec D3). The two HTTP components are `bindings.http` (URL `https://api.github.com/unknown_path`; the `-success` one adds `errorIfNot2XX: "false"`); the Kafka component is `bindings.kafka` with `topics`/`publishTopic` = `topic-{appID}`, `consumerGroup` = `{appID}`, `authRequired=false`, `initialOffset=oldest`.

- [ ] **Step 1: Add the Kafka dependency** to `sdk-tests/pom.xml` (in `<dependencies>`, no `<version>` — managed by the root `testcontainers-bom`):
```xml
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers-kafka</artifactId>
      <scope>test</scope>
    </dependency>
```

- [ ] **Step 2: Add `kafka()` to `SharedTestInfra`** (mirror the `redis()`/`mongo()` lazy-singleton style; use `org.testcontainers.kafka.KafkaContainer` with an in-network listener so the daprd container can reach it):
```java
  private static final String KAFKA_NETWORK_ALIAS = "kafka";
  private static volatile org.testcontainers.kafka.KafkaContainer kafka;

  public static synchronized org.testcontainers.kafka.KafkaContainer kafka() {
    if (kafka == null) {
      kafka = new org.testcontainers.kafka.KafkaContainer("apache/kafka:3.8.0")
          .withNetwork(network())
          .withNetworkAliases(KAFKA_NETWORK_ALIAS)
          .withListener(KAFKA_NETWORK_ALIAS + ":19092")   // advertised on the shared network
          .withReuse(true);
      kafka.start();
    }
    return kafka;
  }

  /** Broker address reachable from other containers on the shared network. */
  public static String kafkaInternalBroker() {
    return KAFKA_NETWORK_ALIAS + ":19092";
  }
```
  > If the Testcontainers 2.0.5 `KafkaContainer` API for the in-network listener differs from `.withListener(String)`, consult the Testcontainers Kafka docs and adjust; the requirement is a broker advertised as `kafka:19092` on `SharedTestInfra.network()`. Do not guess silently — verify against the resolved API.

- [ ] **Step 3: Add component helpers to `BaseContainerIT`**:
```java
  protected static Component kafkaBinding(String name) {
    SharedTestInfra.kafka();
    return new Component(name, "bindings.kafka", "v1", Map.of(
        "brokers", SharedTestInfra.kafkaInternalBroker(),
        "topics", "topic-{appID}",
        "publishTopic", "topic-{appID}",
        "consumerGroup", "{appID}",
        "authRequired", "false",
        "initialOffset", "oldest"
    ));
  }

  protected static Component httpBinding(String name, String url, boolean errorIfNot2xx) {
    return new Component(name, "bindings.http", "v1", Map.of(
        "url", url,
        "errorIfNot2XX", Boolean.toString(errorIfNot2xx)
    ));
  }
```

- [ ] **Step 4: Rewrite `BindingIT`** to `extends BaseContainerIT`. For the two HTTP tests, start a `DaprContainer` (no app) via `daprBuilder(...)` with `.withComponent(httpBinding("github-http-binding-404", "https://api.github.com/unknown_path", true))` (and the `-success` variant with `false`); invoke via `newDaprClient(dapr)`. For `inputOutputBinding`, use `startAppAndAttach("bindingit-grpc", InputBindingService.class, HTTP, factory)` where the factory adds `.withComponent(kafkaBinding("sample123"))`; keep the health-check-then-publish-then-read flow and both assertions. Remove all `BaseIT`/`DaprRun`/`startDaprApp`/file-component usage.

- [ ] **Step 5: Verify** — run `-Dit.test=BindingIT`. Expected: PASS, all 3 tests run (the two HTTP tests require outbound network to api.github.com, as before).

- [ ] **Step 6: Commit**
```bash
git add sdk-tests/pom.xml \
        sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfra.java \
        sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java \
        sdk-tests/src/test/java/io/dapr/it/binding/http/BindingIT.java
git commit -s -m "test: migrate BindingIT to Testcontainers Kafka + in-code bindings"
```

---

## Task 8: Strip legacy setup from the CI `build` job

**Files:**
- Modify: `.github/workflows/build.yml` (the `build` job only)

- [ ] **Step 1: Edit `build.yml`** — in the `build` job (`name: "Build jdk:… sb:…"`), remove these steps and env: "Set up Dapr CLI"; the Go setup + `dapr/cli` & `dapr/dapr` checkout/override/placement steps (all `DAPR_REF`/`DAPR_CLI_REF` conditionals); "Uninstall Dapr runtime" + "Ensure Dapr runtime uninstalled" wait loop; "Initialize Dapr runtime"; "Spin local environment" (`docker compose … local-test.yml up -d kafka`); "Install local ToxiProxy"; and the now-unused env keys `GOVER`/`GOOS`/`GOARCH`/`GOPROXY`/`DAPR_CLI_VER`/`DAPR_RUNTIME_VER`/`DAPR_INSTALL_URL`/`DAPR_CLI_REF`/`DAPR_REF`/`TOXIPROXY_URL`. **Keep:** checkout, `docker version`, setup-java, `./mvnw clean install -DskipTests`, the sb 3.x / sb 4.x integration-test run steps, and all failsafe/surefire report-upload steps. Do NOT touch the `test`, `build-durabletask`, or `publish` jobs.

- [ ] **Step 2: Verify YAML** — `yamllint .github/workflows/build.yml` if available, else confirm the `build` job still has: checkout → setup-java → `mvnw clean install -DskipTests` → integration-test run → report uploads, and references no removed env var (grep for `DAPR_CLI_VER`, `TOXIPROXY_URL`, `local-test.yml`, `dapr init`, `dapr uninstall` → expect no matches in `build.yml`).

- [ ] **Step 3: Commit**
```bash
git add .github/workflows/build.yml
git commit -s -m "ci: drop Dapr CLI/init/Kafka/ToxiProxy setup from build job"
```

---

## Task 9: Delete dead legacy harness + full-module verify

**Files:**
- Delete: `sdk-tests/src/test/java/io/dapr/it/BaseIT.java`, `DaprRun.java`, `DaprRunConfig.java`, `ToxiProxyRun.java`
- Delete: `sdk-tests/components/kafka_bindings.yaml`, `sdk-tests/components/http_binding.yaml`, `sdk-tests/deploy/local-test.yml`
- **Keep:** `AppRun.java`, `DaprPorts.java`, `Command.java`, `Stoppable.java`, `SharedTestInfra.java`, `BaseContainerIT.java`

- [ ] **Step 1: Confirm zero references** before deleting each file:
```bash
cd /Users/svegiraju/Git/java-sdk
for c in BaseIT DaprRun DaprRunConfig ToxiProxyRun; do
  echo "== $c =="; grep -rn "\b$c\b" sdk-tests/src --include=*.java | grep -v "/$c.java:" || echo "  (no refs)"
done
```
  Expected: `(no refs)` for all four. If any reference remains, that test was not fully migrated — fix it before deleting. Also grep for remaining consumers of the two YAMLs / `local-test.yml`:
```bash
grep -rn "kafka_bindings\|http_binding\|local-test.yml" sdk-tests .github || echo "(no refs)"
```

- [ ] **Step 2: Delete the files** (only after Step 1 is clean):
```bash
git rm sdk-tests/src/test/java/io/dapr/it/BaseIT.java \
       sdk-tests/src/test/java/io/dapr/it/DaprRun.java \
       sdk-tests/src/test/java/io/dapr/it/DaprRunConfig.java \
       sdk-tests/src/test/java/io/dapr/it/ToxiProxyRun.java \
       sdk-tests/components/kafka_bindings.yaml \
       sdk-tests/components/http_binding.yaml \
       sdk-tests/deploy/local-test.yml
```

- [ ] **Step 3: Full-module verify** — build the whole SDK then run every `sdk-tests` IT with the trimmed setup (requires Docker):
```bash
cd /Users/svegiraju/Git/java-sdk
./mvnw clean install -B -q -DskipTests && \
./mvnw -B -pl sdk-tests -Pintegration-tests dependency:copy-dependencies verify
```
  Expected: `BUILD SUCCESS`; all 20 sdk-tests ITs run (the one `ActorSdkResiliencyIT` test skipped); no compilation error from the deletions. If Docker is unavailable locally, state that this must be validated in CI and push the branch.

- [ ] **Step 4: Commit**
```bash
git add -A
git commit -s -m "test: remove dead dapr-run harness, binding YAMLs, and local-test.yml"
```

---

## Done criteria

- All 7 ITs extend `BaseContainerIT`; no `sdk-tests` file references `BaseIT`/`DaprRun`/`DaprRunConfig`/`ToxiProxyRun`.
- `build.yml`'s `build` job installs no Dapr CLI and runs no `dapr init`/`dapr uninstall`/host Kafka/host ToxiProxy.
- Full `sdk-tests` `verify` is green in CI; the `build sb:3.5.x` job completes materially faster than 26 min (target ≈ the sb:4.0.x job) and well under the 45-min timeout — satisfying issue #1522's `<20 min` acceptance criterion.
