# Support and Version Compatibility

The Dapr Java SDK is released in version lines, and **the SDK version is the primary axis**: each
line is built against a specific Dapr runtime API and — for the Spring integration modules — a
specific Spring Boot generation. Importing `io.dapr.spring:dapr-spring-bom` (or `io.dapr:dapr-sdk-bom`)
at a given version pins the whole stack, core and Spring, to that single version.

## Supported versions

| SDK line    | Dapr runtime API | Spring Boot (Spring modules only) | JDK | Status                                            |
|-------------|------------------|-----------------------------------|-----|---------------------------------------------------|
| **1.19.x+** | v1.18            | 4.0.x                             | 17+ | Active — new features and fixes                   |
| **1.18.x**  | v1.18            | 3.5.x                             | 17+ | Maintenance — best-effort security/critical fixes |

The **core** SDK modules (`dapr-sdk`, `dapr-sdk-actors`, `dapr-sdk-workflows`, `durabletask-client`)
are framework-agnostic and do not depend on Spring Boot — the Spring Boot column applies only to the
Spring integration modules (`dapr-sdk-springboot`, `dapr-spring-*`). Core and Spring modules always
share the same SDK version (the Spring BOM imports `dapr-sdk-bom` at its own version), so they never
diverge.

### Spring Boot stack detail

For the Spring integration modules, each Spring Boot generation implies the following stack:

| Spring Boot | Spring Framework | Jakarta EE | Jackson             | JUnit |
|-------------|------------------|------------|---------------------|-------|
| 4.0.x       | 7.x              | 11         | 3 (`tools.jackson`) | 6     |
| 3.5.x       | 6.x              | 10         | 2 (`com.fasterxml`) | 5     |

Spring Boot 4 is **not** binary-compatible with Spring Boot 3 (Spring Framework 6→7, Jakarta EE
10→11, Jackson 2→3), so a single build cannot serve both — which is why Spring Boot support is split
across SDK lines.

## Choosing a version

- **Spring integration on Spring Boot 4.0.x** → **1.19.x+**
- **Spring integration on Spring Boot 3.5.x** → **1.18.x**
- **Core SDK only (no Spring)** → any supported line; choose by the Dapr runtime you target.

Mixing a 1.19.x Spring starter into a Spring Boot 3 application (or a 1.18.x starter into Spring
Boot 4) results in runtime failures such as `NoClassDefFoundError` / `NoSuchMethodError`, caused by
the incompatible Spring Framework, Jakarta namespace, and Jackson versions.

## Dapr runtime compatibility

Dapr runtime compatibility applies to **all** SDK modules (core and Spring alike) and does not change
with your Spring Boot version — the SDK talks to the Dapr sidecar over a versioned gRPC/HTTP API,
pinned per SDK line (see the table above). Because the Dapr runtime maintains API backward
compatibility within a major version, an SDK line also works against later runtime releases. See the
[Dapr version support policy](https://docs.dapr.io/operations/support/support-release-policy/) for the
runtime's own supported-version window.

## Support policy

- **1.19.x (Spring Boot 4.0)** is the actively developed line.
- **1.18.x (Spring Boot 3.5)** is maintained on a best-effort basis for security and critical fixes,
  as a bridge for users still on Spring Boot 3.5 while they move to Spring Boot 4. Spring Boot 3.5's
  own OSS support window has ended (see the
  [Spring Boot support page](https://spring.io/projects/spring-boot#support)), so users on Spring
  Boot 3.x are encouraged to upgrade.

## Migrating from Spring Boot 3.5 to 4.0

1. Upgrade your application to Spring Boot 4.0, following the official
   [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki).
2. Move the Dapr SDK dependency (or `dapr-spring-bom` import) to **1.19.x**. The artifact coordinates
   are unchanged; only the version differs.
