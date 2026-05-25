# sdk-tests Testcontainers Migration — Session Handoff

**Date written:** 2026-05-25
**Branch:** `users/sveigraju/fix-integ-tests-2` (note typo in username — user's, not a mistake)
**Status:** ~70% complete; CI failing on actor ITs and a couple gRPC/tracing tests.

## Reference docs

- **Spec:** [../specs/2026-05-25-sdk-tests-testcontainers-migration-design.md](../specs/2026-05-25-sdk-tests-testcontainers-migration-design.md)
- **Plan:** [../plans/2026-05-25-sdk-tests-testcontainers-migration.md](../plans/2026-05-25-sdk-tests-testcontainers-migration.md)

## What landed

13 ITs migrated from `BaseIT`/`DaprRun` (legacy `dapr run` CLI) to `BaseContainerIT`/`DaprContainer` (Testcontainers):

- [SecretsClientIT](../../../sdk-tests/src/test/java/io/dapr/it/secrets/SecretsClientIT.java)
- [ApiIT](../../../sdk-tests/src/test/java/io/dapr/it/api/ApiIT.java)
- [ConfigurationClientIT](../../../sdk-tests/src/test/java/io/dapr/it/configuration/ConfigurationClientIT.java)
- [AbstractStateClientIT](../../../sdk-tests/src/test/java/io/dapr/it/state/AbstractStateClientIT.java) + [GRPCStateClientIT](../../../sdk-tests/src/test/java/io/dapr/it/state/GRPCStateClientIT.java) (Mongo test `@Disabled`)
- 4 actor ITs: ActorExceptionIT, ActivationDeactivationIT, ActorMethodNameIT, ActorTurnBasedConcurrencyIT
- 2 method-invoke ITs (http + grpc)
- 2 tracing ITs (http + grpc)

Plus foundation:
- [BaseContainerIT](../../../sdk-tests/src/test/java/io/dapr/it/containers/BaseContainerIT.java) — helpers + `@AfterAll` cleanup; no fields.
- [SharedTestInfra](../../../sdk-tests/src/test/java/io/dapr/it/containers/SharedTestInfra.java) — JVM-singleton Redis + Zipkin on shared `Network`, `withReuse(true)`.
- [AppRun.java](../../../sdk-tests/src/test/java/io/dapr/it/AppRun.java) — added a public 6-arg constructor accepting explicit Dapr HTTP/gRPC port overrides.
- jedis 5.1.0 added to [sdk-tests/pom.xml](../../../sdk-tests/pom.xml) (test scope) — replaces `docker exec dapr_redis redis-cli` shell-out in ConfigurationClientIT.
- CI: `[sdk-tests/deploy/local-test.yml](../../../sdk-tests/deploy/local-test.yml)` and `[.github/workflows/build.yml](../../../.github/workflows/build.yml)` trimmed to drop the Mongo service from compose-up.

9 ITs intentionally not migrated (per spec Non-Goals): BindingIT, ActorReminderFailoverIT, ActorReminderRecoveryIT, ActorTimerRecoveryIT, ActorStateIT, WaitForSidecarIT, ActorSdkResiliencyIT, plus 2 in `durabletask-client/`.

## Commits on the branch (newest first)

```
2e254491b Reorder: expose host ports AFTER dapr.start()
753264135 Diagnostic: stream daprd logs + DEBUG level
a187728b8 Fix CI: add wait helpers + secrets metadata
7421fdfae Fix CI: bypass legacy daprd-stdout success-message check
2d2c41ed2 Fix two CI failures in BaseContainerIT
7fa3139de Fix things                          ← user-applied
59dc3d813 fix things                          ← user-applied
8374d9611 CI: drop Mongo from local-test.yml + compose-up step
c9533343a Migrate TracingIT (grpc) to Testcontainers
082371fc2 Migrate TracingIT (http) to Testcontainers
bf8022fcd Migrate MethodInvokeIT (grpc) to Testcontainers
8cea6e308 Migrate MethodInvokeIT (http) to Testcontainers
3dafd354a Migrate ActorMethodNameIT to Testcontainers
ffa2add61 Migrate ActorTurnBasedConcurrencyIT to Testcontainers
e5ac323f8 Migrate ActivationDeactivationIT to Testcontainers
88d28d731 Migrate ActorExceptionIT to Testcontainers
1b76a7b6e Add Zipkin container to SharedTestInfra
81ab4deb9 Migrate state client ITs to Testcontainers
ad5bad7b4 Migrate ConfigurationClientIT to Testcontainers
103d8fc51 Migrate ApiIT to Testcontainers
b81291264 Use Map.of for SecretsClientIT payload helper
8f29e69d0 Migrate SecretsClientIT to Testcontainers
+ Phase 1 foundation commits earlier
```

## Current CI failure state (last run)

```
Failures:
  MethodInvokeIT.testInvokeException:130 expected: <UNKNOWN> but was: <UNAVAILABLE>
  TracingIT.testInvoke:104 expected: <false> but was: <true>     (×2 — http and grpc)
Errors (timeout 60s each):
  ActivationDeactivationIT       → "Timed out waiting for Dapr condition: any registered actors"
  ActorExceptionIT               → ditto
  ActorMethodNameIT              → ditto
  ActorTurnBasedConcurrencyIT    → ditto
  BindingIT.httpOutputBindingErrorIgnoredByComponent  ← non-migrated; ignore for this work
```

The 4 actor ITs all fail at `BaseContainerIT.waitForActorsReady` — daprd never reports any registered actors via its metadata endpoint.

## What's been tried (none of these fixed the actor failures)

| Commit | Attempt |
|---|---|
| `7421fdfae` | Bypassed brittle success-message check (was looking for `"dapr initialized. Status: Running"` which only daprd's stdout emits — invisible in the containerized world). Now passes `""` to `Command.run` so it returns on Maven's first banner line; AppRun's `assertListeningOnPort` is the real readiness signal. **This unblocked subprocess startup.** |
| `a187728b8` | (a) Added `nestedSeparator: ":"` + `multiValued: "true"` to SecretsClientIT's component metadata. (b) Added `waitForActorsReady(dapr)` helper using `DaprWait.forActors().waitUntilReady(dapr)`. (c) `client.waitForSidecar(30s)` round-trip in `startAppAndAttach` to harden gRPC readiness. |
| `753264135` | Diagnostic: streams daprd container logs to stdout via `withLogConsumer(...)` + bumps to DEBUG. Output prefixed with `[daprd]`. |
| `2e254491b` | Moved `Testcontainers.exposeHostPorts(appPort)` from before `dapr.start()` to after, matching spring-boot-4-sdk-tests' DaprActorsIT pattern. **Did not fix actor registration.** |

## What's left to try

Ranked by likelihood:

1. **Switch actor ITs to `state.in-memory`** instead of `state.redis`. Spring-boot-4 reference uses in-memory and works. Theory: daprd inside its container can't reach the shared Redis (`redis:6379` alias on the testcontainers Network), state store init fails, daprd refuses to register actors. The `[daprd]` logs from the diagnostic commit should confirm this once we get a CI run that includes them. Helper already drafted (was reverted): add `inMemoryActorStateStore(name)` to `BaseContainerIT`, swap call sites in 4 actor ITs.

2. **Inspect actual `[daprd]` logs from CI failsafe report.** The diagnostic commit is in place but the user's last paste didn't include `[daprd]`-prefixed lines. Need to grep the failsafe report for them. The full daprd log will show whether component init failed, whether app discovery is being attempted, what URL daprd is dialing, etc.

3. **For TracingIT `assertFalse(arr.isEmpty())` failures**: Zipkin returned no spans. Likely same daprd-instability symptom as actors (daprd not running long enough / not pushing spans). May fix automatically when actor issue is fixed.

4. **For MethodInvokeIT.testInvokeException UNAVAILABLE**: gRPC channel reset symptom; likely fixed once daprd is stable.

## Local validation blocker

Apple Claude Code's TCC sandbox blocks the Bash tool (and subprocesses) from accessing the Docker socket, even with `dangerouslyDisableSandbox: true`. The user's terminal `docker ps` works; spawned subprocesses don't. So Claude can't run `mvn verify` locally to iterate; everything has gone through GitHub Actions CI.

Workarounds:
- User runs `cd sdk-tests && JAVA_HOME=$(/usr/libexec/java_home -v 25) ../mvnw -B -Pintegration-tests -Dit.test=ActorExceptionIT -Dspotbugs.skip=true verify 2>&1 | tee /tmp/it.log`, pastes log.
- Or grant Claude Code's process Full Disk Access in System Settings → Privacy & Security, restart Claude Code.

## Other constraints worth knowing

- **`-s` sign-off required on every commit.** Project rule, captured in `~/.claude/projects/.../memory/feedback_signoff_commits.md`. Never use `Co-Authored-By: Claude` trailer.
- **Branch name has typo:** `users/sveigraju/fix-integ-tests-2` (note `sveigraju` vs the user's actual `svegiraju`). User's choice; don't "fix".
- **Java 17 required by the build** but only Java 13 and 25 installed locally. Java 25 works for compile; SpotBugs (`spotbugs-maven-plugin:4.8.2.0`) can't read Java 25 class files — pass `-Dspotbugs.skip=true`.
- **Maven wrapper jar (`maven-wrapper.jar`) is missing from the repo** and the network sandbox blocks downloads. Use system `mvn` instead of `./mvnw` from any spawned shell.

## Resuming the work

A new session should:

1. `cd /Users/svegiraju/Git/java-sdk`
2. `git checkout users/sveigraju/fix-integ-tests-2`
3. Read this file + the spec + the plan.
4. Get the latest CI failsafe report from the GitHub Actions run on this branch and grep for `[daprd]` lines.
5. Apply fix #1 (in-memory state store) if the daprd logs show component-init / Redis connectivity errors.
