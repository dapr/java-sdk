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

## Pre-requisites

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

## Installation

Install the required JDK using [SDKMAN!](https://sdkman.io/):

```bash
sdk env install
```

[Next, import the Java SDK packages to get started]({{< ref java-client.md >}}). 

## Try it out

Put the Dapr Java SDK to the test. Walk through the Java quickstarts adn tutorials to see Dapr in action:

| SDK samples | Description |
| ----------- | ----------- |
| [Quickstarts] | Experience Dapr's API building blocks in just a few minutes using the Java SDK. |
| [SDK samples](https://github.com/dapr/java-sdk/tree/master/examples) | Clone the SDK repo to try out some examples and get started. |

## More information

Learn more about the [Dapr Java SDK packages available to add to your Java applications](https://dapr.github.io/java-sdk/).