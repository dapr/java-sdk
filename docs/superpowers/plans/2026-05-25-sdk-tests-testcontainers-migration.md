# sdk-tests Testcontainers Migration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate 12 sdk-tests integration tests (13 files) from the `dapr run`-based `BaseIT`/`DaprRun` harness to Testcontainers via the existing `DaprContainer` class.

**Architecture:** Introduce `SharedTestInfra` (JVM-singleton Redis/Zipkin via `withReuse(true)` on a shared Docker `Network`) and `BaseContainerIT` (abstract base providing helpers only; each subclass owns its own `private static DaprContainer dapr` / `AppRun app` fields). `AppRun` gains a constructor overload accepting explicit Dapr HTTP/gRPC ports so the app subprocess can point at the `DaprContainer`'s mapped ports. The existing `BaseIT` / `DaprRun` / `AppRun` / `DaprPorts` infrastructure stays intact for the 9 non-migrated ITs.

**Tech Stack:** JUnit 5 (Jupiter), Testcontainers (`testcontainers-junit-jupiter`, `testcontainers-dapr` — both already in [sdk-tests/pom.xml](../../../sdk-tests/pom.xml)), Maven Failsafe, `redis:7-alpine`, `openzipkin/zipkin:latest`, the [`io.dapr.testcontainers.DaprContainer`](../../../testcontainers-dapr/src/main/java/io/dapr/testcontainers/DaprContainer.java) class from the local `testcontainers-dapr` module.

**Spec:** [docs/superpowers/specs/2026-05-25-sdk-tests-testcontainers-migration-design.md](../specs/2026-05-25-sdk-tests-testcontainers-migration-design.md)

---

## File Structure

**New files (all under [sdk-tests/src/test/java/io/dapr/it/containers/](../../../sdk-tests/src/test/java/io/dapr/it/containers/)):**

| File | Responsibility |
|---|---|
| `SharedTestInfra.java` | JVM-singleton holder for Redis + Zipkin containers and a shared `Network`. Lazy startup, reuse enabled. |
| `BaseContainerIT.java` | Abstract base class: helpers (`daprBuilder`, `startApp`, `newDaprClient*`, `newActorClient*`, component factories, `deferClose`, `deferStop`) and `@AfterAll` cleanup. Holds no `DaprContainer`/`AppRun` fields. |

**Modified files:**

| File | Change |
|---|---|
| [sdk-tests/src/test/java/io/dapr/it/AppRun.java](../../../sdk-tests/src/test/java/io/dapr/it/AppRun.java) | Add a constructor overload `AppRun(DaprPorts ports, String successMessage, Class serviceClass, int maxWaitMilliseconds, Integer daprHttpPortOverride, Integer daprGrpcPortOverride)` that uses the override ports for the `DAPR_HTTP_PORT`/`DAPR_GRPC_PORT` env vars instead of `ports.getHttpPort()` / `ports.getGrpcPort()`. Existing callers untouched. |
| [.github/workflows/build.yml](../../../.github/workflows/build.yml) line 190 | `docker compose -f ./sdk-tests/deploy/local-test.yml up -d mongo kafka` → `docker compose -f ./sdk-tests/deploy/local-test.yml up -d kafka` |
| [sdk-tests/deploy/local-test.yml](../../../sdk-tests/deploy/local-test.yml) | Remove `mongo` service block |
| [sdk-tests/src/test/java/io/dapr/it/state/AbstractStateClientIT.java](../../../sdk-tests/src/test/java/io/dapr/it/state/AbstractStateClientIT.java) | `@Disabled("Requires MongoDB; not part of Testcontainers migration scope")` on `saveAndQueryAndDeleteState` (line 142). |

**Rewritten files (extend `BaseContainerIT` instead of `BaseIT`, `@BeforeAll` setup using `DaprContainer`):**

13 files — see Tasks 5–17 for each. Their `@Test` method bodies stay unchanged; only setup/teardown and field declarations change.

**Untouched (legacy ITs continue to extend `BaseIT`):**

- [sdk-tests/src/test/java/io/dapr/it/BaseIT.java](../../../sdk-tests/src/test/java/io/dapr/it/BaseIT.java)
- [sdk-tests/src/test/java/io/dapr/it/DaprRun.java](../../../sdk-tests/src/test/java/io/dapr/it/DaprRun.java)
- [sdk-tests/src/test/java/io/dapr/it/DaprPorts.java](../../../sdk-tests/src/test/java/io/dapr/it/DaprPorts.java)
- [sdk-tests/src/test/java/io/dapr/it/DaprRunConfig.java](../../../sdk-tests/src/test/java/io/dapr/it/DaprRunConfig.java)
- 9 non-migrated ITs (BindingIT, ActorReminderFailoverIT, ActorReminderRecoveryIT, ActorTimerRecoveryIT, ActorStateIT, WaitForSidecarIT, ActorSdkResiliencyIT, and the two durabletask-client ITs)
- [sdk-tests/components/](../../../sdk-tests/components/) YAMLs — still used by `dapr run` for the legacy ITs.

---

## How to test these tasks

Throughout this plan, the canonical commands are:

- **Compile only:** `(cd sdk-tests && ../mvnw test-compile -q)`
- **Single migrated IT:** `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=<ITClassName> -q)`
- **All migrated ITs after the migration is complete:** `(cd sdk-tests && ../mvnw verify -q)`

Docker must be running locally. On CI (GitHub `ubuntu-latest`) Docker is preinstalled.

Each task ends with a commit (frequent commits). Use the existing branch `users/svegiraju/fix-integ-tests`.

---

## Phase 1 — Foundation

### Task 1: `SharedTestInfra` (Redis only, Zipkin added later in Task 9)

**Files:**
- Create: [sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfra.java](../../../sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfra.java)
- Test: [sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfraTest.java](../../../sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfraTest.java)

- [ ] **Step 1: Write the failing test**

```java
// sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfraTest.java
package io.dapr.it.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedTestInfraTest {

  @Test
  void networkIsSingleton() {
    Network n1 = SharedTestInfra.network();
    Network n2 = SharedTestInfra.network();
    assertSame(n1, n2);
  }

  @Test
  void redisStartsAndIsReachable() {
    GenericContainer<?> redis = SharedTestInfra.redis();
    assertTrue(redis.isRunning());
    assertNotNull(redis.getMappedPort(6379));
    assertEquals("redis", redis.getNetworkAliases().get(0));
  }

  @Test
  void redisInternalHostFormat() {
    SharedTestInfra.redis();  // ensure started
    assertEquals("redis:6379", SharedTestInfra.redisInternalHost());
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `(cd sdk-tests && ../mvnw test -Dtest=SharedTestInfraTest -q)`
Expected: COMPILE FAILURE — `SharedTestInfra` does not exist.

- [ ] **Step 3: Implement `SharedTestInfra`**

```java
// sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfra.java
package io.dapr.it.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

/**
 * JVM-singleton holder for backing service containers shared across all
 * migrated integration tests. Containers are started lazily on first access
 * and reused for the lifetime of the JVM. With {@code withReuse(true)}, dev
 * machines that opt in via ~/.testcontainers.properties also reuse across
 * JVM runs.
 */
public final class SharedTestInfra {

  private static final String REDIS_NETWORK_ALIAS = "redis";
  private static final String ZIPKIN_NETWORK_ALIAS = "zipkin";

  private static volatile Network network;
  private static volatile GenericContainer<?> redis;
  private static volatile GenericContainer<?> zipkin;

  private SharedTestInfra() {}

  public static synchronized Network network() {
    if (network == null) {
      network = Network.newNetwork();
    }
    return network;
  }

  public static synchronized GenericContainer<?> redis() {
    if (redis == null) {
      redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
          .withNetwork(network())
          .withNetworkAliases(REDIS_NETWORK_ALIAS)
          .withExposedPorts(6379)
          .withReuse(true);
      redis.start();
    }
    return redis;
  }

  public static String redisInternalHost() {
    return REDIS_NETWORK_ALIAS + ":6379";
  }

  // Zipkin accessor added in Task 9.
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `(cd sdk-tests && ../mvnw test -Dtest=SharedTestInfraTest -q)`
Expected: 3 tests pass. Redis container pulls + starts on first invocation (~5-15s cold).

- [ ] **Step 5: Commit**

```bash
git add sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfra.java \
        sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfraTest.java
git commit -m "Add SharedTestInfra singleton for Redis container

Provides a JVM-wide Network and lazy Redis container shared across all
migrated integration tests. Uses withReuse(true) for dev-loop speed."
```

---

### Task 2: `AppRun` constructor overload with explicit Dapr port overrides

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/AppRun.java](../../../sdk-tests/src/test/java/io/dapr/it/AppRun.java)
- Test: [sdk-tests/src/test/java/io/dapr/it/AppRunOverrideTest.java](../../../sdk-tests/src/test/java/io/dapr/it/AppRunOverrideTest.java)

- [ ] **Step 1: Write the failing test**

```java
// sdk-tests/src/test/java/io/dapr/it/AppRunOverrideTest.java
package io.dapr.it;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppRunOverrideTest {

  /**
   * Verifies that when we construct AppRun with explicit Dapr port overrides,
   * the DAPR_HTTP_PORT / DAPR_GRPC_PORT env vars on the spawned command point
   * at the override values, not at the DaprPorts-allocated ones.
   */
  @Test
  void daprPortOverridesAreUsedInEnv() throws Exception {
    DaprPorts ports = DaprPorts.build(true, true, true);
    AppRun app = new AppRun(ports, "ready", Object.class, 1000, 12345, 67890);

    Field commandField = AppRun.class.getDeclaredField("command");
    commandField.setAccessible(true);
    Command command = (Command) commandField.get(app);

    Field envField = Command.class.getDeclaredField("env");
    envField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, String> env = (Map<String, String>) envField.get(command);

    assertEquals("12345", env.get("DAPR_HTTP_PORT"));
    assertEquals("67890", env.get("DAPR_GRPC_PORT"));
  }
}
```

- [ ] **Step 2: Verify the `Command` class shape**

Run: `grep -n 'class Command\|private.*env\|public Command' sdk-tests/src/test/java/io/dapr/it/Command.java`
Expected: confirms `Command` has an `env` field. If the field name is different, adjust the test in Step 1 accordingly. (If `Command` isn't in this directory, run `find sdk-tests/src/test -name 'Command.java'` to locate it.)

- [ ] **Step 3: Run test to verify it fails**

Run: `(cd sdk-tests && ../mvnw test -Dtest=AppRunOverrideTest -q)`
Expected: COMPILE FAILURE — the new 6-arg `AppRun` constructor does not exist.

- [ ] **Step 4: Add the constructor overload to `AppRun`**

Open [sdk-tests/src/test/java/io/dapr/it/AppRun.java](../../../sdk-tests/src/test/java/io/dapr/it/AppRun.java) and add this constructor immediately after the existing 4-arg constructor (around line 51):

```java
/**
 * Overload used by {@link io.dapr.it.containers.BaseContainerIT} when the Dapr
 * sidecar runs in a Testcontainer rather than via {@code dapr run}. The
 * {@code DAPR_HTTP_PORT} / {@code DAPR_GRPC_PORT} env vars on the spawned
 * app process point at the explicit override values (typically the
 * DaprContainer's mapped host ports) instead of {@code ports.getHttpPort() /
 * ports.getGrpcPort()}.
 */
AppRun(DaprPorts ports,
       String successMessage,
       Class serviceClass,
       int maxWaitMilliseconds,
       Integer daprHttpPortOverride,
       Integer daprGrpcPortOverride) {
  this.command = new Command(
          successMessage,
          buildCommand(serviceClass, ports),
          new HashMap<>() {{
            put("DAPR_HTTP_PORT", daprHttpPortOverride.toString());
            put("DAPR_GRPC_PORT", daprGrpcPortOverride.toString());
          }});
  this.ports = ports;
  this.maxWaitMilliseconds = maxWaitMilliseconds;
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `(cd sdk-tests && ../mvnw test -Dtest=AppRunOverrideTest -q)`
Expected: PASS.

- [ ] **Step 6: Confirm no existing callers break**

Run: `(cd sdk-tests && ../mvnw test-compile -q)`
Expected: clean compile. The existing 4-arg `AppRun(...)` constructor is unchanged.

- [ ] **Step 7: Commit**

```bash
git add sdk-tests/src/test/java/io/dapr/it/AppRun.java \
        sdk-tests/src/test/java/io/dapr/it/AppRunOverrideTest.java
git commit -m "Add AppRun constructor overload for explicit Dapr port overrides

Lets BaseContainerIT point the spawned app subprocess at a Testcontainer
DaprContainer's mapped HTTP/gRPC ports. Existing callers untouched."
```

---

### Task 3: `BaseContainerIT` skeleton (helpers only, no fields)

**Files:**
- Create: [sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java](../../../sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java)
- Test: [sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerITSmokeTest.java](../../../sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerITSmokeTest.java)

- [ ] **Step 1: Write the smoke test (acts as our first end-to-end check)**

```java
// sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerITSmokeTest.java
package io.dapr.it.containers;

import io.dapr.client.DaprClient;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Minimal smoke test that exercises BaseContainerIT's helpers end-to-end.
 * Boots a no-app DaprContainer with no components and verifies that we can
 * build a DaprClient against it and invoke a metadata call.
 */
class BaseContainerITSmokeTest extends BaseContainerIT {

  private static DaprContainer dapr;

  @BeforeAll
  static void init() {
    dapr = daprBuilder("smoke-test").build();
    dapr.start();
    deferStop(dapr);
  }

  @Test
  void canBuildAndUseDaprClient() {
    try (DaprClient client = newDaprClient(dapr)) {
      // waitForSidecar is a cheap healthcheck — it's fine if it returns immediately.
      client.waitForSidecar(5000).block();
      assertNotNull(client);
    }
  }
}
```

- [ ] **Step 2: Run the smoke test to confirm it fails to compile**

Run: `(cd sdk-tests && ../mvnw test-compile -q)`
Expected: COMPILE FAILURE — `BaseContainerIT` does not exist.

- [ ] **Step 3: Implement `BaseContainerIT`**

```java
// sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java
package io.dapr.it.containers;

import io.dapr.actors.client.ActorClient;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.config.Property;
import io.dapr.it.AppRun;
import io.dapr.it.DaprPorts;
import io.dapr.it.Stoppable;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.Testcontainers;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Base class for sdk-tests integration tests that run Dapr inside a
 * Testcontainer rather than via the local {@code dapr run} CLI.
 *
 * <p>Each subclass owns its own {@code private static DaprContainer dapr}
 * (and optionally {@code AppRun app}) field. This class holds no
 * Dapr/App fields itself — it only provides helpers and {@code @AfterAll}
 * cleanup hooks.
 *
 * <p>Lifecycle (per IT class):
 * <ol>
 *   <li>{@code @BeforeAll}: call {@link #startApp} (if needed), then build
 *       the DaprContainer via {@link #daprBuilder}, start it, and call
 *       {@link #deferStop}.</li>
 *   <li>{@code @AfterAll}: inherited cleanup drains deferStop (LIFO) then
 *       deferClose.</li>
 * </ol>
 */
public abstract class BaseContainerIT {

  /** Pinned Dapr runtime image. Matches what spring-boot-4-sdk-tests uses. */
  protected static final String DAPR_IMAGE = "daprio/daprd:1.15.6";

  protected static final String STATE_STORE_NAME = "statestore";
  protected static final String PUBSUB_NAME = "messagebus";
  protected static final String CONFIG_STORE_NAME = "redisconfigstore";

  private static final Deque<Stoppable> TO_BE_STOPPED = new LinkedList<>();
  private static final Deque<AutoCloseable> TO_BE_CLOSED = new LinkedList<>();

  // ---------- DaprContainer builder ----------

  /**
   * Returns a pre-configured {@link DaprContainer} builder wired into the
   * shared Network and Redis. Callers add components and (optionally) an app
   * port before calling {@code .build().start()}.
   */
  protected static DaprContainer daprBuilder(String appName) {
    SharedTestInfra.redis();   // ensure Redis is up before DaprContainer needs it
    return new DaprContainer(DAPR_IMAGE)
        .withAppName(appName)
        .withNetwork(SharedTestInfra.network())
        .withDaprLogLevel(DaprLogLevel.INFO)
        .withReusablePlacement(true);
  }

  // ---------- App lifecycle ----------

  /** Pair returned by {@link #startAppAndAttach}. */
  public record DaprAndApp(DaprContainer dapr, AppRun app) {}

  /**
   * Two-phase startup for ITs that need an app callback. Allocates the app
   * port, exposes it to Testcontainers, lets the caller build and start the
   * DaprContainer (which now knows the appPort + appChannelAddress), then
   * spawns the AppRun subprocess with the DaprContainer's mapped HTTP/gRPC
   * ports. Returns both. Both are registered for {@code @AfterAll} cleanup
   * via {@link #deferStop} (DaprContainer first, AppRun second — stopped LIFO).
   *
   * @param appName       used both as the Dapr app id and the AppRun name
   * @param serviceClass  the class whose {@code main(String[])} the subprocess runs
   * @param protocol      reserved for future use; AppRun currently ignores it
   * @param daprFactory   given the allocated app port, returns a STARTED
   *                      DaprContainer (factory body builds DaprContainer,
   *                      calls {@code .withAppPort(appPort)
   *                      .withAppChannelAddress("host.testcontainers.internal")},
   *                      and calls {@code .start()})
   */
  protected static DaprAndApp startAppAndAttach(
      String appName,
      Class<?> serviceClass,
      AppRun.AppProtocol protocol,
      java.util.function.IntFunction<DaprContainer> daprFactory) throws Exception {
    // Only the app port matters here — Dapr HTTP/gRPC ports will come from
    // the started DaprContainer's getMappedPort. Allocate only what we need.
    DaprPorts ports = DaprPorts.build(true, false, false);
    int appPort = ports.getAppPort();
    Testcontainers.exposeHostPorts(appPort);

    DaprContainer dapr = daprFactory.apply(appPort);
    // dapr is started inside the factory.
    deferStop(dapr);

    AppRun app = new AppRun(
        ports,
        getServiceSuccessMessage(serviceClass),
        serviceClass,
        60_000,
        dapr.getHttpPort(),
        dapr.getGrpcPort());
    app.start();
    deferStop(app);
    return new DaprAndApp(dapr, app);
  }

  /**
   * Best-effort lookup of a {@code public static final String SUCCESS_MESSAGE}
   * on the service class, falling back to {@code "You're up and running!"}.
   * Existing sdk-tests service classes follow this convention.
   */
  private static String getServiceSuccessMessage(Class<?> serviceClass) {
    try {
      Object value = serviceClass.getField("SUCCESS_MESSAGE").get(null);
      if (value instanceof String) {
        return (String) value;
      }
    } catch (NoSuchFieldException | IllegalAccessException ignored) {
      // fall through
    }
    return "You're up and running!";
  }

  // ---------- DaprClient / ActorClient factories ----------

  protected static DaprClient newDaprClient(DaprContainer dapr) {
    return newDaprClientBuilder(dapr).build();
  }

  protected static DaprClientBuilder newDaprClientBuilder(DaprContainer dapr) {
    return new DaprClientBuilder().withPropertyOverrides(daprOverrides(dapr));
  }

  protected static ActorClient newActorClient(DaprContainer dapr) {
    ActorClient client = new ActorClient(new Properties(daprOverrides(dapr)), null);
    deferClose(client);
    return client;
  }

  private static Map<Property<?>, String> daprOverrides(DaprContainer dapr) {
    Map<Property<?>, String> overrides = new HashMap<>();
    overrides.put(Properties.HTTP_ENDPOINT, "http://127.0.0.1:" + dapr.getHttpPort());
    overrides.put(Properties.GRPC_ENDPOINT, "127.0.0.1:" + dapr.getGrpcPort());
    overrides.put(Properties.HTTP_PORT, String.valueOf(dapr.getHttpPort()));
    overrides.put(Properties.GRPC_PORT, String.valueOf(dapr.getGrpcPort()));
    return overrides;
  }

  // ---------- Component helpers (Redis) ----------

  protected static Component redisStateStore(String name) {
    return new Component(name, "state.redis", "v1", Map.of(
        "redisHost", SharedTestInfra.redisInternalHost(),
        "redisPassword", "",
        "actorStateStore", "true"
    ));
  }

  protected static Component redisPubSub(String name) {
    return new Component(name, "pubsub.redis", "v1", Map.of(
        "redisHost", SharedTestInfra.redisInternalHost(),
        "redisPassword", "",
        "processingTimeout", "100ms",
        "redeliverInterval", "100ms"
    ));
  }

  protected static Component redisConfigStore(String name) {
    return new Component(name, "configuration.redis", "v1", Map.of(
        "redisHost", SharedTestInfra.redisInternalHost(),
        "redisPassword", ""
    ));
  }

  // ---------- Cleanup ----------

  protected static <T extends AutoCloseable> T deferClose(T object) {
    TO_BE_CLOSED.push(object);
    return object;
  }

  protected static void deferStop(Stoppable stoppable) {
    TO_BE_STOPPED.push(stoppable);
  }

  /**
   * Adapter so a Testcontainer can be registered alongside AppRuns in the
   * stop queue.
   */
  protected static void deferStop(org.testcontainers.containers.GenericContainer<?> container) {
    TO_BE_STOPPED.push(() -> container.stop());
  }

  @AfterAll
  protected static void cleanUp() throws Exception {
    while (!TO_BE_STOPPED.isEmpty()) {
      try {
        TO_BE_STOPPED.pop().stop();
      } catch (Exception e) {
        // best-effort
        e.printStackTrace();
      }
    }
    while (!TO_BE_CLOSED.isEmpty()) {
      try {
        TO_BE_CLOSED.pop().close();
      } catch (Exception e) {
        // best-effort
        e.printStackTrace();
      }
    }
  }
}
```

Notes for the implementer:
- The `startApp` stub that throws `UnsupportedOperationException` exists only as a tombstone — actor ITs will call `startAppAndAttach`. If you find a cleaner API after building one IT, you may delete `startApp` and rename `startAppAndAttach` to `startApp`.
- `Stoppable` is the existing interface at [sdk-tests/src/test/java/io/dapr/it/Stoppable.java](../../../sdk-tests/src/test/java/io/dapr/it/Stoppable.java) — verify it has a single `void stop()` method (or `throws InterruptedException`). Adjust the lambda in the `deferStop(GenericContainer)` overload if the signature differs.

- [ ] **Step 4: Run the smoke test**

Run: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=BaseContainerITSmokeTest -q)`

Wait — this is a `*Test`, not `*IT`, so Surefire runs it. Re-run as:
Run: `(cd sdk-tests && ../mvnw test -Dtest=BaseContainerITSmokeTest -q)`
Expected: PASS. Redis + DaprContainer start (cold image pull on first run: 30-60s). The `waitForSidecar` call returns successfully.

- [ ] **Step 5: Commit**

```bash
git add sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java \
        sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerITSmokeTest.java
git commit -m "Add BaseContainerIT helpers + smoke test

Provides daprBuilder, startAppAndAttach, newDaprClient(dapr), Component
factories, and @AfterAll cleanup. Each subclass owns its own static
DaprContainer + AppRun fields (D10 from the spec).

Smoke test boots a no-component DaprContainer to verify the helper
plumbing end-to-end."
```

---

## Phase 2 — Easy ITs (no app callback)

### Task 4: Migrate `SecretsClientIT`

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/secrets/SecretsClientIT.java](../../../sdk-tests/src/test/java/io/dapr/it/secrets/SecretsClientIT.java)
- Move: [sdk-tests/components/secret.json](../../../sdk-tests/components/secret.json) → also referenced from classpath. Check whether it's already a test resource via `find sdk-tests/src/test/resources -name 'secret.json'`. If not present in `src/test/resources/`, copy it there for `MountableFile.forClasspathResource` to find.

- [ ] **Step 1: Verify the existing test currently fails (or passes via legacy harness) before migration**

Run: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=SecretsClientIT -q)`
Expected: depends on local Dapr install. If it passes, note that as the baseline. If it fails because `dapr` isn't installed, that's also fine — after migration it should pass without `dapr`.

- [ ] **Step 2: Ensure `secret.json` is on the classpath**

```bash
ls sdk-tests/src/test/resources/ 2>/dev/null
ls sdk-tests/components/secret.json
```

If `sdk-tests/src/test/resources/secret.json` doesn't exist:

```bash
mkdir -p sdk-tests/src/test/resources
cp sdk-tests/components/secret.json sdk-tests/src/test/resources/secret.json
```

(We keep the original in `sdk-tests/components/` because legacy ITs still reference it via `dapr run --components-path`.)

- [ ] **Step 3: Rewrite `SecretsClientIT`**

Replace the contents of [sdk-tests/src/test/java/io/dapr/it/secrets/SecretsClientIT.java](../../../sdk-tests/src/test/java/io/dapr/it/secrets/SecretsClientIT.java). Keep the `@Test` method bodies unchanged; only the imports, class declaration, and `@BeforeAll` setup change.

```java
package io.dapr.it.secrets;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SecretsClientIT extends BaseContainerIT {

  private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();
  private static final String SECRETS_STORE_NAME = "localSecretStore";
  private static final String LOCAL_SECRET_FILE_PATH = "./src/test/resources/secret.json";
  private static final String KEY1 = UUID.randomUUID().toString();
  private static final String KYE2 = UUID.randomUUID().toString();

  private static DaprContainer dapr;
  private static File localSecretFile;
  private DaprClient daprClient;

  @BeforeAll
  public static void init() throws Exception {
    localSecretFile = new File(LOCAL_SECRET_FILE_PATH);
    assertTrue(localSecretFile.exists(), "Expected " + LOCAL_SECRET_FILE_PATH + " on disk");
    initSecretFile();

    dapr = daprBuilder("secrets-it")
        .withComponent(new Component(SECRETS_STORE_NAME, "secretstores.local.file", "v1", Map.of(
            "secretsFile", "/dapr-secret.json"
        )))
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("secret.json"),
            "/dapr-secret.json"
        );
    dapr.start();
    deferStop(dapr);
  }

  @BeforeEach
  public void setup() {
    this.daprClient = newDaprClient(dapr);
  }

  @AfterEach
  public void tearDown() throws Exception {
    daprClient.close();
    clearSecretFile();
  }

  @Test
  public void getSecret() throws Exception {
    Map<String, String> data = daprClient.getSecret(SECRETS_STORE_NAME, KEY1).block();
    assertEquals(2, data.size());
    assertEquals("The Metrics IV", data.get("title"));
    assertEquals("2020", data.get("year"));
  }

  @Test
  public void getBulkSecret() throws Exception {
    Map<String, Map<String, String>> data = daprClient.getBulkSecret(SECRETS_STORE_NAME).block();
    assertTrue(data.size() >= 2);
    assertEquals(2, data.get(KEY1).size());
    assertEquals("The Metrics IV", data.get(KEY1).get("title"));
    assertEquals("2020", data.get(KEY1).get("year"));
    assertEquals(1, data.get(KYE2).size());
    assertEquals("Jon Doe", data.get(KYE2).get("name"));
  }

  @Test
  public void getSecretKeyNotFound() {
    assertThrows(RuntimeException.class, () -> daprClient.getSecret(SECRETS_STORE_NAME, "unknownKey").block());
  }

  @Test
  public void getSecretStoreNotFound() {
    assertThrows(RuntimeException.class, () -> daprClient.getSecret("unknownStore", "unknownKey").block());
  }

  private static void initSecretFile() throws Exception {
    Map<String, Object> key2 = new HashMap<>() {{ put("name", "Jon Doe"); }};
    Map<String, Object> key1 = new HashMap<>() {{
      put("title", "The Metrics IV");
      put("year", "2020");
    }};
    Map<String, Map<String, Object>> secret = new HashMap<>() {{
      put(KEY1, key1);
      put(KYE2, key2);
    }};
    try (FileOutputStream fos = new FileOutputStream(localSecretFile)) {
      JSON_SERIALIZER.writeValue(fos, secret);
    }
  }

  private static void clearSecretFile() throws IOException {
    try (FileOutputStream fos = new FileOutputStream(localSecretFile)) {
      IOUtils.write("{}", fos);
    }
  }
}
```

Note: `secret.json` is mounted into the container as `/dapr-secret.json` and the Component's `secretsFile` metadata points at that container path.

- [ ] **Step 4: Run the migrated IT**

Run: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=SecretsClientIT -q)`
Expected: 4 tests pass. Redis + Dapr containers start; ~20s wall-clock for cold start.

- [ ] **Step 5: Commit**

```bash
git add sdk-tests/src/test/java/io/dapr/it/secrets/SecretsClientIT.java \
        sdk-tests/src/test/resources/secret.json
git commit -m "Migrate SecretsClientIT to Testcontainers

Boots Dapr via DaprContainer with secretstores.local.file pointing at a
file mounted from classpath via MountableFile. No application callback
needed."
```

---

### Task 5: Migrate `ApiIT`

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/api/ApiIT.java](../../../sdk-tests/src/test/java/io/dapr/it/api/ApiIT.java)

- [ ] **Step 1: Inspect the existing IT to understand its tests**

Run: `cat sdk-tests/src/test/java/io/dapr/it/api/ApiIT.java`

Note which `@Test` methods exist, and the in-method `startDaprApp(this.getClass().getSimpleName(), DEFAULT_TIMEOUT)` pattern. After migration these tests will share one DaprContainer.

- [ ] **Step 2: Rewrite `ApiIT` setup**

Replace the class declaration, imports, and field/setup section. Keep all `@Test` method bodies unchanged but:
- Replace any `DaprRun run = startDaprApp(...)` lines with use of the shared static `dapr`.
- Replace `run.newDaprClientBuilder().build()` with `newDaprClient(dapr)`.

Pattern:

```java
package io.dapr.it.api;

// existing imports minus io.dapr.it.BaseIT, io.dapr.it.DaprRun
import io.dapr.client.DaprClient;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ApiIT extends BaseContainerIT {

  private static DaprContainer dapr;

  @BeforeAll
  static void init() {
    dapr = daprBuilder("api-it");
    dapr.start();
    deferStop(dapr);
  }

  // existing @Test methods, but each one now does:
  //   try (DaprClient client = newDaprClient(dapr)) { ... }
  // instead of allocating its own DaprRun.
}
```

- [ ] **Step 3: Run the migrated IT**

Run: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=ApiIT -q)`
Expected: All tests pass. Watch for any test that relied on a fresh sidecar — if one fails with "metadata already exists" / "previous test polluted state", note it and either:
- namespace the test's keys/IDs with a UUID, or
- restore per-method DaprContainer for just that IT (fallback per D9).

- [ ] **Step 4: Commit**

```bash
git add sdk-tests/src/test/java/io/dapr/it/api/ApiIT.java
git commit -m "Migrate ApiIT to Testcontainers

Lifecycle shifts from in-method startDaprApp to per-class @BeforeAll.
Tests share one DaprContainer; verified state-independence per @Test."
```

---

### Task 6: Migrate `ConfigurationClientIT`

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/configuration/ConfigurationClientIT.java](../../../sdk-tests/src/test/java/io/dapr/it/configuration/ConfigurationClientIT.java)

- [ ] **Step 1: Audit how config values are seeded today**

Run: `grep -n 'redis-cli\|jedis\|Runtime\.getRuntime\|ProcessBuilder' sdk-tests/src/test/java/io/dapr/it/configuration/ConfigurationClientIT.java`

If the test shells out to `redis-cli`, that command runs against host port 6379 today. Post-migration we need to use Jedis pointed at `SharedTestInfra.redis().getMappedPort(6379)`.

- [ ] **Step 2: Verify `jedis` is available**

Run: `grep -n 'jedis' sdk-tests/pom.xml`
If absent, add it under `<dependencies>` in [sdk-tests/pom.xml](../../../sdk-tests/pom.xml):

```xml
<dependency>
  <groupId>redis.clients</groupId>
  <artifactId>jedis</artifactId>
  <version>5.1.0</version>
  <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Rewrite `ConfigurationClientIT`**

Replace setup. Pattern:

```java
package io.dapr.it.configuration;

// imports ...
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.it.containers.SharedTestInfra;
import io.dapr.testcontainers.DaprContainer;
import redis.clients.jedis.Jedis;

public class ConfigurationClientIT extends BaseContainerIT {

  private static DaprContainer dapr;
  private static Jedis jedis;

  @BeforeAll
  static void init() {
    dapr = daprBuilder("config-it")
        .withComponent(redisConfigStore("redisconfigstore"));
    dapr.start();
    deferStop(dapr);

    jedis = new Jedis(
        SharedTestInfra.redis().getHost(),
        SharedTestInfra.redis().getMappedPort(6379));
    deferClose(jedis);
  }

  // Replace any redis-cli shell-out with jedis.set(...) / jedis.publish(...) / etc.
  // The existing @Test method bodies use DaprClient — replace with newDaprClient(dapr).
}
```

- [ ] **Step 4: Run the migrated IT**

Run: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=ConfigurationClientIT -q)`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add sdk-tests/src/test/java/io/dapr/it/configuration/ConfigurationClientIT.java sdk-tests/pom.xml
git commit -m "Migrate ConfigurationClientIT to Testcontainers

Seeds Redis via Jedis against the shared Redis container instead of
shelling out to redis-cli on the host. Adds jedis as a test dependency."
```

---

### Task 7: Migrate state ITs (`AbstractStateClientIT` + `GRPCStateClientIT`) and disable the Mongo test

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/state/AbstractStateClientIT.java](../../../sdk-tests/src/test/java/io/dapr/it/state/AbstractStateClientIT.java)
- Modify: [sdk-tests/src/test/java/io/dapr/it/state/GRPCStateClientIT.java](../../../sdk-tests/src/test/java/io/dapr/it/state/GRPCStateClientIT.java)

- [ ] **Step 1: Inspect both files**

Run:
- `head -60 sdk-tests/src/test/java/io/dapr/it/state/AbstractStateClientIT.java`
- `cat sdk-tests/src/test/java/io/dapr/it/state/GRPCStateClientIT.java`

Note: `AbstractStateClientIT` is abstract and provides the test body. `GRPCStateClientIT` is the concrete subclass that wires the gRPC client. There may also be `HTTPStateClientIT` — confirm with `find sdk-tests/src/test/java/io/dapr/it/state -name '*.java'`.

- [ ] **Step 2: `@Disabled` the Mongo-dependent test in `AbstractStateClientIT`**

In [AbstractStateClientIT.java](../../../sdk-tests/src/test/java/io/dapr/it/state/AbstractStateClientIT.java), line 142, add `@org.junit.jupiter.api.Disabled` immediately above the `@Test` on `saveAndQueryAndDeleteState`:

```java
@org.junit.jupiter.api.Disabled("Requires MongoDB query state store; out of scope for Testcontainers migration.")
@Test
public void saveAndQueryAndDeleteState() throws JsonProcessingException {
  // unchanged body
}
```

- [ ] **Step 3: Change `AbstractStateClientIT` to extend `BaseContainerIT` and configure Dapr**

```java
public abstract class AbstractStateClientIT extends BaseContainerIT {

  protected static DaprContainer dapr;

  @BeforeAll
  static void initState() {
    dapr = daprBuilder("state-it")
        .withComponent(redisStateStore(STATE_STORE_NAME));
    dapr.start();
    deferStop(dapr);
  }

  // Replace `protected DaprClient buildDaprClient()` (or whatever the abstract
  // hook is named) so that subclasses can still pick HTTP vs gRPC. Most likely
  // the existing abstract method returns a DaprClient; have it delegate to
  // newDaprClient(dapr) — possibly via a protocol override.
}
```

- [ ] **Step 4: Update `GRPCStateClientIT`**

Trim it down to:

```java
public class GRPCStateClientIT extends AbstractStateClientIT {
  // override whatever's necessary to force gRPC protocol on the client.
  // If buildDaprClient() reads a Properties override, the override goes here.
}
```

- [ ] **Step 5: Run both ITs**

Run: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=GRPCStateClientIT -q)`
Expected: all `@Test` methods pass except `saveAndQueryAndDeleteState` (skipped).

If there's an `HTTPStateClientIT` discovered in Step 1, run it too.

- [ ] **Step 6: Commit**

```bash
git add sdk-tests/src/test/java/io/dapr/it/state/
git commit -m "Migrate state client ITs to Testcontainers

AbstractStateClientIT now configures one Redis state store (actor enabled)
via DaprContainer in @BeforeAll. The single MongoDB-dependent test
(saveAndQueryAndDeleteState) is @Disabled — out of scope per the spec.
GRPCStateClientIT extends the new base."
```

---

## Phase 3 — Zipkin in `SharedTestInfra` (prep for TracingIT)

### Task 8: Extend `SharedTestInfra` with Zipkin

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfra.java](../../../sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfra.java)
- Modify: [sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfraTest.java](../../../sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfraTest.java)

- [ ] **Step 1: Extend the test**

Add to `SharedTestInfraTest`:

```java
@Test
void zipkinStartsAndIsReachable() {
  GenericContainer<?> z = SharedTestInfra.zipkin();
  assertTrue(z.isRunning());
  assertNotNull(z.getMappedPort(9411));
  assertEquals("zipkin", z.getNetworkAliases().get(0));
}

@Test
void zipkinInternalEndpointFormat() {
  SharedTestInfra.zipkin();
  assertEquals("http://zipkin:9411/api/v2/spans", SharedTestInfra.zipkinInternalEndpoint());
}
```

- [ ] **Step 2: Run to confirm failure**

Run: `(cd sdk-tests && ../mvnw test -Dtest=SharedTestInfraTest -q)`
Expected: compile failure on `SharedTestInfra.zipkin()`.

- [ ] **Step 3: Add Zipkin to `SharedTestInfra`**

Append to [SharedTestInfra.java](../../../sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfra.java):

```java
public static synchronized GenericContainer<?> zipkin() {
  if (zipkin == null) {
    zipkin = new GenericContainer<>(DockerImageName.parse("openzipkin/zipkin:latest"))
        .withNetwork(network())
        .withNetworkAliases(ZIPKIN_NETWORK_ALIAS)
        .withExposedPorts(9411)
        .withReuse(true);
    zipkin.start();
  }
  return zipkin;
}

public static String zipkinInternalEndpoint() {
  return "http://" + ZIPKIN_NETWORK_ALIAS + ":9411/api/v2/spans";
}
```

- [ ] **Step 4: Run to confirm pass**

Run: `(cd sdk-tests && ../mvnw test -Dtest=SharedTestInfraTest -q)`
Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfra.java \
        sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfraTest.java
git commit -m "Add Zipkin container to SharedTestInfra"
```

---

## Phase 4 — Actor ITs (need app callback)

All four actor ITs follow the same migration pattern. The IT classes today use one of `StatefulActorService` / `ActorService` / similar. Confirm the exact service class per file with `grep 'startDaprApp\|Service.class' <file>`.

### Task 9: Migrate `ActorExceptionIT`

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/actors/ActorExceptionIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorExceptionIT.java)

- [ ] **Step 1: Inspect**

Run: `cat sdk-tests/src/test/java/io/dapr/it/actors/ActorExceptionIT.java`

Identify the service class and the test pattern.

- [ ] **Step 2: Rewrite setup**

`startAppAndAttach` returns a `DaprAndApp` record (defined in Task 3) so the caller gets both the started `DaprContainer` and the `AppRun`. Adapt names to match the actual service class:

```java
package io.dapr.it.actors;

import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.AppRun;
import io.dapr.it.actors.app.SomeActorService;   // adjust to actual class
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ActorExceptionIT extends BaseContainerIT {

  private static DaprContainer dapr;
  private static AppRun app;
  private static ActorClient actorClient;

  @BeforeAll
  static void init() throws Exception {
    var pair = startAppAndAttach(
        "actor-exception-it",
        SomeActorService.class,
        AppRun.AppProtocol.HTTP,
        appPort -> {
          DaprContainer d = daprBuilder("actor-exception-it")
              .withAppPort(appPort)
              .withAppChannelAddress("host.testcontainers.internal")
              .withComponent(redisStateStore(STATE_STORE_NAME));
          d.start();
          return d;
        });
    dapr = pair.dapr();
    app = pair.app();
    actorClient = newActorClient(dapr);
  }

  // existing @Test method bodies, with these replacements:
  //   - run.getAppName() -> "actor-exception-it"
  //   - run.newActorClient() -> actorClient
  //   - run.newDaprClientBuilder().build() -> newDaprClient(dapr)
}
```

- [ ] **Step 3: Update the `@Test` method bodies**

The tests today reference `run.getAppName()` / `run.newActorClient()`. Replace with the literal app name string (`"actor-exception-it"`) and the static `actorClient`.

- [ ] **Step 4: Run**

Run: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=ActorExceptionIT -q)`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add sdk-tests/src/test/java/io/dapr/it/actors/ActorExceptionIT.java
git commit -m "Migrate ActorExceptionIT to Testcontainers"
```

---

### Task 10: Migrate `ActivationDeactivationIT`

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/actors/ActivationDeactivationIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActivationDeactivationIT.java)

- [ ] **Step 1: Inspect**

Run: `cat sdk-tests/src/test/java/io/dapr/it/actors/ActivationDeactivationIT.java`

This file currently calls `startDaprApp` from inside `@Test` bodies. After migration, the sidecar starts once in `@BeforeAll`. Audit each `@Test` for actor-ID uniqueness; if a test reuses the same actor ID as another test, append a `UUID.randomUUID()` suffix.

- [ ] **Step 2: Rewrite using the same pattern as `ActorExceptionIT`**

```java
public class ActivationDeactivationIT extends BaseContainerIT {

  private static DaprContainer dapr;
  private static AppRun app;
  private static ActorClient actorClient;

  @BeforeAll
  static void init() throws Exception {
    var pair = startAppAndAttach(
        "activation-deactivation-it",
        StatefulActorService.class,   // verify this is the right class
        AppRun.AppProtocol.HTTP,
        appPort -> {
          DaprContainer d = daprBuilder("activation-deactivation-it")
              .withAppPort(appPort)
              .withAppChannelAddress("host.testcontainers.internal")
              .withComponent(redisStateStore(STATE_STORE_NAME));
          d.start();
          return d;
        });
    dapr = pair.dapr();
    app = pair.app();
    actorClient = newActorClient(dapr);
  }

  // @Test bodies: replace var run = startDaprApp(...) with use of static fields.
}
```

- [ ] **Step 3: Run**

Run: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=ActivationDeactivationIT -q)`
Expected: PASS. If a test fails with stale actor state, namespace its actor ID with a UUID.

- [ ] **Step 4: Commit**

```bash
git add sdk-tests/src/test/java/io/dapr/it/actors/ActivationDeactivationIT.java
git commit -m "Migrate ActivationDeactivationIT to Testcontainers

Per-class @BeforeAll lifecycle. Actor IDs verified unique across @Test
methods."
```

---

### Task 11: Migrate `ActorTurnBasedConcurrencyIT`

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/actors/ActorTurnBasedConcurrencyIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorTurnBasedConcurrencyIT.java)

- [ ] **Step 1: Inspect**: `cat sdk-tests/src/test/java/io/dapr/it/actors/ActorTurnBasedConcurrencyIT.java`
- [ ] **Step 2: Rewrite** using the same pattern as Task 10. Adjust the service class to whatever this IT uses.
- [ ] **Step 3: Run**: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=ActorTurnBasedConcurrencyIT -q)`
- [ ] **Step 4: Commit**:
```bash
git add sdk-tests/src/test/java/io/dapr/it/actors/ActorTurnBasedConcurrencyIT.java
git commit -m "Migrate ActorTurnBasedConcurrencyIT to Testcontainers"
```

---

### Task 12: Migrate `ActorMethodNameIT`

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/actors/ActorMethodNameIT.java](../../../sdk-tests/src/test/java/io/dapr/it/actors/ActorMethodNameIT.java)

- [ ] **Step 1: Inspect**: `cat sdk-tests/src/test/java/io/dapr/it/actors/ActorMethodNameIT.java`
- [ ] **Step 2: Rewrite** using the same pattern as Task 10.
- [ ] **Step 3: Run**: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=ActorMethodNameIT -q)`
- [ ] **Step 4: Commit**:
```bash
git add sdk-tests/src/test/java/io/dapr/it/actors/ActorMethodNameIT.java
git commit -m "Migrate ActorMethodNameIT to Testcontainers"
```

---

## Phase 5 — Method invoke ITs

### Task 13: Migrate `MethodInvokeIT (http)`

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/methodinvoke/http/MethodInvokeIT.java](../../../sdk-tests/src/test/java/io/dapr/it/methodinvoke/http/MethodInvokeIT.java)

- [ ] **Step 1: Inspect**

Run: `cat sdk-tests/src/test/java/io/dapr/it/methodinvoke/http/MethodInvokeIT.java`

Note: today this uses `@BeforeEach` to spin a fresh DaprRun per `@Test`. The migration switches to `@BeforeAll`. All `@Test` methods invoke methods on `daprRun.getAppName()` — replace with the literal app name.

- [ ] **Step 2: Rewrite**

```java
public class MethodInvokeIT extends BaseContainerIT {

  private static final String APP_NAME = "methodinvoke-http-it";
  private static final int NUM_MESSAGES = 10;

  private static DaprContainer dapr;
  private static AppRun app;

  @BeforeAll
  static void init() throws Exception {
    var pair = startAppAndAttach(
        APP_NAME,
        MethodInvokeService.class,
        AppRun.AppProtocol.HTTP,
        appPort -> {
          DaprContainer d = daprBuilder(APP_NAME)
              .withAppPort(appPort)
              .withAppChannelAddress("host.testcontainers.internal");
          d.start();
          return d;
        });
    dapr = pair.dapr();
    app = pair.app();
  }

  // @Test bodies stay the same but:
  //   - use newDaprClient(dapr) instead of daprRun.newDaprClientBuilder().build()
  //   - use APP_NAME instead of daprRun.getAppName()
}
```

**Cross-test state warning**: this IT mutates a server-side message map. Earlier `@Test` methods leave state in the app. Today that's safe because each `@Test` got a fresh sidecar AND a fresh app subprocess. After migration the app is shared — verify that `@Test` methods either don't depend on a clean state or order their assertions accordingly. If a test fails because of leftover messages from a previous test, add `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` and `@Order(n)` annotations, or refactor to use per-test message prefixes.

- [ ] **Step 3: Run**

Run: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=io.dapr.it.methodinvoke.http.MethodInvokeIT -q)`
Expected: PASS. If fails, see the warning above.

- [ ] **Step 4: Commit**

```bash
git add sdk-tests/src/test/java/io/dapr/it/methodinvoke/http/MethodInvokeIT.java
git commit -m "Migrate MethodInvokeIT (http) to Testcontainers

@BeforeEach -> @BeforeAll. Verified or refactored @Test methods for
shared-state independence."
```

---

### Task 14: Migrate `MethodInvokeIT (grpc)`

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/methodinvoke/grpc/MethodInvokeIT.java](../../../sdk-tests/src/test/java/io/dapr/it/methodinvoke/grpc/MethodInvokeIT.java)

- [ ] **Step 1**: Inspect — should be structurally similar to the http variant.
- [ ] **Step 2**: Apply the same rewrite pattern; use `AppRun.AppProtocol.GRPC` and `daprBuilder(...).withAppProtocol(DaprProtocol.GRPC)`.
- [ ] **Step 3**: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=io.dapr.it.methodinvoke.grpc.MethodInvokeIT -q)`
- [ ] **Step 4**: Commit:
```bash
git add sdk-tests/src/test/java/io/dapr/it/methodinvoke/grpc/MethodInvokeIT.java
git commit -m "Migrate MethodInvokeIT (grpc) to Testcontainers"
```

---

## Phase 6 — Tracing ITs (Zipkin)

### Task 15: Migrate `TracingIT (http)` with per-test trace-ID assertions

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/tracing/http/TracingIT.java](../../../sdk-tests/src/test/java/io/dapr/it/tracing/http/TracingIT.java)

- [ ] **Step 1: Inspect today's assertion strategy**

Run: `cat sdk-tests/src/test/java/io/dapr/it/tracing/http/TracingIT.java`

Note: today each `@Test` gets a fresh sidecar + fresh Zipkin (or fresh sidecar talking to a local Zipkin if one exists). Tests likely assert against "all spans since the test started." Post-migration, Zipkin is shared and accumulates spans across all tests.

- [ ] **Step 2: Build tracing `Configuration`**

```java
import io.dapr.testcontainers.Configuration;
import io.dapr.testcontainers.TracingConfigurationSettings;
import io.dapr.testcontainers.ZipkinTracingConfigurationSettings;

// in init():
SharedTestInfra.zipkin();   // ensure started

dapr = daprBuilder(APP_NAME)
    .withAppPort(appPort)
    .withAppChannelAddress("host.testcontainers.internal")
    .withConfiguration(new Configuration(
        "tracing",
        new TracingConfigurationSettings(
            "1",                                          // samplingRate
            true,                                         // stdout
            null,
            new ZipkinTracingConfigurationSettings(SharedTestInfra.zipkinInternalEndpoint())
        ),
        null                                             // appHttpPipeline
    ));
```

(Check the actual `TracingConfigurationSettings` / `ZipkinTracingConfigurationSettings` constructor signatures via `cat testcontainers-dapr/src/main/java/io/dapr/testcontainers/TracingConfigurationSettings.java` and `cat testcontainers-dapr/src/main/java/io/dapr/testcontainers/ZipkinTracingConfigurationSettings.java`.)

- [ ] **Step 3: Refactor test assertions to query by per-test trace ID**

Today the test might do something like "fetch all spans, assert count == 1". Change to:

```java
@Test
void someTracedCall() {
  String traceId = generateTraceId();   // 32 hex chars
  // make the dapr call with a manually constructed traceparent header containing traceId

  // query Zipkin for spans with this traceId
  String url = "http://" + SharedTestInfra.zipkin().getHost()
      + ":" + SharedTestInfra.zipkin().getMappedPort(9411)
      + "/api/v2/trace/" + traceId;
  // poll with retry until span(s) appear or timeout
  // assert against the contents of THIS trace, not all spans in Zipkin
}
```

If the existing test sets up the trace context via OpenTelemetry SDK (the test pom imports `opentelemetry-exporter-zipkin`), reuse that machinery and just record the trace ID for the assertion query.

- [ ] **Step 4: Run**

Run: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=io.dapr.it.tracing.http.TracingIT -q)`
Expected: PASS. May need a `Retry` helper (poll Zipkin) because span ingestion is asynchronous — there's likely one in [sdk-tests/src/test/java/io/dapr/it/Retry.java](../../../sdk-tests/src/test/java/io/dapr/it/Retry.java).

- [ ] **Step 5: Commit**

```bash
git add sdk-tests/src/test/java/io/dapr/it/tracing/http/TracingIT.java
git commit -m "Migrate TracingIT (http) to Testcontainers

Asserts on per-test trace ID via Zipkin REST instead of total span count,
since Zipkin is shared across @Test methods after the @BeforeEach ->
@BeforeAll switch."
```

---

### Task 16: Migrate `TracingIT (grpc)`

**Files:**
- Modify: [sdk-tests/src/test/java/io/dapr/it/tracing/grpc/TracingIT.java](../../../sdk-tests/src/test/java/io/dapr/it/tracing/grpc/TracingIT.java)

- [ ] **Step 1**: Inspect.
- [ ] **Step 2**: Apply the same pattern as Task 15 with `AppRun.AppProtocol.GRPC` and `daprBuilder(...).withAppProtocol(DaprProtocol.GRPC)`.
- [ ] **Step 3**: `(cd sdk-tests && ../mvnw failsafe:integration-test -Dit.test=io.dapr.it.tracing.grpc.TracingIT -q)`
- [ ] **Step 4**: Commit:
```bash
git add sdk-tests/src/test/java/io/dapr/it/tracing/grpc/TracingIT.java
git commit -m "Migrate TracingIT (grpc) to Testcontainers"
```

---

## Phase 7 — Full suite verification + CI

### Task 17: Full sdk-tests `verify` (catches cross-IT interactions)

- [ ] **Step 1: Run the full suite**

Run: `(cd sdk-tests && ../mvnw verify -q)` (this runs both Surefire unit tests and Failsafe ITs).

Expected: all 22 ITs pass. The 13 migrated use Testcontainers; the 9 legacy ITs still use `dapr run` (Dapr CLI must be installed and `dapr init` already run for them locally — same prereq as today).

- [ ] **Step 2: If failures, triage**

| Symptom | Likely cause | Fix |
|---|---|---|
| State bleed across `@Test`s in a migrated IT | per-class lifecycle exposes a latent test interdep | Namespace IDs with UUIDs; if intractable, fall back to per-method `DaprContainer` for just that IT |
| Port collisions between sequential IT classes | Surefire fork reused `host.testcontainers.internal` mapping | Each IT class allocates a fresh app port; Testcontainers handles this — investigate |
| Zipkin spans missing under load | async ingestion not given enough time | Increase poll retries in the trace-id assertion helper |
| Cold Docker image pull times out | network latency | Pre-pull images locally; reuse takes over after first run |

- [ ] **Step 3: Commit any fixes uncovered in Step 2**

Per-fix; no batch commit.

---

### Task 18: CI changes

**Files:**
- Modify: [sdk-tests/deploy/local-test.yml](../../../sdk-tests/deploy/local-test.yml)
- Modify: [.github/workflows/build.yml](../../../.github/workflows/build.yml)

- [ ] **Step 1: Inspect `local-test.yml`**

Run: `cat sdk-tests/deploy/local-test.yml`

Identify the `mongo` service block.

- [ ] **Step 2: Remove `mongo` service**

Edit [sdk-tests/deploy/local-test.yml](../../../sdk-tests/deploy/local-test.yml) to delete only the `mongo:` service stanza. Leave everything else (kafka, etc.) untouched.

- [ ] **Step 3: Update CI**

In [.github/workflows/build.yml](../../../.github/workflows/build.yml), line 190:

Change:
```yaml
        docker compose -f ./sdk-tests/deploy/local-test.yml up -d mongo kafka
```

To:
```yaml
        docker compose -f ./sdk-tests/deploy/local-test.yml up -d kafka
```

- [ ] **Step 4: Verify the compose file still parses**

Run: `docker compose -f sdk-tests/deploy/local-test.yml config -q`
Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add sdk-tests/deploy/local-test.yml .github/workflows/build.yml
git commit -m "CI: drop Mongo from local-test.yml + compose-up step

The only Mongo consumer (AbstractStateClientIT#saveAndQueryAndDeleteState)
is now @Disabled as part of the Testcontainers migration."
```

---

## Phase 8 — Push and observe

### Task 19: Push the branch and watch CI

- [ ] **Step 1: Push**

```bash
git push -u origin users/svegiraju/fix-integ-tests
```

- [ ] **Step 2: Watch the CI run**

```bash
gh run watch
```

Or open the run in the GitHub UI.

- [ ] **Step 3: If CI fails, triage per Task 17 Step 2 table; if it passes, the migration is done.**

---

## Done criteria

- [ ] All 22 sdk-tests ITs run (13 migrated, 9 legacy).
- [ ] `(cd sdk-tests && ../mvnw verify -q)` passes locally.
- [ ] CI build on the branch is green.
- [ ] `BaseIT`, `DaprRun`, `AppRun`, `DaprPorts`, `DaprRunConfig` are unchanged except for the additive `AppRun` constructor overload.
- [ ] The 9 non-migrated ITs (listed in [the spec](../specs/2026-05-25-sdk-tests-testcontainers-migration-design.md)) are unchanged.
- [ ] No new `*.java` test files outside [sdk-tests/src/test/java/io/dapr/it/containers/](../../../sdk-tests/src/test/java/io/dapr/it/containers/) other than the rewritten IT bodies.
