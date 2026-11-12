# Container-only integration tests

The migrated HTTP method-invocation suite owns its application, daprd, placement,
scheduler and network. Docker is required; the application runs in a Java container,
not through a host Maven process. Maven stages its test classpath automatically at
`pre-integration-test`. The application container mounts that staged directory
read-only, rather than copying the payload into each container. Docker must be able
to access the workspace path; a remote Docker daemon must have the same path mounted.

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
