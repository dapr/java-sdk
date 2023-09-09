# Dapr Workflow Sample

In this example, we'll use Dapr to test workflow features.

Visit [the Workflow documentation landing page](https://docs.dapr.io/developing-applications/building-blocks/workflow) for more information.

This example contains the two parts :

1. WorkflowConsoleApp

  It utilizes the workflow SDK as well as the workflow management API for starting workflows instances. The main WorkflowConsoleApp.java file contains the main setup of the app, including the registration of the workflow and workflow activities. The workflow definition is found in the workflows package and the workflow activity definitions are found in the activities package. All the models used by workflow activities are in models package.

2. WorkflowClient

  It scheduales an instance of the OrderProcessingWorkflow (defined in the console package), starts it, and waits for the workflow result and output. 

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

### Running the demo Workflow worker

The first Java class to consider is `WorkflowConsoleApp`. Its job is to register an implementation of `OrderProcessingWorkflow` in the Dapr's workflow runtime engine. In `WorkflowConsoleApp.java` file, you will find the `WorkflowConsoleApp` class and the `main` method. See the code snippet below:

```java
public class WorkflowConsoleApp {
  public static void main(String[] args) throws Exception {
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder().registerWorkflow(OrderProcessingWorkflow.class);
    builder.registerActivity(NotifyActivity.class);
    builder.registerActivity(ProcessPaymentActivity.class);
    builder.registerActivity(RequestApprovalActivity.class);
    builder.registerActivity(ReserveInventoryActivity.class);
    builder.registerActivity(UpdateInventoryActivity.class);

    // Build and then start the workflow runtime pulling and executing tasks
    try (WorkflowRuntime runtime = builder.build()) {
      System.out.println("Start workflow runtime");
      runtime.start();
    }

    System.exit(0);
  }
}
```

This application uses WorkflowRuntimeBuilder class to build up Dapr workflow runtime. `registerWorkflow()` method is used to register `OrderProcessingWorkflow` as a Workflow in the Dapr Workflow runtime, and `registerActivity()` method is used to register all the activities in `OrderProcessingWorkflow`.

`runtime.start()` method will start the engine within the Dapr workflow runtime.

Now, execute the following script in order to run WorkflowConsoleApp:
```sh
dapr run --app-id WorkflowConsoleApp --resources-path ./components/workflows --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.console.WorkflowConsoleApp
```

### Running the Workflow client

The `WorkflowClient` starts instances of OrderProcessingWorkflow that had been registered with Dapr.

With the WorkflowConsoleApp running, use the follow command to start the workflow with the WorkflowClient:

```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.client.WorkflowClient
```
