## Dapr SDK for Java

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
  <distributionManagement>
   ...
    <!-- BEGIN: Dapr's repositories -->
    <snapshotRepository>
      <id>ossrh</id>
      <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </snapshotRepository>
    <repository>
      <id>ossrh</id>
      <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
    </repository>
    <!-- END: Dapr's repositories -->
   ...
  </distributionManagement>
  ...
  <dependencyManagement>
      <dependencies>
        ...
         <!-- Dapr's core SDK with all features, except Actors. -->
        <dependency>
          <groupId>io.dapr</groupId>
          <artifactId>dapr-sdk</artifactId>
          <version>0.2.0-SNAPSHOT</version>
        </dependency>
        <!-- Dapr's SDK for Actors (optional). -->
        <dependency>
          <groupId>io.dapr</groupId>
          <artifactId>dapr-sdk</artifactId>
          <version>0.2.0-SNAPSHOT</version>
        </dependency>
        ...
      </dependencies>
  </dependencyManagement>
</project>
```


#### Running the examples
Clone this repository including the submodules:

```sh
git clone https://github.com/dapr/java-sdk.git
```

Then head over to build the [Maven](https://maven.apache.org/install.html) (Apache Maven version 3.x) project:

```sh
# make sure you are in the `java-sdk` directory.
mvn install
```

Try the following examples to learn more about Dapr's Java SDK:
* [Invoking a Http service](./examples/src/main/java/io/dapr/examples/invoke/http)
* [Invoking a Grpc service](./examples/src/main/java/io/dapr/examples/invoke/grpc)
* [State management](./examples/src/main/java/io/dapr/examples/state)
* [PubSub with subscriber over Http](./examples/src/main/java/io/dapr/examples/pubsub/http)
* [Binding with input over Http](./examples/src/main/java/io/dapr/examples/bindings/http)
* [Actors over Http](./examples/src/main/java/io/dapr/examples/actors/http)

#### Debug Java application or Dapr's Java SDK

If you have a Java application or an issue on this SDK that needs to be debugged, run Dapr using a dummy command and start the application from your IDE (IntelliJ, for example).
For Linux and MacOS:

```sh
dapr run --app-id testapp --app-port 3000 --port 3500 --grpc-port 5001 -- cat
```

For Windows:
```sh
dapr run --app-id testapp --app-port 3000 --port 3500 --grpc-port 5001 -- waitfor FOREVER
```

When running your Java application from IDE, make sure the following environment variables are set, so the Java SDK knows how to connect to Dapr's sidecar:
```
DAPR_HTTP_PORT=3500
DAPR_GRPC_PORT=5001
```

Now you can go to your IDE (like IntelliJ, for example) and debug your Java application, using port `3500` to call Dapr while also listening to port `3000` to expose Dapr's callback endpoint.

Calls to Dapr's APIs on `http://localhost:3500/*` should work now and trigger breakpoints in your code.

**If your application needs to subscribe to topics or register Actors in Dapr, for example, then start debugging your app first and run dapr with dummy command last.**

**If using Visual Studio Code, also consider [this solution as well](https://github.com/dapr/docs/tree/master/howto/vscode-debugging-daprd).**

#### Creating and publishing the artifacts to Nexus Repository
In case you need to publish Dapr's SDK to a private Nexus repo, run the command below from the project's root directory:

```sh
mvn package
mvn deploy:deploy-file -DgeneratePom=false -DrepositoryId=nexus -Durl=http://localhost:8081/repository/maven-releases -DpomFile=pom.xml -Dfile=target/dapr-sdk-0.2.0.jar
```

For more documentation reference :

https://maven.apache.org/plugins/maven-deploy-plugin

https://help.sonatype.com/repomanager3/user-interface/uploading-components

### Development

#### Maven Module version management
When releasing a new version of this SDK you must increase the version of all modules and pom files, so run the following commands:

```sh
mvn versions:set -DnewVersion="0.1.0-preview02"
mvn versions:commit
```