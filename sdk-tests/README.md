# Invocation integration tests

Run with JDK 17 and Docker from the repository root. The Maven integration
profile stages the runtime automatically; no separate dependency-copy goal is
needed for these suites:

```sh
./mvnw -B -Pintegration-tests -pl sdk-tests -am -Djacoco.skip=true '-Dit.test=io.dapr.it.testcontainers.methodinvoke.http.MethodInvokeIT,io.dapr.it.testcontainers.methodinvoke.grpc.MethodInvokeIT' -Dfailsafe.failIfNoSpecifiedTests=false verify
```

Select either fully qualified class name to run a single suite. Check the
`sdk-tests/target/failsafe-reports` XML for the expected five HTTP and three
gRPC tests. For foundation lifecycle checks, select
`io.dapr.it.testcontainers.support.DaprTestEnvironmentIT` instead.
If an incremental build reports SpotBugs findings in generated protobuf
classes, rerun with `clean verify`.

The Docker-free readiness tests run with Surefire outside the
integration profile (after installing the SDK dependencies locally):

```sh
./mvnw -B -f sdk-tests/pom.xml -Dspotbugs.exclude.filter.file="$PWD/spotbugs-exclude.xml" -Dtest=GrpcCallbackWaitStrategyTest clean test
```

The application runtime is a read-only bind mount of `sdk-tests/target/it-runtime`;
it must be accessible to the Docker daemon, so remote Docker is not guaranteed
to work. The application container uses a fixed Java 17 image even when Maven
runs on Java 21 or 25. Other integration tests may still require the explicit
`dependency:copy-dependencies` goal and additional infrastructure.
