# Container-only integration tests

The migrated HTTP method-invocation suite owns its application, daprd, placement,
scheduler and network. Docker is required; the application runs in a Java container,
not through a host Maven process. Maven stages its test classpath automatically at
`pre-integration-test`.

From the repository root, run:

```sh
./mvnw -B -Pintegration-tests -pl sdk-tests -am -Djacoco.skip=true \
  -Dit.test=io.dapr.it.testcontainers.methodinvoke.http.MethodInvokeIT \
  -Dfailsafe.failIfNoSpecifiedTests=false clean verify
```

The application image defaults to the Temurin JDK matching the test JVM's major
version (17, 21 or 25). Override it with `-Ddapr.it.java.image=registry/image:tag`;
the image must provide a compatible `java` executable. Container logs identify the
selected image and environment. The full legacy integration suite still requires
the existing explicit `dependency:copy-dependencies` preparation.

Run the Docker-free foundation and architecture tests with Surefire (the ordinary
integration-tests profile disables Surefire):

```sh
./mvnw -B -Pintegration-tests,container-foundation-unit-tests -pl sdk-tests -am \
  -Djacoco.skip=true -Dspotbugs.skip=true -Dtest=FoundationTest,ArchitectureTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

The unit-test command skips SpotBugs because its validate-phase analysis reports
existing findings in previously generated protobuf classes on incremental builds.
The clean integration build above still runs the normal quality gates.
