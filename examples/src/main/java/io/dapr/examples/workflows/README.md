# Dapr Workflow Sample

In this example, we'll use Dapr to test workflow features.

Visit [the Workflow documentation landing page](https://docs.dapr.io/developing-applications/building-blocks/workflow) for more information.

This example contains the follow classes:

* DemoWorkflow: An example of a Dapr Workflow.
* DemoWorkflowClient: This application will start workflows using Dapr.
* DemoWorkflowService: An application that registers a workflow to the Dapr workflow runtime engine. It also executes the workflow instance.
 
## Pre-requisites

* [Dapr and Dapr Cli](https://docs.dapr.io/getting-started/install-dapr/).
  * Run `dapr init`.
* Java JDK 11 (or greater):
    * [Microsoft JDK 11](https://docs.microsoft.com/en-us/java/openjdk/download#openjdk-11)
    * [Oracle JDK 11](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK11)
    * [OpenJDK 11](https://jdk.java.net/11/)
* [Apache Maven](https://maven.apache.org/install.html) version 3.x.

### Checking out the code

Clone this repository:

```sh
git clone https://github.com/dapr/java-sdk.git
cd java-sdk
```

Then build the Maven project:

```sh
# make sure you are in the `java-sdk` directory.
mvn install
```

Get into the `examples` directory.
```sh
cd examples
```

### Running the demo Workflow service

The first Java class to consider is `DemoWorkflowService`. Its job is to register an implementation of `DemoWorkflow` in the Dapr's workflow runtime engine. In `DemoWorkflowService.java` file, you will find the `DemoWorkflowService` class and the `main` method. See the code snippet below:

```java
public class DemoWorkflowService {

  public static void main(String[] args) throws Exception {
    // Register the Workflow with the runtime.
    WorkflowRuntime.getInstance().registerWorkflow(DemoWorkflow.class);
    WorkflowRuntime.getInstance().start();
  }
}
```

This application uses `WorkflowRuntime.getInstance().registerWorkflow()` in order to register `DemoWorkflow` as a Workflow in the Dapr Workflow runtime.

`WorkflowRuntime.getInstance().start()` method will build and start the engine within the Dapr workflow runtime.

Now, execute the following script in order to run DemoWorkflowService:
```sh
dapr run --app-id demoworkflowservice --resources-path ./components/workflows --dapr-grpc-port 4001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.DemoWorkflowService
```

### Running the Workflow client

The `DemoWorkflowClient` starts instances of workflows that have been registered with Dapr.

With the DemoWorkflowService running, use the follow command to start the workflow with the DemoWorkflowClient:

```sh
dapr run --app-id demoworkflowclient --resources-path ./components/workflows --dapr-grpc-port 4001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.DemoWorkflowClient
```
