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

This spec covers migrating **13 of those 21 ITs** to Testcontainers. The remaining 8 ITs either test sidecar lifecycle behavior (failover, recovery, slow startup) that Testcontainers' opaque lifecycle makes awkward, or use complex external topologies (Kafka bindings, ToxiProxy-mediated resiliency) that are easier to leave on `DaprRun`.

## Goals

- Migrate 13 ITs to use [`DaprContainer`](../../../testcontainers-dapr/src/main/java/io/dapr/testcontainers/DaprContainer.java) instead of `DaprRun`.
- Replace `BaseIT` extension with a new `BaseContainerIT` extension for migrated tests.
- Containerize all backing services (Redis, Zipkin) used by migrated ITs.
- Keep `BaseIT` / `DaprRun` / `AppRun` / `DaprPorts` infrastructure untouched for the 8 non-migrated ITs.
- Update CI ([`.github/workflows/build.yml`](../../../.github/workflows/build.yml)) to remove the no-longer-needed MongoDB step.
- Land everything in a single PR.

## Non-Goals

- Migrating these 8 ITs (out of scope; will stay on `DaprRun`):
  - [BindingIT.java](../../../sdk-tests/src/test/java/io/dapr/it/binding/http/BindingIT.java) — Kafka bindings topology
  - [ActorReminderFailoverIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorReminderFailoverIT.java) — sidecar restart mid-test
  - [ActorReminderRecoveryIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorReminderRecoveryIT.java) — sidecar restart mid-test
  - [ActorTimerRecoveryIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorTimerRecoveryIT.java) — sidecar restart mid-test
  - [WaitForSidecarIT.java](../../../sdk-tests/src/test/java/io/dapr/it/resiliency/WaitForSidecarIT.java) — client starts before sidecar
  - [ActorSdkResiliencyIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorSdkResiliencyIT.java) — ToxiProxy between client and sidecar
  - The two [durabletask-client/](../../../durabletask-client/) ITs ([DurableTaskClientIT.java](../../../durabletask-client/src/test/java/io/dapr/durabletask/DurableTaskClientIT.java), [ErrorHandlingIT.java](../../../durabletask-client/src/test/java/io/dapr/durabletask/ErrorHandlingIT.java)) — separate module, separate effort.
- Replacing `AppRun`. We keep the `mvn exec:java` subprocess pattern for the app side; only the Dapr sidecar is containerized.
- Introducing MongoDB as a Testcontainer. The one Mongo-dependent test (`AbstractStateClientIT#saveAndQueryAndDeleteState`) gets `@Disabled` with a comment.
- Migrating the `dapr/cli` install, `dapr init`, Kafka, or ToxiProxy steps out of CI — they remain for the 8 non-migrated ITs.

## Decisions

| # | Decision | Rationale |
|---|---|---|
| D1 | Replace ITs in-place (not parallel suite) | Avoid running both old and new versions of the same logic; cleaner end-state. |
| D2 | App stays in `mvn exec:java` subprocess via `AppRun` (Option A) | Lower-risk than rewriting the app harness; goal is removing `dapr run`, not `AppRun`. |
| D3 | Containerize all backing services (Redis, Zipkin) via Testcontainers | Removes the host-local-Redis assumption; matches `DaprContainer`'s self-contained model. |
| D4 | Single `BaseContainerIT` shared base class | Consistent surface area across 13 ITs; mirrors the role `BaseIT` plays today. |
| D5 | Single PR for all 13 ITs + CI change | One cutover; matches user preference. |
| D6 | Update [`.github/workflows/build.yml`](../../../.github/workflows/build.yml) in the same PR | Migration isn't useful unless CI exercises it; trim Mongo from compose-up step. |
| D7 | Shared deps (Redis/Zipkin) via Testcontainers `withReuse(true)` + JVM singleton; per-class Dapr sidecar | Component config differs per test, so Dapr can't be shared. Deps are stateless enough to share. |
| D8 | Keep `BaseIT` + `DaprRun` + `AppRun` + `DaprPorts` for the 8 non-migrated ITs | Smallest blast radius; no rename churn. |

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

Abstract base class extended by all 13 migrated ITs. Public API:

```java
public abstract class BaseContainerIT {

  protected static DaprContainer dapr;   // populated by subclass in @BeforeAll
  protected static AppRun app;           // optional, only for ITs needing callback

  /** Pre-configured DaprContainer.Builder: shared network, log streaming,
   *  appChannelAddress=host.testcontainers.internal, image pinned via constant. */
  protected static DaprContainer.Builder daprBuilder(String appName);

  /** Spawns the service class via AppRun (mvn exec:java), exposes its port to
   *  Testcontainers, returns the running AppRun. MUST be called BEFORE dapr.start(). */
  protected static AppRun startApp(String appName, Class<?> serviceClass,
                                   AppRun.AppProtocol protocol) throws Exception;

  protected static DaprClient newDaprClient();
  protected static DaprClientBuilder newDaprClientBuilder();
  protected static ActorClient newActorClient();
  protected static ActorClient newActorClient(ResiliencyOptions opts);

  /** Internal-network hostnames for use in DaprContainer Component metadata. */
  protected static String redisInternalHost();        // "redis:6379"
  protected static String zipkinInternalEndpoint();   // "http://zipkin:9411/api/v2/spans"

  /** Pre-built Components referencing shared deps. */
  protected static Component redisStateStore(String name);   // actorStateStore=true
  protected static Component redisPubSub(String name);
  protected static Component redisConfigStore(String name);

  protected static <T extends AutoCloseable> T deferClose(T object);

  @AfterAll
  static void cleanUp();   // drains deferred closes, stops app, stops dapr
}
```

### Coexistence

[`BaseIT.java`](../../../sdk-tests/src/test/java/io/dapr/it/BaseIT.java), [`DaprRun.java`](../../../sdk-tests/src/test/java/io/dapr/it/DaprRun.java), [`AppRun.java`](../../../sdk-tests/src/test/java/io/dapr/it/AppRun.java), [`DaprPorts.java`](../../../sdk-tests/src/test/java/io/dapr/it/DaprPorts.java), and [`DaprRunConfig.java`](../../../sdk-tests/src/test/java/io/dapr/it/DaprRunConfig.java) stay untouched. The 8 non-migrated ITs continue to extend `BaseIT`.

`AppRun` is consumed by **both** `BaseIT` (today) and `BaseContainerIT` (new). Its public API does not change. The only behavioral concern: when invoked from `BaseContainerIT`, the `DAPR_HTTP_PORT` / `DAPR_GRPC_PORT` env vars must point at the `DaprContainer`'s mapped ports rather than `DaprPorts`-allocated host ports. This is handled by an overload (or a builder variant) of `AppRun` that accepts explicit Dapr port overrides; `BaseContainerIT.startApp()` is the only caller of that overload.

## Startup ordering & Dapr→app callback

The Dapr sidecar, running in a container, can only reach the host JVM via `host.testcontainers.internal:<port>`. `Testcontainers.exposeHostPorts(port)` must be called **before** any container that needs to reach back is started.

Per-IT-class lifecycle:

```
@BeforeAll:
  1. SharedTestInfra.redis().start()       // idempotent
  2. (if app needed) app = startApp(appName, ServiceClass.class, HTTP)
       - AppRun spawns mvn exec:java with chosen free port
       - BaseContainerIT.startApp() calls Testcontainers.exposeHostPorts(port)
  3. dapr = daprBuilder(appName)
       .withAppPort(app.getAppPort())                              // skip if no app
       .withAppChannelAddress("host.testcontainers.internal")      // skip if no app
       .withComponent(redisStateStore("statestore"))
       .withNetwork(SharedTestInfra.network())
       .dependsOn(SharedTestInfra.redis())
       .build();
  4. dapr.start();          // DaprContainer waits for sidecar healthy

@AfterAll:
  - dapr.stop()
  - app.stop()              // if started
  - deferClose() drains
  - SharedTestInfra containers are NOT stopped (JVM shutdown hook via reuse=true)
```

**Client-only ITs** (Secrets, Config, State, Api) skip steps 2 and the `withAppPort` / `withAppChannelAddress` calls.

**MethodInvokeIT (#12/#13)**: spawns one app via `startApp` (the invoked method's host); the test JVM acts as the caller. The grpc and http variants differ only in `AppRun.AppProtocol`.

**TracingIT (#14/#15)**: uses `daprBuilder(...).withConfiguration(new Configuration(...).withZipkinTracingConfigurationSettings(new ZipkinTracingConfigurationSettings(SharedTestInfra.zipkinInternalEndpoint())))`. Test assertions hit Zipkin's REST API on its mapped port to verify spans landed.

## Per-IT Migration Matrix

| # | IT | Components | App? | Notes |
|---|---|---|---|---|
| 1 | [SecretsClientIT](../../../sdk-tests/src/test/java/io/dapr/it/secrets/SecretsClientIT.java) | `secretstores.local.file` (mount `secret.json`) | No | Drop `BaseIT.startDaprApp`; use `MountableFile.forClasspathResource("secret.json")`. |
| 2 | [ConfigurationClientIT](../../../sdk-tests/src/test/java/io/dapr/it/configuration/ConfigurationClientIT.java) | `configuration.redis` → shared Redis | No | Replace `redis-cli` seeding with Jedis pointed at `SharedTestInfra.redis().getMappedPort(6379)`. |
| 3 | [AbstractStateClientIT](../../../sdk-tests/src/test/java/io/dapr/it/state/AbstractStateClientIT.java) | `state.redis` (actorStateStore=true) | No | `@Disabled` on `saveAndQueryAndDeleteState` (only Mongo-dependent test). |
| 4 | [GRPCStateClientIT](../../../sdk-tests/src/test/java/io/dapr/it/state/GRPCStateClientIT.java) | inherits #3 | No | Just extends `BaseContainerIT` instead of `BaseIT`. |
| 5 | [ApiIT](../../../sdk-tests/src/test/java/io/dapr/it/api/ApiIT.java) | none | No | Trivial: use `newDaprClient()`. |
| 6 | [ActorStateIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorStateIT.java) | `state.redis` (actorStateStore=true) | Yes (`ActorService`) | `startApp()` + `withAppPort`; placement is built into `DaprContainer`. |
| 7 | [ActivationDeactivationIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActivationDeactivationIT.java) | same as #6 | Yes | Same pattern. |
| 8 | [ActorTurnBasedConcurrencyIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorTurnBasedConcurrencyIT.java) | same | Yes | Same pattern. |
| 9 | [ActorExceptionIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorExceptionIT.java) | same | Yes | Same pattern. |
| 10 | [ActorMethodNameIT](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorMethodNameIT.java) | same | Yes | Same pattern. |
| 11 | [MethodInvokeIT (grpc)](../../../sdk-tests/src/test/java/io/dapr/it/methodinvoke/grpc/MethodInvokeIT.java) | none | Yes | Single app (invoked method host). Test JVM is caller. |
| 12 | [MethodInvokeIT (http)](../../../sdk-tests/src/test/java/io/dapr/it/methodinvoke/http/MethodInvokeIT.java) | none | Yes | Same as #11 with HTTP. |
| 13 | [TracingIT (grpc)](../../../sdk-tests/src/test/java/io/dapr/it/tracing/grpc/TracingIT.java) | tracing `Configuration` → shared Zipkin | Yes | Verify spans via Zipkin REST on mapped port. |
| 14 | [TracingIT (http)](../../../sdk-tests/src/test/java/io/dapr/it/tracing/http/TracingIT.java) | same as #13 | Yes | Same as #13 with HTTP. |

(That's 14 rows because TracingIT has two protocol variants. Migration count = **13 ITs** if you count TracingIT as one logical IT; 14 if you count each file.)

### Removed from migrated ITs

- All references to `BaseIT.startDaprApp(...)`.
- Imports of `DaprRun`, `DaprPorts`, `DaprRunConfig`.
- File-based component lookups from [sdk-tests/components/](../../../sdk-tests/components/) — components are now defined in-code via the `Component` model from [testcontainers-dapr](../../../testcontainers-dapr/).

### Preserved YAMLs

[sdk-tests/components/](../../../sdk-tests/components/) and [sdk-tests/configurations/](../../../sdk-tests/configurations/) stay on disk because the 8 non-migrated ITs still load them via `dapr run --components-path`.

## CI changes ([.github/workflows/build.yml](../../../.github/workflows/build.yml))

| Step (line) | Disposition |
|---|---|
| Checkout/build dapr CLI (optional, conditional) | **Keep** — 8 ITs still use `dapr run`. |
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
| `AppRun` subprocess + DaprContainer combined startup is slower per IT than `dapr run` is today | Acceptable: Redis is shared via reuse, image pulls are cached. If wall-clock regresses badly we can revisit `EmbeddedAppServer` (Option B from brainstorming). |
| `withReuse(true)` requires `~/.testcontainers.properties` opt-in for dev parity with CI | Document in sdk-tests README; CI runs with reuse disabled implicitly (per-job hosts). |
| `AppRun` env-var port overrides change touch a shared file | Pure addition (new constructor overload); existing callers untouched. |
| 13 new IT classes building/pulling DaprContainer on CI could lengthen cold runs by 30-60s | Acceptable trade for removing host Dapr CLI dependency. |

## Testing

- Each migrated IT class runs locally via `cd sdk-tests && ../mvnw verify -Dit.test=<ClassName>`.
- Full sdk-tests `verify` must pass locally and on CI.
- The 8 non-migrated ITs must continue to pass unchanged.
- New `BaseContainerIT` and `SharedTestInfra` are exercised exclusively by the migrated ITs; no additional unit tests for them.

## Open questions

None at spec-approval time. Implementation plan will resolve concrete `DaprContainer` image tag, Redis image tag, Zipkin image tag, and `host.testcontainers.internal` wait strategy.

## Out of scope (future work)

- Migrating the 8 non-migrated ITs (especially the actor lifecycle group) once `DaprContainer` exposes friendlier sidecar restart APIs.
- Migrating the two [durabletask-client/](../../../durabletask-client/) ITs.
- Replacing `AppRun` with an in-JVM `EmbeddedAppServer` to remove subprocess overhead.
