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

Execute the following command to run `DemoWorkflowWorker`:

```sh
dapr run --app-id demoworkflowworker --resources-path ./components/workflows --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.DemoWorkflowWorker
```

**Expected output**

```
todo
```

## Run the `DemoWorkflowClient

Now that the workflow worker is running, you can start workflow instances registered with Dapr using the `DemoWorkflowClient`. In the following excerpt from the [`DemoWorkflowClient.java` file](https://github.com/dapr/java-sdk/blob/master/examples/src/main/java/io/dapr/examples/workflows/DemoWorkflowClient.java), notice the `DemoWorkflowClient` class and the main method that starts the workflow instances.

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

Start the workflow by running the following command:

```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.DemoWorkflowClient
```

**Expected output:**

```
todo
```

## What happened?

1. When you ran `dapr run`, the workflow worker registered the workflow (`DemoWorkflow`) and its actvities to the Dapr Workflow engine. 
1. When you ran `java`, the workflow client started the workflow instances.


## Next steps
- [Learn more about Dapr workflow]({{< ref workflow >}})
- [Workflow API reference]({{< ref workflow_api.md >}})