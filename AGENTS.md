# AGENTS.md — Dapr Java SDK

This file provides context for AI agents working on the Dapr Java SDK.
The project is the official Java SDK for [Dapr](https://dapr.io/) (Distributed Application Runtime),
enabling Java developers to build distributed applications using Dapr building blocks.

Repository: https://github.com/dapr/java-sdk
License: Apache 2.0

## Before you open a pull request

These rules apply to every agent, whether it acts on its own or on behalf of a human contributor.
Pull requests that ignore them will be closed without review.

1. **Work autonomously only on issues labeled `ready-for-agent`.** A maintainer adds this label
   when the approach is settled and an agent may pick up the issue and open a PR without a human
   driving it. Check with `gh issue view <number> --json labels`.
2. **Otherwise, work only for a human a maintainer assigned.** Without the label, do not open a PR
   unless the issue it resolves is assigned to the human you are working for, and a maintainer made
   that assignment. Self-assignment with the `/assign` bot command does not count. Check who
   assigned the issue with `gh api repos/dapr/java-sdk/issues/<number>/events`: the `assigned` event's
   `assigner` must be a maintainer. If there is no issue, or it is not assigned this way, stop and
   ask in the issue instead of opening a PR.
3. **Get the approach approved when the change needs decisions.** If the PR requires design
   decisions, post the proposed approach as a comment on the issue and wait until at least one
   maintainer (`@dapr/maintainers-java-sdk`, see `CODEOWNERS`) has approved it. Only then open the
   PR, and link the approval comment in the PR description. A change needs decisions when it does
   any of the following:
   - adds or changes a public API (new methods, classes, builders, configuration properties)
   - changes existing behavior that users may depend on, or is a breaking change
   - adds a new dependency or changes a dependency's major version
   - has more than one reasonable implementation and you had to pick one

## Project structure

```
sdk/                         # Core SDK (io.dapr:dapr-sdk): DaprClient, DaprPreviewClient, serializers, config
sdk-autogen/                 # gRPC/protobuf stubs generated at build time from the Dapr protos (DO NOT EDIT)
sdk-actors/                  # Actor framework (virtual actor model)
sdk-workflows/               # Workflow authoring API (io.dapr.workflows)
durabletask-client/          # Durable Task gRPC client and worker used by sdk-workflows (io.dapr.durabletask)
sdk-springboot/              # Spring Boot integration for actors and pub/sub callbacks
dapr-spring/                 # Spring Boot 4 integrations: auto-configuration, starters, Spring Data,
                             #   Spring Messaging, workflows, observation, BOM
testcontainers-dapr/         # Testcontainers module for running daprd in tests
sdk-bom/                     # Bill of materials for the SDK artifacts
examples/                    # User-facing examples, validated in CI with mechanical-markdown
spring-boot-examples/        # Spring Boot examples, validated in CI with mechanical-markdown
sdk-tests/                   # Integration tests against a real Dapr runtime (integration-tests profile)
spring-boot-sdk-tests/       # Spring Boot integration tests (integration-tests profile)
docs/                        # Generated Javadoc, published per release (DO NOT EDIT BY HAND)
```

## Key architectural patterns

- **Client architecture**: `DaprClient` holds the stable APIs. `DaprPreviewClient` holds alpha and
  preview APIs. `DaprClientImpl` implements both over gRPC. Build clients with `DaprClientBuilder`.
- **Reactive API**: Client methods return Project Reactor `Mono`/`Flux`. Callers block with `.block()`.
- **Actor model**: `ActorRuntime` hosts actors server-side; `ActorProxyBuilder` creates client-side proxies.
- **Workflows**: Users write `Workflow` and `WorkflowActivity` implementations and run them with
  `WorkflowRuntimeBuilder`; `DaprWorkflowClient` schedules and manages instances. The engine underneath
  is `durabletask-client`.
- **Serialization**: Pluggable via `DaprObjectSerializer`. `DefaultObjectSerializer` (Jackson) is the default.
- **Protos**: `sdk-autogen` downloads the Dapr and Durable Task proto files at build time from the URLs in
  the root `pom.xml` (`dapr.proto.baseurl`, `durabletask.proto.baseurl`) and generates the Java stubs.

## Java version support

- **Minimum**: Java 17 (`maven.compiler.release` is 17)
- **Tested in CI**: 17, 21, 25
- **Local builds**: use JDK 17 (`.sdkmanrc` and `mise.toml` pin Temurin 17). Some build plugins
  (JaCoCo, SpotBugs) can fail on newer JDKs.

## Building and testing

Use the Maven wrapper.

```bash
# Build everything and run unit tests (what the "Unit tests" CI job runs)
./mvnw clean install -DskipITs=true

# Build without tests
./mvnw clean install -DskipTests

# Build and test one module and the modules it depends on
./mvnw -pl sdk-workflows -am install

# Run one test class
./mvnw -pl sdk -Dtest=DaprClientGrpcTest test
```

Integration tests need Docker. `sdk-tests` also needs the Dapr CLI with `dapr init` done.

```bash
# Integration tests (sdk-tests, spring-boot-examples, spring-boot-sdk-tests)
./mvnw -B -pl '!durabletask-client' -Pintegration-tests clean dependency:copy-dependencies verify
```

## Code style and quality gates

These run in the `validate` phase of every build. A violation fails the build.

- **Checkstyle** (`checkstyle.xml`): 2-space indentation, 120-character lines, no star imports,
  ordered imports.
- **License header**: Every Java file must start with the Apache header in `.java_header`
  (`Copyright <year> The Dapr Authors`). Copy it from a neighboring file.
- **SpotBugs**: Fails the build on findings. Exclusions live in `spotbugs-exclude.xml`; add one only
  with a reason.
- **JaCoCo**: Enforces 80% coverage on the modules that enable it.

## Examples

New APIs need an example. `CONTRIBUTING.md` requires it.

- Examples live under `examples/src/main/java/io/dapr/examples/<area>/`, each with a `README.md`.
- The README contains mechanical-markdown `<!-- STEP -->` blocks. CI runs them with `mm.py` and
  compares the output to `expected_stdout_lines`. Changing what an example prints can break CI.
- A new example README must also be added to the list in `.github/workflows/validate.yml`.
- Run one locally: `cd examples && mm.py ./src/main/java/io/dapr/examples/<area>/README.md`
  (needs `pip install mechanical-markdown` and the Dapr CLI).

## Commit and PR conventions

- **DCO required**: Every commit must include a `Signed-off-by` line. Use `git commit -s`.
- **Branch targets**: PRs go to `master` or `release-*` branches.
- **PR template**: Fill in `.github/pull_request_template.md`, including the issue reference.
- **Backports**: Maintainers add a `backport release-X.Y` label; a bot opens the backport PR after merge.

## Agent task checklist

Not every item applies to every change. Consider each one.

### Before writing code

- [ ] Confirm the issue has the `ready-for-agent` label, or a maintainer assigned it to the human you work for (see "Before you open a pull request")
- [ ] If the change needs decisions, confirm a maintainer approved the approach in the issue
- [ ] Read the existing source files in the area you are changing and follow their patterns
- [ ] Check whether the change also belongs in `DaprPreviewClient` or the Spring integrations in `dapr-spring`

### Implementation

- [ ] Do not edit generated code (`sdk-autogen` output) or `docs/`
- [ ] New alpha or preview Dapr APIs go in `DaprPreviewClient`, not `DaprClient`
- [ ] Add the license header to new Java files
- [ ] Add Javadoc to new public types and methods

### Tests

- [ ] Add or update unit tests in the module you changed (JUnit 5, Mockito)
- [ ] If the change touches runtime behavior end to end, add or update a test in `sdk-tests`

### Examples and documentation

- [ ] New user-facing API: add an example with a mechanical-markdown README and register it in `validate.yml`
- [ ] Changed output of an existing example: update its `expected_stdout_lines`

### Final verification

- [ ] `./mvnw clean install -DskipITs=true` passes (includes Checkstyle and SpotBugs)
- [ ] Commits are signed off: `git commit -s`

## Important files

| File | Purpose |
|------|---------|
| `pom.xml` | Parent POM: modules, dependency versions, proto URLs, plugin config |
| `checkstyle.xml` | Code style rules |
| `.java_header` | Required license header pattern |
| `spotbugs-exclude.xml` | SpotBugs exclusions |
| `.github/workflows/build.yml` | Unit tests, integration tests, release |
| `.github/workflows/validate.yml` | Runs the examples with mechanical-markdown |
| `CONTRIBUTING.md` | Contribution guidelines |

## Gotchas

- **Generated code**: Never edit the generated protobuf classes. To pick up proto changes, change the
  proto URLs in the root `pom.xml`.
- **Examples are tested by output matching**: Changing log or print output can break `validate.yml`.
- **Background processes in examples**: Examples that start an app in the background must stop it in
  a later STEP, or CI hangs.
- **DCO signoff**: The DCO bot blocks PRs whose commits lack `Signed-off-by`.
