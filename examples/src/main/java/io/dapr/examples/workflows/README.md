# Dapr Workflow Sample

In this example, we'll use Dapr to test workflow features.

Visit [the Workflow documentation landing page](https://docs.dapr.io/developing-applications/building-blocks/workflow) for more information.

This example contains the follow classes:

* DemoWorkflow: An example of a Dapr Workflow.
* DemoWorkflowClient: This application will start workflows using Dapr.
* DemoWorkflowWorker: An application that registers a workflow to the Dapr workflow runtime engine. It also executes the workflow instance.
 
## Pre-requisites

* [Dapr CLI](https://docs.dapr.io/getting-started/install-dapr-cli/).
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

### Initialize Dapr

Run `dapr init` to initialize Dapr in Self-Hosted Mode if it's not already initialized.

### Running the demo Workflow worker

The first Java class to consider is `DemoWorkflowWorker`. Its job is to register an implementation of `DemoWorkflow` in Dapr's workflow runtime engine. In the `DemoWorkflowWorker.java` file, you will find the `DemoWorkflowWorker` class and the `main` method. See the code snippet below:

```java
public class DemoWorkflowWorker {

  public static void main(String[] args) throws Exception {
    // Register the Workflow with the runtime.
    WorkflowRuntime.getInstance().registerWorkflow(DemoWorkflow.class);
    System.out.println("Start workflow runtime");
    WorkflowRuntime.getInstance().startAndBlock();
    System.exit(0);
  }
}
```

This application uses `WorkflowRuntime.getInstance().registerWorkflow()` in order to register `DemoWorkflow` as a Workflow in the Dapr Workflow runtime.

The `WorkflowRuntime.getInstance().start()` method will build and start the engine within the Dapr workflow runtime.

The Workflow class `DemoWorkflow` extends from `Workflow` and is responsible for implementing the logic for creating and running workflows.

```java
public abstract class Workflow {
  public Workflow(){
  }

  /**
   * Executes the workflow logic.
   *
   * @return A WorkflowStub.
   */
  public abstract WorkflowStub create();

  /**
   * Executes the workflow logic.
   *
   * @param ctx provides access to methods for scheduling durable tasks and getting information about the current
   *            workflow instance.
   */
  public void run(WorkflowContext ctx) {
    this.create().run(ctx);
  }
}

```
The running logic of the `DemoWorkflow` is to run its create method. And the `create()` method can devide into six parts:
- wait for `TimedOutEvent`
- wait for `TestEvent`
- wait for external event and all tasks to finish
- wait for external event and any task to finish
- call activity `DemoWorkflowActivity` which is registered in `DemoWorkflowWorker`
- wait for `restartEvent` (if not received it will call child workflow and return)

The Activity class `DemoWorkflowActivity` is an implementation of `WorkflowActivity` interface, which contains a `run` method. This activity modify the input message, sleeps 5 seconds and then passes the new message as output.

```java
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DemoWorkflowActivity implements WorkflowActivity {

  @Override
  public DemoActivityOutput run(WorkflowActivityContext ctx) {
	...

    var message = ctx.getInput(DemoActivityInput.class).getMessage();
    var newMessage = message + " World!, from Activity";
	...

    try {
      TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

	...
    var output = new DemoActivityOutput(message, newMessage);
	...

    return output;
  }
}
```

The `DemoSubWorkflow` class is a child workflow that prints some metadata and its input message.

Now, execute the following script in order to run DemoWorkflowWorker:
```sh
dapr run --app-id demoworkflowworker --resources-path ./components/workflows --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.DemoWorkflowWorker
```

### Running the Workflow client

A DaprWorkflowClient can manage Dapr workflow instances. The `DemoWorkflowClient` starts instances of workflows that have been registered with Dapr, raises events the workflow are waiting and finally terminates the instances.

With the DemoWorkflowWorker running, use the follow command to start the workflow with the DemoWorkflowClient:

```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.DemoWorkflowClient
```
