# sdk-tests Integration Test Migration to Testcontainers — Design

**Date:** 2026-05-25
**Status:** Approved (pending spec review)
**Author:** Siri Varma Vegiraju (with Claude)
**Scope:** [sdk-tests/](../../../sdk-tests/) module

## Problem

Today, 21 integration tests under [sdk-tests/src/test/java/io/dapr/it/](../../../sdk-tests/src/test/java/io/dapr/it/) run by shelling out to the locally installed Dapr CLI (`dapr init` + `dapr run`) via the `BaseIT` / `DaprRun` / `AppRun` infrastructure. This:

- Requires every developer and CI runner to install the Dapr CLI and run `dapr init` before tests can pass.
- Couples test runs to whatever Dapr runtime version is installed on the host.
- Makes hermetic, parallel test execution difficult.
- Diverges from the newer [spring-boot-4-sdk-tests/](../../../spring-boot-4-sdk-tests/) module, which already uses Testcontainers via the [testcontainers-dapr/](../../../testcontainers-dapr/) library.

This spec covers migrating **12 of those 21 ITs** to Testcontainers (13 files — TracingIT has separate grpc/http variants). The remaining 9 ITs either test sidecar lifecycle behavior (failover, recovery, slow startup, actor state across sidecar restart) that Testcontainers' opaque lifecycle makes awkward, or use complex external topologies (Kafka bindings, ToxiProxy-mediated resiliency) that are easier to leave on `DaprRun`.

## Goals

- Migrate 12 ITs (13 files) to use [`DaprContainer`](../../../testcontainers-dapr/src/main/java/io/dapr/testcontainers/DaprContainer.java) instead of `DaprRun`.
- Replace `BaseIT` extension with a new `BaseContainerIT` extension for migrated tests.
- Containerize all backing services (Redis, Zipkin) used by migrated ITs.
- Keep `BaseIT` / `DaprRun` / `AppRun` / `DaprPorts` infrastructure untouched for the 9 non-migrated ITs.
- Update CI ([`.github/workflows/build.yml`](../../../.github/workflows/build.yml)) to remove the no-longer-needed MongoDB step.
- Land everything in a single PR.

## Non-Goals

- Migrating these 9 ITs (out of scope; will stay on `DaprRun`):
  - [BindingIT.java](../../../sdk-tests/src/test/java/io/dapr/it/binding/http/BindingIT.java) — Kafka bindings topology
  - [ActorReminderFailoverIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorReminderFailoverIT.java) — sidecar restart mid-test
  - [ActorReminderRecoveryIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorReminderRecoveryIT.java) — sidecar restart mid-test
  - [ActorTimerRecoveryIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorTimerRecoveryIT.java) — sidecar restart mid-test
  - [ActorStateIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorStateIT.java) — explicitly stops one sidecar and starts a second to verify actor state survives the restart ([line 130-138](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorStateIT.java#L130-L138))
  - [WaitForSidecarIT.java](../../../sdk-tests/src/test/java/io/dapr/it/resiliency/WaitForSidecarIT.java) — client starts before sidecar
  - [ActorSdkResiliencyIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorSdkResiliencyIT.java) — ToxiProxy between client and sidecar
  - The two [durabletask-client/](../../../durabletask-client/) ITs ([DurableTaskClientIT.java](../../../durabletask-client/src/test/java/io/dapr/durabletask/DurableTaskClientIT.java), [ErrorHandlingIT.java](../../../durabletask-client/src/test/java/io/dapr/durabletask/ErrorHandlingIT.java)) — separate module, separate effort.
- Replacing `AppRun`. We keep the `mvn exec:java` subprocess pattern for the app side; only the Dapr sidecar is containerized.
- Introducing MongoDB as a Testcontainer. The one Mongo-dependent test (`AbstractStateClientIT#saveAndQueryAndDeleteState`) gets `@Disabled` with a comment.
- Migrating the `dapr/cli` install, `dapr init`, Kafka, or ToxiProxy steps out of CI — they remain for the 9 non-migrated ITs.

## Decisions

| # | Decision | Rationale |
|---|---|---|
| D1 | Replace ITs in-place (not parallel suite) | Avoid running both old and new versions of the same logic; cleaner end-state. |
| D2 | App stays in `mvn exec:java` subprocess via `AppRun` (Option A) | Lower-risk than rewriting the app harness; goal is removing `dapr run`, not `AppRun`. |
| D3 | Containerize all backing services (Redis, Zipkin) via Testcontainers | Removes the host-local-Redis assumption; matches `DaprContainer`'s self-contained model. |
| D4 | Single `BaseContainerIT` shared base class providing only helpers and cleanup | Consistent surface area across 12 ITs; mirrors the role `BaseIT` plays today. |
| D5 | Single PR for all 12 ITs + CI change | One cutover; matches user preference. |
| D6 | Update [`.github/workflows/build.yml`](../../../.github/workflows/build.yml) in the same PR | Migration isn't useful unless CI exercises it; trim Mongo from compose-up step. |
| D7 | Shared deps (Redis/Zipkin) via Testcontainers `withReuse(true)` + JVM singleton; per-class Dapr sidecar | Component config differs per test, so Dapr can't be shared. Deps are stateless enough to share. |
| D8 | Keep `BaseIT` + `DaprRun` + `AppRun` + `DaprPorts` for the 9 non-migrated ITs | Smallest blast radius; no rename churn. |
| D9 | Per-class `@BeforeAll` lifecycle for all migrated ITs (semantic change from today's per-`@Test` pattern in 8 ITs: ApiIT, ActivationDeactivationIT, ActorTurnBasedConcurrencyIT, ActorMethodNameIT, MethodInvokeIT × 2, TracingIT × 2) | Per-method DaprContainer startup adds 3–5s × ~50 test methods = ~3–4 min CI regression. Audit per @Test confirms tests use unique keys/actor IDs and don't depend on fresh sidecar state. TracingIT mitigation: each @Test asserts on a unique trace ID rather than total span count. |
| D10 | Each migrated IT subclass owns its own `private static DaprContainer dapr` (and `AppRun app` where needed); base class does NOT hold these as `protected static` | Avoids state bleed when Surefire forks share a JVM across IT classes; explicit ownership per IT. |

## Architecture

Two new pieces of test infrastructure live under [sdk-tests/src/test/java/io/dapr/it/](../../../sdk-tests/src/test/java/io/dapr/it/):

### `SharedTestInfra`

JVM-singleton holder for backing services that aren't Dapr.

- `RedisContainer` — `redis:7-alpine`, `withReuse(true)`, joined to shared `Network`.
- `ZipkinContainer` — `openzipkin/zipkin`, `withReuse(true)`, joined to shared `Network` (only used by `TracingIT`).
- Shared `Network network = Network.newNetwork()` — cached as a static so `DaprContainer` can join via `withNetwork(network)` and resolve `redis:6379` / `zipkin:9411` internally.
- Lazy startup: each accessor (`SharedTestInfra.redis()`, `SharedTestInfra.zipkin()`) starts its container on first access. Tests that don't need Zipkin never start Zipkin.
- `withReuse(true)` means local dev sessions skip startup on subsequent runs. CI gets fresh containers per job (reuse is per-host).

### `BaseContainerIT`

Abstract base class extended by all 12 migrated ITs. Per **D10**, the base class holds **no** `DaprContainer` or `AppRun` fields — each subclass owns its own statics. The base class provides only helpers and `@AfterAll` cleanup.

```java
public abstract class BaseContainerIT {

  /** Pre-configured DaprContainer.Builder: shared network, log streaming,
   *  appChannelAddress=host.testcontainers.internal, image pinned via constant. */
  protected static DaprContainer.Builder daprBuilder(String appName);

  /** Spawns the service class via AppRun (mvn exec:java), exposes its port to
   *  Testcontainers, returns the running AppRun. MUST be called BEFORE starting
   *  the DaprContainer that needs to call back into it. Caller owns the returned
   *  AppRun (typically stored in a private static field). Also registers the
   *  AppRun for @AfterAll cleanup via deferStop(). */
  protected static AppRun startApp(String appName, Class<?> serviceClass,
                                   AppRun.AppProtocol protocol) throws Exception;

  /** DaprClient factories bound to the supplied DaprContainer. */
  protected static DaprClient newDaprClient(DaprContainer dapr);
  protected static DaprClientBuilder newDaprClientBuilder(DaprContainer dapr);
  protected static ActorClient newActorClient(DaprContainer dapr);
  protected static ActorClient newActorClient(DaprContainer dapr, ResiliencyOptions opts);

  /** Internal-network hostnames for use in DaprContainer Component metadata. */
  protected static String redisInternalHost();        // "redis:6379"
  protected static String zipkinInternalEndpoint();   // "http://zipkin:9411/api/v2/spans"

  /** Pre-built Components referencing shared deps. */
  protected static Component redisStateStore(String name);   // actorStateStore=true
  protected static Component redisPubSub(String name);
  protected static Component redisConfigStore(String name);

  /** Register a resource for @AfterAll cleanup. */
  protected static <T extends AutoCloseable> T deferClose(T object);
  protected static void deferStop(Stoppable stoppable);   // for AppRun, DaprContainer

  @AfterAll
  static void cleanUp();   // drains deferStop queue then deferClose queue
}
```

**Typical subclass shape (client-only IT — SecretsClientIT):**

```java
public class SecretsClientIT extends BaseContainerIT {
  private static DaprContainer dapr;

  @BeforeAll
  static void init() {
    dapr = daprBuilder("secrets-it")
        .withComponent(new Component("localSecretStore", "secretstores.local.file", "v1",
            Map.of("secretsFile", "/components/secret.json")))
        .withCopyFileToContainer(MountableFile.forClasspathResource("secret.json"),
            "/components/secret.json")
        .build();
    dapr.start();
    deferStop(dapr);
  }

  @Test
  void getSecret() {
    try (DaprClient c = newDaprClient(dapr)) { /* ... */ }
  }
}
```

**Typical subclass shape (actor IT — needs callback):**

```java
public class ActorMethodNameIT extends BaseContainerIT {
  private static DaprContainer dapr;
  private static AppRun app;

  @BeforeAll
  static void init() throws Exception {
    app = startApp("actor-method-name-it", ActorService.class, HTTP);  // also exposes host port + deferStop
    dapr = daprBuilder("actor-method-name-it")
        .withAppPort(app.getAppPort())
        .withAppChannelAddress("host.testcontainers.internal")
        .withComponent(redisStateStore("statestore"))
        .build();
    dapr.start();
    deferStop(dapr);
  }
}
```

### Coexistence

[`BaseIT.java`](../../../sdk-tests/src/test/java/io/dapr/it/BaseIT.java), [`DaprRun.java`](../../../sdk-tests/src/test/java/io/dapr/it/DaprRun.java), [`AppRun.java`](../../../sdk-tests/src/test/java/io/dapr/it/AppRun.java), [`DaprPorts.java`](../../../sdk-tests/src/test/java/io/dapr/it/DaprPorts.java), and [`DaprRunConfig.java`](../../../sdk-tests/src/test/java/io/dapr/it/DaprRunConfig.java) stay untouched. The 9 non-migrated ITs continue to extend `BaseIT`.

`AppRun` is consumed by **both** `BaseIT` (today) and `BaseContainerIT` (new). Its public API stays the same with one addition: a new constructor overload (or builder variant) accepting explicit `daprHttpPort` / `daprGrpcPort` overrides, so `BaseContainerIT.startApp()` can point the app subprocess at the `DaprContainer`'s mapped ports rather than at `DaprPorts`-allocated host ports. Existing callers from `BaseIT` are unaffected.

## Startup ordering & Dapr→app callback

The Dapr sidecar, running in a container, can only reach the host JVM via `host.testcontainers.internal:<port>`. `Testcontainers.exposeHostPorts(port)` must be called **before** any container that needs to reach back is started.

Per-IT-class lifecycle (subclass owns the `dapr` and `app` static fields per **D10**):

```
@BeforeAll (in subclass):
  1. SharedTestInfra.redis().start()       // idempotent
  2. (if app needed) app = startApp(appName, ServiceClass.class, HTTP)
       - AppRun spawns mvn exec:java with chosen free port
       - BaseContainerIT.startApp() calls Testcontainers.exposeHostPorts(port)
       - BaseContainerIT.startApp() registers the AppRun via deferStop()
  3. dapr = daprBuilder(appName)
       .withAppPort(app.getAppPort())                              // skip if no app
       .withAppChannelAddress("host.testcontainers.internal")      // skip if no app
       .withComponent(redisStateStore("statestore"))
       .build();
  4. dapr.start();          // DaprContainer waits for sidecar healthy
  5. deferStop(dapr);

@AfterAll (inherited from BaseContainerIT):
  - drains deferStop queue (LIFO): stops dapr, then app
  - drains deferClose queue
  - SharedTestInfra containers are NOT stopped (JVM shutdown hook via reuse=true)
```

**Client-only ITs** (Secrets, Config, State, Api) skip steps 2 and the `withAppPort` / `withAppChannelAddress` calls.

**MethodInvokeIT (#12/#13)**: spawns one app via `startApp` (the invoked method's host); the test JVM acts as the caller. The grpc and http variants differ only in `AppRun.AppProtocol`.

**TracingIT (#14/#15)**: uses `daprBuilder(...).withConfiguration(new Configuration(...).withZipkinTracingConfigurationSettings(new ZipkinTracingConfigurationSettings(SharedTestInfra.zipkinInternalEndpoint())))`. Test assertions hit Zipkin's REST API on its mapped port to verify spans landed.

## Per-IT Migration Matrix

All migrated ITs use per-class `@BeforeAll` lifecycle per **D9**. The "Today's lifecycle" column is informational — where it says per-`@Test` or in-method, migration changes that to per-class and the implementer must verify tests are state-independent (use unique keys/actor IDs).

| # | IT | Components | App? | Today's lifecycle | Migration notes |
|---|---|---|---|---|---|
| 1 | [SecretsClientIT](../../../sdk-tests/src/test/java/io/dapr/it/secrets/SecretsClientIT.java) | `secretstores.local.file` (mount `secret.json`) | No | `@BeforeAll` | Drop `BaseIT.startDaprApp`; use `MountableFile.forClasspathResource("secret.json")`. |
| 2 | [ConfigurationClientIT](../../../sdk-tests/src/test/java/io/dapr/it/configuration/ConfigurationClientIT.java) | `configuration.redis` → shared Redis | No | `@BeforeAll` | Replace `redis-cli` seeding with Jedis pointed at `SharedTestInfra.redis().getMappedPort(6379)`. |
| 3 | [AbstractStateClientIT](../../../sdk-tests/src/test/java/io/dapr/it/state/AbstractStateClientIT.java) | `state.redis` (actorStateStore=true) | No | n/a (abstract) | `@Disabled` on `saveAndQueryAndDeleteState` (only Mongo-dependent test). |
| 4 | [GRPCStateClientIT](../../../sdk-tests/src/test/java/io/dapr/it/state/GRPCStateClientIT.java) | inherits #3 | No | `@BeforeAll` | Just extends `BaseContainerIT` instead of `BaseIT`. |
| 5 | [ApiIT](../../../sdk-tests/src/test/java/io/dapr/it/api/ApiIT.java) | none | No | in-method `startDaprApp` | Refactor to `@BeforeAll`; use `newDaprClient(dapr)`. |
| 6 | [ActivationDeactivationIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActivationDeactivationIT.java) | `state.redis` (actorStateStore=true) | Yes (`StatefulActorService`) | in-method `startDaprApp` | Refactor to `@BeforeAll`; verify actor IDs are unique across tests. |
| 7 | [ActorTurnBasedConcurrencyIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorTurnBasedConcurrencyIT.java) | same as #6 | Yes | in-method `startDaprApp` | Refactor to `@BeforeAll`; verify actor IDs are unique. |
| 8 | [ActorExceptionIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorExceptionIT.java) | same | Yes | `@BeforeAll` | Same pattern as #6 but already class-scoped. |
| 9 | [ActorMethodNameIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorMethodNameIT.java) | same | Yes | in-method `startDaprApp` | Refactor to `@BeforeAll`. |
| 10 | [MethodInvokeIT (grpc)](../../../sdk-tests/src/test/java/io/dapr/it/methodinvoke/grpc/MethodInvokeIT.java) | none | Yes (single app: invoked-method host; test JVM is caller) | `@BeforeEach` | Refactor to `@BeforeAll`; tests already namespace by request payload, but verify. |
| 11 | [MethodInvokeIT (http)](../../../sdk-tests/src/test/java/io/dapr/it/methodinvoke/http/MethodInvokeIT.java) | none | Yes (single app) | `@BeforeEach` | Same as #10 with HTTP protocol. |
| 12 | [TracingIT (grpc)](../../../sdk-tests/src/test/java/io/dapr/it/tracing/grpc/TracingIT.java) | tracing `Configuration` → shared Zipkin | Yes | `@BeforeEach` | Refactor to `@BeforeAll`; **change assertion strategy** from "spans this test produced" to "query Zipkin by per-test unique trace ID". |
| 13 | [TracingIT (http)](../../../sdk-tests/src/test/java/io/dapr/it/tracing/http/TracingIT.java) | same as #12 | Yes | `@BeforeEach` | Same as #12 with HTTP. |

That's **13 files / 12 logical ITs** (TracingIT and MethodInvokeIT each have grpc + http variants in separate files; AbstractStateClientIT is an abstract parent of GRPCStateClientIT). The total IT count in `sdk-tests/src/test/java/io/dapr/it/` before this work is **22 files** (9 non-migrated + 13 migrated).

### Removed from migrated ITs

- All references to `BaseIT.startDaprApp(...)`.
- Imports of `DaprRun`, `DaprPorts`, `DaprRunConfig`.
- File-based component lookups from [sdk-tests/components/](../../../sdk-tests/components/) — components are now defined in-code via the `Component` model from [testcontainers-dapr](../../../testcontainers-dapr/).

### Preserved YAMLs

[sdk-tests/components/](../../../sdk-tests/components/) and [sdk-tests/configurations/](../../../sdk-tests/configurations/) stay on disk because the 9 non-migrated ITs still load them via `dapr run --components-path`.

## CI changes ([.github/workflows/build.yml](../../../.github/workflows/build.yml))

| Step (line) | Disposition |
|---|---|
| Checkout/build dapr CLI (optional, conditional) | **Keep** — 9 ITs still use `dapr run`. |
| `dapr uninstall --all` (164) | **Keep** — needed for legacy ITs. |
| `dapr init --runtime-version $DAPR_RUNTIME_VER` (173) | **Keep** — needed for legacy ITs. |
| Override `daprd` / placement (optional) | **Keep**. |
| `docker compose -f ./sdk-tests/deploy/local-test.yml up -d mongo kafka` (190) | **Trim**: change to `up -d kafka`. |
| Install ToxiProxy (192–197) | **Keep** — `ActorSdkResiliencyIT` still on `BaseIT`. |
| `./mvnw clean install -DskipTests` (199) | **Unchanged**. |
| Failsafe runs and report uploads (208–231) | **Unchanged** — IT discovery surface is identical. |

Also remove the `mongo` service from [sdk-tests/deploy/local-test.yml](../../../sdk-tests/deploy/local-test.yml).

Docker is already available on `ubuntu-latest` GitHub runners; Testcontainers auto-discovers via `DOCKER_HOST`. No additional CI setup is required.

## Risks & mitigations

| Risk | Mitigation |
|---|---|
| `host.testcontainers.internal` resolution differs on Linux vs. Docker Desktop vs. Colima | Testcontainers handles this transparently when `exposeHostPorts` is called; CI is Linux only, dev varies. Doc the requirement in spec + sdk-tests README. |
| Switching 8 ITs from per-`@Test` to per-class lifecycle (**D9**) could surface state-bleed bugs | Per-IT audit during implementation: confirm tests use unique UUIDs/actor IDs for state isolation; for TracingIT, change assertion strategy to query Zipkin by per-test trace ID (instead of asserting total span count). If an IT cannot be made state-independent, fall back to per-method DaprContainer for just that IT. |
| `AppRun` subprocess + DaprContainer combined startup is slower per IT than `dapr run` is today | Acceptable: Redis is shared via reuse, image pulls are cached. If wall-clock regresses badly we can revisit `EmbeddedAppServer` (Option B from brainstorming). |
| `withReuse(true)` requires `~/.testcontainers.properties` opt-in for dev parity with CI | Document in sdk-tests README; CI runs with reuse disabled implicitly (per-job hosts). |
| `AppRun` env-var port overrides change touch a shared file | Pure addition (new constructor overload); existing callers untouched. |
| 12 new IT classes pulling DaprContainer on CI could lengthen cold runs by 30-60s | Acceptable trade for removing host Dapr CLI dependency. |

## Testing

- Each migrated IT class runs locally via `cd sdk-tests && ../mvnw verify -Dit.test=<ClassName>`.
- Full sdk-tests `verify` must pass locally and on CI.
- The 9 non-migrated ITs must continue to pass unchanged.
- New `BaseContainerIT` and `SharedTestInfra` are exercised exclusively by the migrated ITs; no additional unit tests for them.

## Open questions

None at spec-approval time. Implementation plan will resolve concrete `DaprContainer` image tag (default to whatever `spring-boot-4-sdk-tests` already uses), Redis image tag, Zipkin image tag, and `host.testcontainers.internal` wait strategy.

## Out of scope (future work)

- Migrating the 9 non-migrated ITs (especially the actor lifecycle group: `ActorStateIT`, `ActorReminderFailoverIT`, `ActorReminderRecoveryIT`, `ActorTimerRecoveryIT`, `WaitForSidecarIT`) once `DaprContainer` exposes friendlier sidecar restart APIs.
- Migrating the two [durabletask-client/](../../../durabletask-client/) ITs.
- Replacing `AppRun` with an in-JVM `EmbeddedAppServer` to remove subprocess overhead.
