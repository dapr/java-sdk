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

## Patterns

Those examples contain the following workflow patterns:
1. [Chaining Pattern](#chaining-pattern)
2. [Fan-out/Fan-in Pattern](#fan-outfan-in-pattern)
3. [Continue As New Pattern](#continue-as-new-pattern)
4. [External Event Pattern](#external-event-pattern)
5. [Sub-workflow Pattern](#sub-workflow-pattern)

### Chaining Pattern
In the chaining pattern, a sequence of activities executes in a specific order. 
In this pattern, the output of one activity is applied to the input of another activity. 
The chaining pattern is useful when you need to execute a sequence of activities in a specific order.

The first Java class is `DemoChainWorker`. Its job is to register an implementation of `DemoChainWorkflow` in Dapr's workflow runtime engine. In the `DemoChainWorker.java` file, you will find the `DemoChainWorker` class and the `main` method. See the code snippet below:
```java
public class DemoChainWorker {
  /**
   * The main method of this app.
   *
   * @param args The port the app will listen on.
   * @throws Exception An Exception.
   */
  public static void main(String[] args) throws Exception {
    // Register the Workflow with the builder.
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder().registerWorkflow(DemoChainWorkflow.class);
    builder.registerActivity(ToUpperCaseActivity.class);

    // Build and then start the workflow runtime pulling and executing tasks
    try (WorkflowRuntime runtime = builder.build()) {
      System.out.println("Start workflow runtime");
      runtime.start();
    }

    System.exit(0);
  }
}
```

The second Java class you want to look at is `DemoChainWorkflow`, it defines the workflow. In this example it chains the activites in order. See the code snippet below:
```java
public class DemoChainWorkflow extends Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());

      String result = "";
      result += ctx.callActivity(ToUpperCaseActivity.class.getName(), "Tokyo", String.class).await() + ", ";
      result += ctx.callActivity(ToUpperCaseActivity.class.getName(), "London", String.class).await() + ", ";
      result += ctx.callActivity(ToUpperCaseActivity.class.getName(), "Seattle", String.class).await();

      ctx.getLogger().info("Workflow finished with result: " + result);
      ctx.complete(result);
    };
  }
}
```

The next Java class you want to look at is `ToUpperCaseActivity`, it defines the logics for a single acitvity, in this case, it converts a string to upper case. See the code snippet below:
```java
public class ToUpperCaseActivity implements WorkflowActivity {

  @Override
  public String run(WorkflowActivityContext ctx) {
    Logger logger = LoggerFactory.getLogger(ToUpperCaseActivity.class);
    logger.info("Starting Chaining Activity: " + ctx.getName());

    var message = ctx.getInput(String.class);
    var newMessage = message.toUpperCase();

    logger.info("Message Received from input: " + message);
    logger.info("Sending message to output: " + newMessage);

    logger.info("Activity returned: " + newMessage);
    logger.info("Activity finished");

    return newMessage;
  }
}
```
<!-- STEP
name: Run Chaining Pattern workflow
match_order: none
output_match_mode: substring
expected_stdout_lines:
  - 'Starting Workflow: io.dapr.examples.workflows.chain.DemoChainWorkflow'
  - 'Starting Activity: io.dapr.examples.workflows.chain.ToUpperCaseActivity'
  - 'Message Received from input: Tokyo'
  - 'Sending message to output: TOKYO'
  - 'Starting Activity: io.dapr.examples.workflows.chain.ToUpperCaseActivity'
  - 'Message Received from input: London'
  - 'Sending message to output: LONDON'
  - 'Starting Activity: io.dapr.examples.workflows.chain.ToUpperCaseActivity'
  - 'Message Received from input: Seattle'
  - 'Sending message to output: SEATTLE'
  - 'Workflow finished with result: TOKYO, LONDON, SEATTLE'
background: true
sleep: 60
timeout_seconds: 60
-->
Execute the following script in order to run DemoChainWorker:
```sh
dapr run --app-id demoworkflowworker --resources-path ./components/workflows --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.chain.DemoChainWorker
```

Once running, the logs will start displaying the different steps: First, you can see workflow is starting:
```text
== APP == Start workflow runtime
== APP == Nov 07, 2023 11:03:07 AM com.microsoft.durabletask.DurableTaskGrpcWorker startAndBlock
== APP == INFO: Durable Task worker is connecting to sidecar at 127.0.0.1:50001.
```

Then, execute the following script in order to run DemoChainClient:
```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.chain.DemoChainClient
```
<!-- END_STEP -->



Now you can see the worker logs showing the acitvity is invoked in sequnce and the status of each activity:
```text
== APP == 2023-11-07 11:03:14,178 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting Workflow: io.dapr.examples.workflows.chain.DemoChainWorkflow
== APP == 2023-11-07 11:03:14,229 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.chain.ToUpperCaseActivity - Starting Activity: io.dapr.examples.workflows.chain.ToUpperCaseActivity
== APP == 2023-11-07 11:03:14,235 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.chain.ToUpperCaseActivity - Message Received from input: Tokyo
== APP == 2023-11-07 11:03:14,235 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.chain.ToUpperCaseActivity - Sending message to output: TOKYO
== APP == 2023-11-07 11:03:14,266 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.chain.ToUpperCaseActivity - Starting Activity: io.dapr.examples.workflows.chain.ToUpperCaseActivity
== APP == 2023-11-07 11:03:14,267 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.chain.ToUpperCaseActivity - Message Received from input: London
== APP == 2023-11-07 11:03:14,267 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.chain.ToUpperCaseActivity - Sending message to output: LONDON
== APP == 2023-11-07 11:03:14,282 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.chain.ToUpperCaseActivity - Starting Activity: io.dapr.examples.workflows.chain.ToUpperCaseActivity
== APP == 2023-11-07 11:03:14,282 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.chain.ToUpperCaseActivity - Message Received from input: Seattle
== APP == 2023-11-07 11:03:14,283 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.chain.ToUpperCaseActivity - Sending message to output: SEATTLE
== APP == 2023-11-07 11:03:14,298 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Workflow finished with result: TOKYO, LONDON, SEATTLE
```
and the client logs showing the workflow is started and finished with expected result:
```text
Started a new chaining model workflow with instance ID: 6e4fe69b-689b-4998-b095-d6b52c7d6328
workflow instance with ID: 6e4fe69b-689b-4998-b095-d6b52c7d6328 completed with result: TOKYO, LONDON, SEATTLE
```

### Fan-out/Fan-in Pattern

In the fan out/fan in pattern, you execute multiple activities in parallel and then wait for all activities to finish. Often, some aggregation work is done on the results that are returned from the activities.

The `DemoFanInOutWorkflow` class defines the workflow. In this example it executes the activities in parallel and then sums the results. See the code snippet below:
```java
public class DemoFanInOutWorkflow extends Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {

      ctx.getLogger().info("Starting Workflow: " + ctx.getName());


      // The input is a list of objects that need to be operated on.
      // In this example, inputs are expected to be strings.
      List<?> inputs = ctx.getInput(List.class);

      // Fan-out to multiple concurrent activity invocations, each of which does a word count.
      List<Task<Integer>> tasks = inputs.stream()
              .map(input -> ctx.callActivity(CountWordsActivity.class.getName(), input.toString(), Integer.class))
              .collect(Collectors.toList());

      // Fan-in to get the total word count from all the individual activity results.
      List<Integer> allWordCountResults = ctx.allOf(tasks).await();
      int totalWordCount = allWordCountResults.stream().mapToInt(Integer::intValue).sum();

      ctx.getLogger().info("Workflow finished with result: " + totalWordCount);
      // Save the final result as the orchestration output.
      ctx.complete(totalWordCount);
    };
  }
}
```

The `CountWordsActivity` class defines the logics for a single acitvity, in this case, it counts the words in a string. See the code snippet below:
```java
public class CountWordsActivity implements WorkflowActivity {
  @Override
  public Object run(WorkflowActivityContext ctx) {
    Logger logger = LoggerFactory.getLogger(ToUpperCaseActivity.class);
    logger.info("Starting Activity: " + ctx.getName());

    String input = ctx.getInput(String.class);
    StringTokenizer tokenizer = new StringTokenizer(input);
    int result = tokenizer.countTokens();

    logger.info("Activity returned: " + result);
    logger.info("Activity finished");

    return result;
  }
}
```
<!-- STEP
name: Run Chaining Pattern workflow
match_order: none
output_match_mode: substring
expected_stdout_lines:
  - 'Activity returned: 2'
  - 'Activity returned: 9'
  - 'Activity returned: 21'
  - 'Activity returned: 17'
  - 'Workflow finished with result: 60'
background: true
sleep: 60
timeout_seconds: 60
-->

Execute the following script in order to run DemoFanInOutWorker:
```sh
dapr run --app-id demoworkflowworker --resources-path ./components/workflows --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.faninout.DemoFanInOutWorker
```

Execute the following script in order to run DemoFanInOutClient:

```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.faninout.DemoFanInOutClient
```
<!-- END_STEP -->

Now you can see the logs from worker:
```text
== APP == 2023-11-07 14:52:03,075 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting Workflow: io.dapr.examples.workflows.faninout.DemoFanInOutWorkflow
== APP == 2023-11-07 14:52:03,144 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Starting Activity: io.dapr.examples.workflows.faninout.CountWordsActivity
== APP == 2023-11-07 14:52:03,147 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity returned: 2
== APP == 2023-11-07 14:52:03,148 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity finished
== APP == 2023-11-07 14:52:03,152 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Starting Activity: io.dapr.examples.workflows.faninout.CountWordsActivity
== APP == 2023-11-07 14:52:03,152 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity returned: 9
== APP == 2023-11-07 14:52:03,152 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity finished
== APP == 2023-11-07 14:52:03,167 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Starting Activity: io.dapr.examples.workflows.faninout.CountWordsActivity
== APP == 2023-11-07 14:52:03,167 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity returned: 21
== APP == 2023-11-07 14:52:03,167 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity finished
== APP == 2023-11-07 14:52:03,170 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Starting Activity: io.dapr.examples.workflows.faninout.CountWordsActivity
== APP == 2023-11-07 14:52:03,170 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity returned: 17
== APP == 2023-11-07 14:52:03,170 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity finished
== APP == 2023-11-07 14:52:03,173 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Starting Activity: io.dapr.examples.workflows.faninout.CountWordsActivity
== APP == 2023-11-07 14:52:03,173 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity returned: 11
== APP == 2023-11-07 14:52:03,174 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity finished
== APP == 2023-11-07 14:52:03,212 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Workflow finished with result: 60
```

and the client:
```text
Started a new fan out/fan in model model workflow with instance ID: 092c1928-b5dd-4576-9468-300bf6aed986
workflow instance with ID: 092c1928-b5dd-4576-9468-300bf6aed986 completed with result: 60
```

### Continue As New Pattern
`ContinueAsNew` API allows you to restart the workflow with a new input.

The `DemoContinueAsNewWorkflow` class defines the workflow. It simulates periodic cleanup work that happen every 10 seconds, after previous cleanup has finished. See the code snippet below:
```java
public class DemoContinueAsNewWorkflow extends Workflow {
  /*
  Compared with a CRON schedule, this periodic workflow example will never overlap.
  For example, a CRON schedule that executes a cleanup every hour will execute it at 1:00, 2:00, 3:00 etc.
  and could potentially run into overlap issues if the cleanup takes longer than an hour.
  In this example, however, if the cleanup takes 30 minutes, and we create a timer for 1 hour between cleanups,
  then it will be scheduled at 1:00, 2:30, 4:00, etc. and there is no chance of overlap.
   */
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());

      ctx.getLogger().info("call CleanUpActivity to do the clean up");
      ctx.callActivity(CleanUpActivity.class.getName()).await();
      ctx.getLogger().info("CleanUpActivity finished");

      ctx.getLogger().info("wait 10 seconds for next clean up");
      ctx.createTimer(Duration.ofSeconds(10)).await();

      // continue the workflow.
      ctx.continueAsNew(null);
    };
  }
}
```

The `CleanUpActivity` class defines the logics for a single acitvity, in this case, it simulates a clean up work. See the code snippet below:
```java
public class CleanUpActivity implements WorkflowActivity {
  @Override
  public Object run(WorkflowActivityContext ctx) {
    Logger logger = LoggerFactory.getLogger(CleanUpActivity.class);
    logger.info("Starting Activity: " + ctx.getName());

    logger.info("start clean up work, it may take few seconds to finish...");

    //Sleeping for 5 seconds to simulate long running operation
    try {
      TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return "clean up finish.";
  }
}
```

Once you start the workflow and client using the following commands:
```sh
dapr run --app-id demoworkflowworker --resources-path ./components/workflows -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.continueasnew.DemoContinueAsNewWorker
```
```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.continueasnew.DemoContinueAsNewClient
````

You will see the logs from worker showing the `CleanUpActivity` is invoked every 10 seconds after previous one is finished:
```text
== APP == 2023-11-07 14:44:42,004 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting Workflow: io.dapr.examples.workflows.continueasnew.DemoContinueAsNewWorkflow
== APP == 2023-11-07 14:44:42,004 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - call CleanUpActivity to do the clean up
== APP == 2023-11-07 14:44:42,009 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.c.CleanUpActivity - Starting Activity: io.dapr.examples.workflows.continueasnew.CleanUpActivity
== APP == 2023-11-07 14:44:42,009 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.c.CleanUpActivity - start clean up work, it may take few seconds to finish...
== APP == 2023-11-07 14:44:47,026 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting Workflow: io.dapr.examples.workflows.continueasnew.DemoContinueAsNewWorkflow
== APP == 2023-11-07 14:44:47,026 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - call CleanUpActivity to do the clean up
== APP == 2023-11-07 14:44:47,030 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - CleanUpActivity finished
== APP == 2023-11-07 14:44:47,030 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - wait 10 seconds for next clean up
== APP == 2023-11-07 14:44:47,033 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.c.CleanUpActivity - Starting Activity: io.dapr.examples.workflows.continueasnew.CleanUpActivity
== APP == 2023-11-07 14:44:47,033 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.c.CleanUpActivity - start clean up work, it may take few seconds to finish...
== APP == 2023-11-07 14:44:52,053 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - CleanUpActivity finished
== APP == 2023-11-07 14:44:52,053 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - wait 10 seconds for next clean up
== APP == 2023-11-07 14:44:57,006 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting Workflow: io.dapr.examples.workflows.continueasnew.DemoContinueAsNewWorkflow
== APP == 2023-11-07 14:44:57,006 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - call CleanUpActivity to do the clean up
== APP == 2023-11-07 14:44:57,012 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.c.CleanUpActivity - Starting Activity: io.dapr.examples.workflows.continueasnew.CleanUpActivity
== APP == 2023-11-07 14:44:57,012 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.c.CleanUpActivity - start clean up work, it may take few seconds to finish...
== APP == 2023-11-07 14:45:02,017 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting Workflow: io.dapr.examples.workflows.continueasnew.DemoContinueAsNewWorkflow
== APP == 2023-11-07 14:45:02,020 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - call CleanUpActivity to do the clean up
== APP == 2023-11-07 14:45:02,021 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - CleanUpActivity finished
== APP == 2023-11-07 14:45:02,021 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - wait 10 seconds for next clean up
...
```

and the client:
```text
Started a new continue-as-new model workflow with instance ID: c853fb93-f0e7-4ad7-ad41-385732386f94
```
It will continue to run until you stop the worker.

### External Event Pattern
In the external event pattern, a workflow is started by an external event. The workflow can then wait for other external events to occur before completing.

The `DemoExternalEventWorkflow` class defines the workflow. It waits for an external event `Approval` to run the corresponding activity. See the code snippet below:
```java
public class DemoExternalEventWorkflow extends Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());

      Boolean approved = ctx.waitForExternalEvent("Approval", boolean.class).await();
      if (approved) {
        ctx.getLogger().info("approval granted - do the approved action");
        ctx.callActivity(ApproveActivity.class.getName()).await();
        ctx.getLogger().info("approval-activity finished");
      } else {
        ctx.getLogger().info("approval denied - send a notification");
        ctx.callActivity(DenyActivity.class.getName()).await();
        ctx.getLogger().info("denied-activity finished");
      }
    };
  }
}
```

In the `DemoExternalEventClient` class we send out Approval event to tell our workflow to run the approved activity. 
```java
client.raiseEvent(instanceId, "Approval", true);
```

Start the workflow and client using the following commands:

ex
```sh
dapr run --app-id demoworkflowworker --resources-path ./components/workflows -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.externalevent.DemoExternalEventWorker
```

```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.externalevent.DemoExternalEventClient
```

The worker logs:
```text
== APP == 2023-11-07 16:01:23,279 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting Workflow: io.dapr.examples.workflows.externalevent.DemoExternalEventWorkflow
== APP == 2023-11-07 16:01:23,279 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Waiting for approval...
== APP == 2023-11-07 16:01:23,324 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - approval granted - do the approved action
== APP == 2023-11-07 16:01:23,348 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.e.ApproveActivity - Starting Activity: io.dapr.examples.workflows.externalevent.ApproveActivity
== APP == 2023-11-07 16:01:23,348 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.e.ApproveActivity - Running approval activity...
== APP == 2023-11-07 16:01:28,410 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - approval-activity finished
```

The client log:
```text
Started a new external-event model workflow with instance ID: 23410d96-1afe-4698-9fcd-c01c1e0db255
workflow instance with ID: 23410d96-1afe-4698-9fcd-c01c1e0db255 completed.
```

### Sub-workflow Pattern
The sub-workflow pattern allows you to call a workflow from another workflow.

The `DemoWorkflow` class defines the workflow. It calls a sub-workflow `DemoSubWorkflow` to do the work. See the code snippet below:
```java
public class DemoWorkflow extends Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());

      var subWorkflowInput = "Hello Dapr Workflow!";
      ctx.getLogger().info("calling subworkflow with input: " + subWorkflowInput);

      var subWorkflowOutput =
              ctx.callSubWorkflow(DemoSubWorkflow.class.getName(), subWorkflowInput, String.class).await();

      ctx.getLogger().info("subworkflow finished with: " + subWorkflowOutput);
    };
  }
}
```

The `DemoSubWorkflow` class defines the sub-workflow. It call the activity to do the work and returns the result. See the code snippet below:
```java
public class DemoSubWorkflow extends Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting SubWorkflow: " + ctx.getName());

      var subWorkflowInput = ctx.getInput(String.class);
      ctx.getLogger().info("SubWorkflow received input: " + subWorkflowInput);

      ctx.getLogger().info("SubWorkflow is calling Activity: " + ReverseActivity.class.getName());
      String result = ctx.callActivity(ReverseActivity.class.getName(), subWorkflowInput, String.class).await();

      ctx.getLogger().info("SubWorkflow finished with: " + result);
      ctx.complete(result);
    };
  }
}
```

The `ReverseActivity` class defines the logics for a single acitvity, in this case, it reverses a string. See the code snippet below:
```java
public class ReverseActivity implements WorkflowActivity {
  @Override
  public Object run(WorkflowActivityContext ctx) {
    Logger logger = LoggerFactory.getLogger(ReverseActivity.class);
    logger.info("Starting Activity: " + ctx.getName());

    var message = ctx.getInput(String.class);
    var newMessage = new StringBuilder(message).reverse().toString();

    logger.info("Message Received from input: " + message);
    logger.info("Sending message to output: " + newMessage);

    logger.info("Activity returned: " + newMessage);
    logger.info("Activity finished");

    return newMessage;
  }
}
```

Start the workflow and client using the following commands:

ex
```sh
dapr run --app-id demoworkflowworker --resources-path ./components/workflows -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.subworkflow.DemoSubWorkflowWorker
```

```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.subworkflow.DemoSubWorkerflowClient
```

The log from worker:
```text
== APP == 2023-11-07 20:08:52,521 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting Workflow: io.dapr.examples.workflows.subworkflow.DemoWorkflow
== APP == 2023-11-07 20:08:52,523 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - calling subworkflow with input: Hello Dapr Workflow!
== APP == 2023-11-07 20:08:52,561 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting SubWorkflow: io.dapr.examples.workflows.subworkflow.DemoSubWorkflow
== APP == 2023-11-07 20:08:52,566 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - SubWorkflow received input: Hello Dapr Workflow!
== APP == 2023-11-07 20:08:52,566 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - SubWorkflow is calling Activity: io.dapr.examples.workflows.subworkflow.ReverseActivity
== APP == 2023-11-07 20:08:52,576 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.subworkflow.ReverseActivity - Starting Activity: io.dapr.examples.workflows.subworkflow.ReverseActivity
== APP == 2023-11-07 20:08:52,577 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.subworkflow.ReverseActivity - Message Received from input: Hello Dapr Workflow!
== APP == 2023-11-07 20:08:52,577 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.subworkflow.ReverseActivity - Sending message to output: !wolfkroW rpaD olleH
== APP == 2023-11-07 20:08:52,596 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - SubWorkflow finished with: !wolfkroW rpaD olleH
== APP == 2023-11-07 20:08:52,611 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - subworkflow finished with: !wolfkroW rpaD olleH
```

The log from client:
```text
Started a new sub-workflow model workflow with instance ID: c2fb9c83-435b-4b55-bdf1-833b39366cfb
workflow instance with ID: c2fb9c83-435b-4b55-bdf1-833b39366cfb completed with result: !wolfkroW rpaD olleH
```