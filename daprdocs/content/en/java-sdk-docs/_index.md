---
type: docs
title: "Dapr Java SDK"
linkTitle: "Java"
weight: 1000
description: Java SDK packages for developing Dapr applications
---

## Pre-requisites

- [Dapr CLI]({{< ref install-dapr-cli.md >}}) installed
- Initialized [Dapr environment]({{< ref install-dapr-selfhost.md >}})
- JDK 11 or above - the published jars are compatible with Java 8:
    - [Oracle's JDK 15](https://www.oracle.com/java/technologies/javase-downloads.html)
    - [Oracle's JDK 11 - LTS](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
    - [OpenJDK](https://openjdk.java.net/)
- Install one of the following build tools for Java:
    - [Maven 3.x](https://maven.apache.org/install.html)
    - [Gradle 6.x](https://gradle.org/install/)

## Importing Dapr's Java SDK

For a Maven project, add the following to your `pom.xml` file: 
```java
<project>
  ...
  <dependencies>
    ...
     // Dapr's core SDK with all features, except Actors. 
    <dependency>
      <groupId>io.dapr</groupId>
      <artifactId>dapr-sdk</artifactId>
      <version>1.1.0</version>
    </dependency>
    // Dapr's SDK for Actors (optional).
    <dependency>
      <groupId>io.dapr</groupId>
      <artifactId>dapr-sdk-actors</artifactId>
      <version>1.1.0</version>
    </dependency>
    // Dapr's SDK integration with SpringBoot (optional).
    <dependency>
      <groupId>io.dapr</groupId>
      <artifactId>dapr-sdk-springboot</artifactId>
      <version>1.1.0</version>
    </dependency>
    ...
  </dependencies>
  ...
</project>
```

For a Gradle project, add the following to your `build.gradle` file:
```java
dependencies {
...
    // Dapr's core SDK with all features, except Actors.
    compile('io.dapr:dapr-sdk:1.1.0')
    // Dapr's SDK for Actors (optional).
    compile('io.dapr:dapr-sdk-actors:1.1.0')
    // Dapr's SDK integration with SpringBoot (optional).
    compile('io.dapr:dapr-sdk-springboot:1.1.0')
}
```

## Building blocks

The Java SDK allows you to interface with all of the [Dapr building blocks]({{< ref building-blocks >}}).

### Invoke a service

```java
// Java Example here
```

- For a full guide on service invocation visit [How-To: Invoke a service]({{< ref howto-invoke-discover-services.md >}}).
- Visit [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples/invoke) for code samples and instructions to try out service invocation

### Save & get application state

```java
// Java Example here
```

- For a full list of state operations visit [How-To: Get & save state]({{< ref howto-get-save-state.md >}}).
- Visit [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples/state) for code samples and instructions to try out state management

### Publish & subscribe to messages

##### Publish messages

```java
// Java Example here
```

##### Subscribe to messages

```java
// Java Example here
```

- For a full list of state operations visit [How-To: Publish & subscribe]({{< ref howto-publish-subscribe.md >}}).
- Visit [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples/pubsub/http) for code samples and instructions to try out pub/sub

### Interact with output bindings

```java
// Java Example here
```

- For a full guide on output bindings visit [How-To: Use bindings]({{< ref howto-bindings.md >}}).
- Visit [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples/bindings/http) for code samples and instructions to try out output bindings

### Retrieve secrets

```java
// Java Example here
```

- For a full guide on secrets visit [How-To: Retrieve secrets]({{< ref howto-secrets.md >}}).
- Visit [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples/secrets) for code samples and instructions to try out retrieving secrets

### Actors

```java
// Java Example here
```

## Related links
- [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples)