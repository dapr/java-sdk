---
type: docs
title: "Dapr Java SDK"
linkTitle: "Java"
weight: 1000
description: Java SDK packages for developing Dapr applications
cascade:
  github_repo: https://github.com/dapr/java-sdk
  github_subdir: daprdocs/content/en/java-sdk-docs
  path_base_for_github_subdir: content/en/developing-applications/sdks/java/
  github_branch: master
---

## Prerequisites

- [Dapr CLI]({{< ref install-dapr-cli.md >}}) installed
- Initialized [Dapr environment]({{< ref install-dapr-selfhost.md >}})
- JDK 11 or above - the published jars are compatible with Java 8:
    - [AdoptOpenJDK 11 - LTS](https://adoptopenjdk.net/)
    - [Oracle's JDK 15](https://www.oracle.com/java/technologies/javase-downloads.html)
    - [Oracle's JDK 11 - LTS](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
    - [OpenJDK](https://openjdk.java.net/)
- Install one of the following build tools for Java:
    - [Maven 3.x](https://maven.apache.org/install.html)
    - [Gradle 6.x](https://gradle.org/install/)

## Import Dapr's Java SDK

Next, import the Java SDK packages to get started. Select your preferred build tool to learn how to import.

{{< tabs Maven Gradle >}}

{{% codetab %}}
<!--Maven-->

For a Maven project, add the following to your `pom.xml` file: 

```xml
<project>
  ...
  <dependencies>
    ...
    <!-- Dapr's core SDK with all features, except Actors. -->
    <dependency>
      <groupId>io.dapr</groupId>
      <artifactId>dapr-sdk</artifactId>
      <version>1.9.0</version>
    </dependency>
    <!-- Dapr's SDK for Actors (optional). -->
    <dependency>
      <groupId>io.dapr</groupId>
      <artifactId>dapr-sdk-actors</artifactId>
      <version>1.9.0</version>
    </dependency>
    <!-- Dapr's SDK integration with SpringBoot (optional). -->
    <dependency>
      <groupId>io.dapr</groupId>
      <artifactId>dapr-sdk-springboot</artifactId>
      <version>1.9.0</version>
    </dependency>
    ...
  </dependencies>
  ...
</project>
```
{{% /codetab %}}

{{% codetab %}}
<!--Gradle-->

For a Gradle project, add the following to your `build.gradle` file:

```java
dependencies {
...
    // Dapr's core SDK with all features, except Actors.
    compile('io.dapr:dapr-sdk:1.9.0')
    // Dapr's SDK for Actors (optional).
    compile('io.dapr:dapr-sdk-actors:1.9.0')
    // Dapr's SDK integration with SpringBoot (optional).
    compile('io.dapr:dapr-sdk-springboot:1.9.0')
}
```

{{% /codetab %}}

{{< /tabs >}}

If you are also using Spring Boot, you may run into a common issue where the `OkHttp` version that the Dapr SDK uses conflicts with the one specified in the Spring Boot _Bill of Materials_.

You can fix this by specifying a compatible `OkHttp` version in your project to match the version that the Dapr SDK uses:

```xml
<dependency>
  <groupId>com.squareup.okhttp3</groupId>
  <artifactId>okhttp</artifactId>
  <version>4.9.0</version>
</dependency>
```

## Try it out

Put the Dapr Java SDK to the test. Walk through the Java quickstarts and tutorials to see Dapr in action:

| SDK samples | Description |
| ----------- | ----------- |
| [Quickstarts]({{< ref quickstarts >}}) | Experience Dapr's API building blocks in just a few minutes using the Java SDK. |
| [SDK samples](https://github.com/dapr/java-sdk/tree/master/examples) | Clone the SDK repo to try out some examples and get started. |

## More information

Learn more about the [Dapr Java SDK packages available to add to your Java applications](https://dapr.github.io/java-sdk/).