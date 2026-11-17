# Containerized SDK integration tests

The migrated HTTP/gRPC invocation and HTTP binding tests use an isolated
`DaprTestEnvironment`. Run the binding suite with JDK 17 and Docker:

```sh
./mvnw -B -Pintegration-tests -pl sdk-tests -am -Djacoco.skip=true \
  -Dit.test=io.dapr.it.testcontainers.binding.http.BindingIT \
  -Dfailsafe.failIfNoSpecifiedTests=false clean verify
```

The integration lifecycle stages the test runtime automatically; no separate
dependency-copy goal is needed for these tests. The binding suite owns its Kafka
broker, uses the private `kafka:19092` listener and a single-partition topic,
and uses the callback application as a deterministic HTTP error fixture.
Inspect `sdk-tests/target/failsafe-reports` for the actual discovered methods.
An incremental `verify` can report SpotBugs findings in previously generated
protobuf classes; the clean command avoids that existing build issue.

Run the Docker-backed ownership and readiness checks with the same command,
substituting `io.dapr.it.testcontainers.support.DaprTestEnvironmentIT` for
the binding class. The integration profile skips Surefire, so after installing
the reactor artifacts, run the Docker-free support checks separately:

```sh
./mvnw -B -f sdk-tests/pom.xml clean \
  '-Dtest=OwnedResourcesTest,GrpcCallbackWaitStrategyTest,MigratedBoundaryTest,InputBindingControllerTest' test
```

The shared environment currently uses a Java 17 image and a read-only bind mount
of `target/it-runtime`. Other Java versions and remote Docker hosts therefore
retain the same image and host filesystem assumptions as the invocation suites.
