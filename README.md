## Dapr SDK for Java

[![Build Status](https://github.com/dapr/java-sdk/workflows/Build/badge.svg?event=push&branch=master)](https://github.com/dapr/java-sdk/actions?workflow=Build)
[![Gitter](https://badges.gitter.im/Dapr/java-sdk.svg)](https://gitter.im/Dapr/java-sdk?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![codecov](https://codecov.io/gh/dapr/java-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/dapr/java-sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This is the Dapr SDK for Java, including the following features:

* PubSub
* Service Invocation
* Binding
* State Store
* Actors

### Getting Started

#### Pre-Requisites
* Java IDE installed:
    * [IntelliJ](https://www.jetbrains.com/idea/download/)
    * [Eclipse](https://www.eclipse.org/downloads/)
    * [Apache NetBeans](https://netbeans.apache.org/download/index.html)
    * [Visual Studio Code](https://code.visualstudio.com/Download)
    * Any other IDE for Java that you prefer.
* Install one of the following build tools for Java:
    * [Maven 3.x](https://maven.apache.org/install.html)
    * [Gradle 6.x](https://gradle.org/install/)
* If needed, install the corresponding plugin for the build tool in your IDE, for example:
    * [Maven in IntelliJ](https://www.jetbrains.com/help/idea/maven.html)
    * [Gradle in IntelliJ](https://www.jetbrains.com/help/idea/gradle-settings.html)
    * [Maven in Eclipse with m2e](https://projects.eclipse.org/projects/technology.m2e)
    * [Gradle in Eclipse with Buildship](https://projects.eclipse.org/projects/tools.buildship)
* An existing Java Maven or Gradle project. You may also start a new project via one of the options below:
    * [New Maven project in IntelliJ](https://www.jetbrains.com/help/idea/maven-support.html#create_new_maven_project)
    * [Maven in 5 minutes](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)

#### Importing Dapr's Java SDK
For a Maven project, add the following to your `pom.xml` file:
```xml
<project>
  ...
  <repositories>
    ...
    <!-- BEGIN: Dapr's repositories -->
    <repository>
      <id>oss-snapshots</id>
      <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </repository>
    <repository>
      <id>oss-release</id>
      <url>https://oss.sonatype.org/content/repositories/releases/</url>
    </repository>
    <!-- END: Dapr's repositories -->
    ...
  </repositories>
  ...
  <dependencyManagement>
      <dependencies>
        ...
         <!-- Dapr's core SDK with all features, except Actors. -->
        <dependency>
          <groupId>io.dapr</groupId>
          <artifactId>dapr-sdk</artifactId>
          <version>0.9.2</version>
        </dependency>
        <!-- Dapr's SDK for Actors (optional). -->
        <dependency>
          <groupId>io.dapr</groupId>
          <artifactId>dapr-sdk-actors</artifactId>
          <version>0.9.2</version>
        </dependency>
        <!-- Dapr's SDK integration with SpringBoot (optional). -->
        <dependency>
          <groupId>io.dapr</groupId>
          <artifactId>dapr-sdk-springboot</artifactId>
          <version>0.9.2</version>
        </dependency>
        <!-- If needed, resolve version conflict of okhttp3. -->
        <dependency>
          <groupId>com.squareup.okhttp3</groupId>
          <artifactId>okhttp</artifactId>
          <version>4.2.2</version> <!-- version required by Dapr's sdk -->
        </dependency>
        ...
      </dependencies>
  </dependencyManagement>
</project>
```

For a Gradle project, add the following to your `build.gradle` file:

```
repositories {
    ...
    // Dapr repositories
    maven {
      url "https://oss.sonatype.org/content/repositories/snapshots"
	  mavenContent {
	    snapshotsOnly()
	  }
    }
    maven {
	  url "https://oss.sonatype.org/content/repositories/releases/"
    }
}
...
dependencies {
...
    // Dapr's core SDK with all features, except Actors.
    compile('io.dapr:dapr-sdk:0.9.2')
    // Dapr's SDK for Actors (optional).
    compile('io.dapr:dapr-sdk-actors:0.9.2')
    // Dapr's SDK integration with SpringBoot (optional).
    compile('io.dapr:dapr-sdk-springboot:0.9.2')

    // If needed, force conflict resolution for okhttp3.
    configurations.all {
        resolutionStrategy.force 'com.squareup.okhttp3:okhttp:4.2.2'
    }
}
```

#### Running the examples
Clone this repository including the submodules:

```sh
git clone https://github.com/dapr/java-sdk.git
```

Then head over to build the [Maven](https://maven.apache.org/install.html) (Apache Maven version 3.x) project:

```sh
# make sure you are in the `java-sdk` directory.
mvn clean install
```

Try the following examples to learn more about Dapr's Java SDK:
* [Invoking a Http service](./examples/src/main/java/io/dapr/examples/invoke/http)
* [Invoking a Grpc service](./examples/src/main/java/io/dapr/examples/invoke/grpc)
* [State management](./examples/src/main/java/io/dapr/examples/state)
* [PubSub with subscriber over Http](./examples/src/main/java/io/dapr/examples/pubsub/http)
* [Binding with input over Http](./examples/src/main/java/io/dapr/examples/bindings/http)
* [Actors over Http](./examples/src/main/java/io/dapr/examples/actors/http)
* [Secrets management](./examples/src/main/java/io/dapr/examples/secrets)
* [Distributed tracing with OpenTelemetry SDK](./examples/src/main/java/io/dapr/examples/tracing)

#### API Documentation

Please, refer to our [Javadoc](https://dapr.github.io/java-sdk/) website.

#### Reactor API

The Java SDK for Dapr is built using [Project Reactor](https://projectreactor.io/). It provides an asynchronous API for Java. When consuming a result is consumed synchronously, as in the examples referenced above, the `block()` method is used.

The code below does not make any API call, it simply returns the [Mono](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html) publisher object. Nothing happens until the application subscribes or blocks on the result:

```java
Mono<Void> result = daprClient.publishEvent("mytopic", "my message");
```

To start execution and receive the result object synchronously(`void` or `Void` becomes an empty result), use `block()`. The code below shows how to execute the call and consume an empty response:
```java
Mono<Void> result = daprClient.publishEvent("mytopic", "my message");
result.block();
```

#### How to use a custom serializer

This SDK provides a basic serialization for request/response objects but also for state objects. Applications should provide their own serialization for production scenarios.

1. Implement the [DaprObjectSerializer](https://dapr.github.io/java-sdk/io/dapr/serializer/DaprObjectSerializer.html) interface. See [this class](sdk-actors/src/test/java/io/dapr/actors/runtime/JavaSerializer.java) as example.
2. Use your serializer class in the following scenarios:
    * When building a new instance of [DaprClient](https://dapr.github.io/java-sdk/io/dapr/client/DaprClient.html):
    ```java
    DaprClient client = (new DaprClientBuilder())
        .withObjectSerializer(new MyObjectSerializer()) // for request/response objects.
        .withStateSerializer(new MyStateSerializer()) // for state objects.
        .build();
    ```
    * When registering an Actor Type:
    ```java
    ActorRuntime.getInstance().registerActor(
      DemoActorImpl.class,
      new MyObjectSerializer(), // for request/response objects.
      new MyStateSerializer()); // for state objects.
    ```
    * When building a new instance of [ActorProxy](https://dapr.github.io/java-sdk/io/dapr/actors/client/ActorProxy.html) to invoke an Actor instance, use the same serializer as when registering the Actor Type:
    ```java
    ActorProxy actor = (new ActorProxyBuilder("DemoActor"))
        .withObjectSerializer(new MyObjectSerializer()) // for request/response objects.
        .build();
    ```


#### Debug Java application or Dapr's Java SDK

**In IntelliJ Community Edition, consider [debugging in IntelliJ](https://docs.dapr.io/developing-applications/ides/intellij/).**

**In Visual Studio Code, consider [debugging in Visual Studio Code](https://docs.dapr.io/developing-applications/ides/vscode-debugging/).**

If you have a Java application or an issue on this SDK that needs to be debugged, run Dapr using a dummy command and start the application from your IDE (IntelliJ, for example).
For Linux and MacOS:

```sh
dapr run --app-id testapp --app-port 3000 --dapr-http-port 3500 --dapr-grpc-port 5001 -- cat
```

For Windows:
```sh
dapr run --app-id testapp --app-port 3000 --dapr-http-port 3500 --dapr-grpc-port 5001 -- waitfor FOREVER
```

When running your Java application from IDE, make sure the following environment variables are set, so the Java SDK knows how to connect to Dapr's sidecar:
```
DAPR_HTTP_PORT=3500
DAPR_GRPC_PORT=5001
```

Now you can go to your IDE (like Eclipse, for example) and debug your Java application, using port `3500` to call Dapr while also listening to port `3000` to expose Dapr's callback endpoint.

Calls to Dapr's APIs on `http://127.0.0.1:3500/*` should work now and trigger breakpoints in your code.

#### Creating and publishing the artifacts to Nexus Repository
In case you need to publish Dapr's SDK to a private Nexus repo, run the command below from the project's root directory:

```sh
mvn package
mvn deploy:deploy-file -DgeneratePom=false -DrepositoryId=nexus -Durl=http://localhost:8081/repository/maven-releases -DpomFile=pom.xml -Dfile=target/dapr-sdk-0.3.0.jar
```

For more documentation reference:

https://maven.apache.org/plugins/maven-deploy-plugin

https://help.sonatype.com/repomanager3/user-interface/uploading-components

### Development

#### Maven Module version management
When releasing a new version of this SDK you must increase the version of all modules and pom files, so run the following commands:

```sh
mvn versions:set -DnewVersion="0.1.0-preview02"
mvn versions:commit
```

#### Update proto files

Change the properties below in [pom.xml](./pom.xml) to point to the desired reference URL in Git. Avoid pointing to master branch since it can change over time and create unpredictable behavior in the build.

```xml
<project>
  ...
  <properties>
    ...
    <dapr.proto.url>https://raw.githubusercontent.com/dapr/dapr/v0.4.0/pkg/proto/dapr/dapr.proto</dapr.proto.url>
    <dapr.client.proto.url>https://raw.githubusercontent.com/dapr/dapr/v0.4.0/pkg/proto/daprclient/daprclient.proto</dapr.client.proto.url>
    ...
  </properties>
  ...
</project>
```
