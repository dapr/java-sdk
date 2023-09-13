---
type: docs
title: "How to: Author and manage Dapr Workflow in the Java SDK"
linkTitle: "How to: Author and manage workflows"
weight: 20000
description: How to get up and running with workflows using the Dapr Java SDK
---

{{% alert title="Note" color="primary" %}}
Dapr Workflow is currently in alpha.
{{% /alert %}}

Let’s create a Dapr workflow and invoke it using the console. With the [provided workflow example](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples/workflows), you will:

- Execute the workflow instance using the [Java workflow worker](https://github.com/dapr/java-sdk/blob/master/examples/src/main/java/io/dapr/examples/workflows/DemoWorkflowWorker.java)
- Utilize the Java workflow client and API calls to [start and terminate workflow instances](https://github.com/dapr/java-sdk/blob/master/examples/src/main/java/io/dapr/examples/workflows/DemoWorkflowClient.java)

This example uses the default configuration from `dapr init` in [self-hosted mode](https://github.com/dapr/cli#install-dapr-on-your-local-machine-self-hosted).

## Prerequisites

- [Dapr CLI and initialized environment](https://docs.dapr.io/getting-started).
- Java JDK 11 (or greater):
  - [Oracle JDK](https://www.oracle.com/java/technologies/downloads), or
  - OpenJDK
- [Apache Maven](https://maven.apache.org/install.html), version 3.x.
<!-- IGNORE_LINKS -->
- [Docker Desktop](https://www.docker.com/products/docker-desktop)
<!-- END_IGNORE -->
- Verify you're using the latest proto bindings

## Set up the environment

Clone the Java SDK repo and navigate into it.

```bash
git clone https://github.com/dapr/java-sdk.git
cd java-sdk
```

Run the following command to install the requirements for running this workflow sample with the Dapr Java SDK.

```bash
mvn install
```

From the Java SDK root directory, navigate to the Dapr Workflow example.

```bash
cd examples
```

## Run the `DemoWorkflowWorker`

The `DemoWorkflowWorker` registers an implementation of `DemoWorkflow` in the Dapr Workflow runtime engine. In the following excerpt from the [`DemoWorkflowWorker.java` file](https://github.com/dapr/java-sdk/blob/master/examples/src/main/java/io/dapr/examples/workflows/DemoWorkflowWorker.java), notice the `DemoWorkflowWorker` class and the main method.

```java
public class DemoWorkflowWorker {

  public static void main(String[] args) throws Exception {
    // Register the Workflow with the builder.
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder().registerWorkflow(DemoWorkflow.class);

    // Build and then start the workflow runtime pulling and executing tasks
    try (WorkflowRuntime runtime = builder.build()) {
      System.out.println("Start workflow runtime");
      runtime.start();
    }

    System.exit(0);
  }
}
```

In the code above:
- `WorkflowRuntime.getInstance().registerWorkflow()` registers `DemoWorkflow` as a workflow in the Dapr Workflow runtime.
- `runtime.start();` builds and starts the engine within the Dapr Workflow runtime.

In the terminal, execute the following command to kick off the `DemoWorkflowWorker`:

```sh
dapr run --app-id demoworkflowworker --resources-path ./components/workflows --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.DemoWorkflowWorker
```

**Expected output**

```
You're up and running! Both Dapr and your app logs will appear here.

...

== APP == Start workflow runtime
== APP == Sep 13, 2023 9:02:03 AM com.microsoft.durabletask.DurableTaskGrpcWorker startAndBlock
== APP == INFO: Durable Task worker is connecting to sidecar at 127.0.0.1:50001.
```

## Run the `DemoWorkflowClient

Now that the workflow worker is ready to go, you can start workflow instances registered with Dapr using the `DemoWorkflowClient`. In the following excerpt from the [`DemoWorkflowClient.java` file](https://github.com/dapr/java-sdk/blob/master/examples/src/main/java/io/dapr/examples/workflows/DemoWorkflowClient.java), notice the `DemoWorkflowClient` class and the main method that starts the workflow instances.

```java
public class DemoWorkflowClient {

  public static void main(String[] args) throws InterruptedException {
    DaprWorkflowClient client = new DaprWorkflowClient();
    
    // Start the workflow instances
    try (client) {
      System.out.println("*****");
      String instanceId = client.scheduleNewWorkflow(DemoWorkflow.class);
      System.out.printf("Started new workflow instance with random ID: %s%n", instanceId);

      System.out.println("Sleep and allow this workflow instance to timeout...");
      TimeUnit.SECONDS.sleep(10);

      System.out.println("*****");
      String instanceToTerminateId = "terminateMe";
      client.scheduleNewWorkflow(DemoWorkflow.class, null, instanceToTerminateId);
      System.out.printf("Started new workflow instance with specified ID: %s%n", instanceToTerminateId);

      TimeUnit.SECONDS.sleep(5);
      System.out.println("Terminate this workflow instance manually before the timeout is reached");
      client.terminateWorkflow(instanceToTerminateId, null);
      System.out.println("*****");
    }

    System.out.println("Exiting DemoWorkflowClient.");
    System.exit(0);
  }
}
```

In a second terminal window, start the workflow by running the following command:

```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.DemoWorkflowClient
```

**Expected output**

```
*******
Started new workflow instance with random ID: 0b4cc0d5-413a-4c1c-816a-a71fa24740d4
*******
**GetInstanceMetadata:Running Workflow**
Result: [Name: 'io.dapr.examples.workflows.DemoWorkflow', ID: '0b4cc0d5-413a-4c1c-816a-a71fa24740d4', RuntimeStatus: RUNNING, CreatedAt: 2023-09-13T13:02:30.547Z, LastUpdatedAt: 2023-09-13T13:02:30.699Z, Input: '"input data"', Output: '']
*******
**WaitForInstanceStart**
Result: [Name: 'io.dapr.examples.workflows.DemoWorkflow', ID: '0b4cc0d5-413a-4c1c-816a-a71fa24740d4', RuntimeStatus: RUNNING, CreatedAt: 2023-09-13T13:02:30.547Z, LastUpdatedAt: 2023-09-13T13:02:30.699Z, Input: '"input data"', Output: '']
*******
**SendExternalMessage**
*******
** Registering parallel Events to be captured by allOf(t1,t2,t3) **
Events raised for workflow with instanceId: 0b4cc0d5-413a-4c1c-816a-a71fa24740d4
*******
** Registering Event to be captured by anyOf(t1,t2,t3) **
Event raised for workflow with instanceId: 0b4cc0d5-413a-4c1c-816a-a71fa24740d4
*******
**WaitForInstanceCompletion**
Result: [Name: 'io.dapr.examples.workflows.DemoWorkflow', ID: '0b4cc0d5-413a-4c1c-816a-a71fa24740d4', RuntimeStatus: FAILED, CreatedAt: 2023-09-13T13:02:30.547Z, LastUpdatedAt: 2023-09-13T13:02:55.054Z, Input: '"input data"', Output: '']
*******
**purgeInstance**
purgeResult: true
*******
**raiseEvent**
Started new workflow instance with random ID: 7707d141-ebd0-4e54-816e-703cb7a52747
Event raised for workflow with instanceId: 7707d141-ebd0-4e54-816e-703cb7a52747
*******
Started new workflow instance with specified ID: terminateMe
Terminate this workflow instance manually before the timeout is reached
*******
Started new  workflow instance with ID: restarting
Sleeping 30 seconds to restart the workflow
**SendExternalMessage: RestartEvent**
Sleeping 30 seconds to terminate the eternal workflow
Exiting DemoWorkflowClient.
```

## What happened?

1. When you ran `dapr run`, the workflow worker registered the workflow (`DemoWorkflow`) and its actvities to the Dapr Workflow engine. 
1. When you ran `java`, the workflow client started the workflow instance with the following activities. You can follow along with the output in the terminal where you ran `dapr run`.
   1. The workflow is started, raises three parallel tasks, and waits for them to complete.
   1. The workflow client calls the activity and sends the "Hello Activity" message to the console.
   1. The workflow times out and is purged.
   1. The workflow client starts a new workflow instance with a random ID, uses another workflow instance called `terminateMe` to terminate it, and restarts it with the workflow called `restarting`.
   1. The worfklow client is then exited.

## Next steps
- [Learn more about Dapr workflow]({{< ref workflow >}})
- [Workflow API reference]({{< ref workflow_api.md >}})