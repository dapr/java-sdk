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
5. [Child-workflow Pattern](#child-workflow-pattern)
6. [Compensation Pattern](#compensation-pattern)
7. [Multi-App Pattern](#multi-app-pattern)

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
timeout_seconds: 20
background: true
-->
Execute the following script in order to run DemoChainWorker:
```sh
dapr run --app-id chainingworker --resources-path ./components/workflows --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.chain.DemoChainWorker 50001
```

Once running, the logs will start displaying the different steps: First, you can see workflow is starting:
```text
== APP == Start workflow runtime
== APP == Nov 07, 2023 11:03:07 AM com.microsoft.durabletask.DurableTaskGrpcWorker startAndBlock
== APP == INFO: Durable Task worker is connecting to sidecar at 127.0.0.1:50001.
```

<!-- END_STEP -->

<!-- STEP
name: Execute Chaining Pattern workflow
match_order: none
output_match_mode: substring
expected_stdout_lines:
  - 'completed with result: TOKYO, LONDON, SEATTLE'
timeout_seconds: 20
-->
Then, execute the following script in order to run DemoChainClient:
```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.chain.DemoChainClient 50001
dapr stop --app-id chainingworker
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
    logger.info("Starting Activity: {}", ctx.getName());

    String input = ctx.getInput(String.class);
    StringTokenizer tokenizer = new StringTokenizer(input);
    int result = tokenizer.countTokens();

    logger.info("Activity returned: {}.", result);
    logger.info("Activity finished");

    return result;
  }
}
```
<!-- STEP
name: Run FanInOut Pattern workflow
match_order: none
output_match_mode: substring
expected_stdout_lines:
  - 'Activity returned: 2.'
  - 'Activity returned: 9.'
  - 'Activity returned: 21.'
  - 'Activity returned: 17.'
  - 'Activity returned: 11.'
  - 'Workflow finished with result: 60'
timeout_seconds: 20
background: true
-->

Execute the following script in order to run DemoFanInOutWorker:
```sh
dapr run --app-id faninoutworker --resources-path ./components/workflows --dapr-grpc-port 50002 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.faninout.DemoFanInOutWorker 50002
```

<!-- END_STEP -->

<!-- STEP
name: Execute FanInOut Pattern workflow
match_order: none
output_match_mode: substring
expected_stdout_lines:
  - 'completed with result: 60'
timeout_seconds: 20
-->
Execute the following script in order to run DemoFanInOutClient:

```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.faninout.DemoFanInOutClient 50002
dapr stop --app-id faninoutworker
```
<!-- END_STEP -->

Now you can see the logs from worker:
```text
== APP == 2023-11-07 14:52:03,075 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting Workflow: io.dapr.examples.workflows.faninout.DemoFanInOutWorkflow
== APP == 2023-11-07 14:52:03,144 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Starting Activity: io.dapr.examples.workflows.faninout.CountWordsActivity
== APP == 2023-11-07 14:52:03,147 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity returned: 2.
== APP == 2023-11-07 14:52:03,148 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity finished
== APP == 2023-11-07 14:52:03,152 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Starting Activity: io.dapr.examples.workflows.faninout.CountWordsActivity
== APP == 2023-11-07 14:52:03,152 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity returned: 9.
== APP == 2023-11-07 14:52:03,152 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity finished
== APP == 2023-11-07 14:52:03,167 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Starting Activity: io.dapr.examples.workflows.faninout.CountWordsActivity
== APP == 2023-11-07 14:52:03,167 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity returned: 21.
== APP == 2023-11-07 14:52:03,167 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity finished
== APP == 2023-11-07 14:52:03,170 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Starting Activity: io.dapr.examples.workflows.faninout.CountWordsActivity
== APP == 2023-11-07 14:52:03,170 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity returned: 17.
== APP == 2023-11-07 14:52:03,170 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity finished
== APP == 2023-11-07 14:52:03,173 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Starting Activity: io.dapr.examples.workflows.faninout.CountWordsActivity
== APP == 2023-11-07 14:52:03,173 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.faninout.CountWordsActivity - Activity returned: 11.
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
```

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

### Child-workflow Pattern
The child-workflow pattern allows you to call a workflow from another workflow.

The `DemoWorkflow` class defines the workflow. It calls a child-workflow `DemoChildWorkflow` to do the work. See the code snippet below:
```java
public class DemoWorkflow extends Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());

      var childWorkflowInput = "Hello Dapr Workflow!";
      ctx.getLogger().info("calling childworkflow with input: " + childWorkflowInput);

      var childWorkflowOutput =
              ctx.callChildWorkflow(DemoChildWorkflow.class.getName(), childWorkflowInput, String.class).await();

      ctx.getLogger().info("childworkflow finished with: " + childWorkflowOutput);
    };
  }
}
```

The `DemoChildWorkflow` class defines the child-workflow. It call the activity to do the work and returns the result. See the code snippet below:
```java
public class DemoChildWorkflow extends Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting ChildWorkflow: " + ctx.getName());

      var childWorkflowInput = ctx.getInput(String.class);
      ctx.getLogger().info("ChildWorkflow received input: " + childWorkflowInput);

      ctx.getLogger().info("ChildWorkflow is calling Activity: " + ReverseActivity.class.getName());
      String result = ctx.callActivity(ReverseActivity.class.getName(), childWorkflowInput, String.class).await();

      ctx.getLogger().info("ChildWorkflow finished with: " + result);
      ctx.complete(result);
    };
  }
}
```

The `ReverseActivity` class defines the logics for a single activity, in this case, it reverses a string. See the code snippet below:
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
dapr run --app-id demoworkflowworker --resources-path ./components/workflows -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.childworkflow.DemoChildWorkflowWorker
```

```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.childworkflow.DemoChildWorkerflowClient
```

The log from worker:
```text
== APP == 2023-11-07 20:08:52,521 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting Workflow: io.dapr.examples.workflows.childworkflow.DemoWorkflow
== APP == 2023-11-07 20:08:52,523 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - calling childworkflow with input: Hello Dapr Workflow!
== APP == 2023-11-07 20:08:52,561 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting ChildWorkflow: io.dapr.examples.workflows.childworkflow.DemoChildWorkflow
== APP == 2023-11-07 20:08:52,566 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - ChildWorkflow received input: Hello Dapr Workflow!
== APP == 2023-11-07 20:08:52,566 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - ChildWorkflow is calling Activity: io.dapr.examples.workflows.childworkflow.ReverseActivity
== APP == 2023-11-07 20:08:52,576 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.childworkflow.ReverseActivity - Starting Activity: io.dapr.examples.workflows.childworkflow.ReverseActivity
== APP == 2023-11-07 20:08:52,577 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.childworkflow.ReverseActivity - Message Received from input: Hello Dapr Workflow!
== APP == 2023-11-07 20:08:52,577 {HH:mm:ss.SSS} [main] INFO  i.d.e.w.childworkflow.ReverseActivity - Sending message to output: !wolfkroW rpaD olleH
== APP == 2023-11-07 20:08:52,596 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - ChildWorkflow finished with: !wolfkroW rpaD olleH
== APP == 2023-11-07 20:08:52,611 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - childworkflow finished with: !wolfkroW rpaD olleH
```

The log from client:
```text
Started a new child-workflow model workflow with instance ID: c2fb9c83-435b-4b55-bdf1-833b39366cfb
workflow instance with ID: c2fb9c83-435b-4b55-bdf1-833b39366cfb completed with result: !wolfkroW rpaD olleH
```

### Compensation Pattern
The compensation pattern is used to "undo" or "roll back" previously completed steps if a later step fails. This pattern is particularly useful in scenarios where you need to ensure that all resources are properly cleaned up even if the process fails.

The example simulates a trip booking workflow that books a flight, hotel, and car. If any step fails, the workflow will automatically compensate (cancel) the previously completed bookings in reverse order.

The `BookTripWorkflow` class defines the workflow. It orchestrates the booking process and handles compensation if any step fails. See the code snippet below:
```java
public class BookTripWorkflow extends Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      List<String> compensations = new ArrayList<>();
      
      try {
        // Book flight
        String flightResult = ctx.callActivity(BookFlightActivity.class.getName(), String.class).await();
        ctx.getLogger().info("Flight booking completed: " + flightResult);
        compensations.add(CancelFlightActivity.class.getName());

        // Book hotel
        String hotelResult = ctx.callActivity(BookHotelActivity.class.getName(), String.class).await();
        ctx.getLogger().info("Hotel booking completed: " + hotelResult);
        compensations.add(CancelHotelActivity.class.getName());

        // Book car
        String carResult = ctx.callActivity(BookCarActivity.class.getName(), String.class).await();
        ctx.getLogger().info("Car booking completed: " + carResult);
        compensations.add(CancelCarActivity.class.getName());

      } catch (Exception e) {
        ctx.getLogger().info("******** executing compensation logic ********");
        // Execute compensations in reverse order
        Collections.reverse(compensations);
        for (String compensation : compensations) {
          try {
            ctx.callActivity(compensation, String.class).await();
          } catch (Exception ex) {
            ctx.getLogger().error("Error during compensation: " + ex.getMessage());
          }
        }
        ctx.complete("Workflow failed, compensation applied");
        return;
      }
      ctx.complete("All bookings completed successfully");
    };
  }
}
```

Each activity class (`BookFlightActivity`, `BookHotelActivity`, `BookCarActivity`) implements the booking logic, while their corresponding compensation activities (`CancelFlightActivity`, `CancelHotelActivity`, `CancelCarActivity`) implement the cancellation logic.

<!-- STEP
name: Run Compensation Pattern workflow worker
match_order: none
output_match_mode: substring
expected_stdout_lines:
  - "Registered Workflow: BookTripWorkflow"
  - "Registered Activity: BookFlightActivity"
  - "Registered Activity: CancelFlightActivity"
  - "Registered Activity: BookHotelActivity"
  - "Registered Activity: CancelHotelActivity"
  - "Registered Activity: BookCarActivity"
  - "Registered Activity: CancelCarActivity"
  - "Successfully built dapr workflow runtime"
  - "Start workflow runtime"
  - "Starting Workflow: io.dapr.examples.workflows.compensation.BookTripWorkflow"
  - "Starting Activity: io.dapr.examples.workflows.compensation.BookFlightActivity"
  - "Activity completed with result: Flight booked successfully"
  - "Flight booking completed: Flight booked successfully"
  - "Starting Activity: io.dapr.examples.workflows.compensation.BookHotelActivity"
  - "Simulating hotel booking process..."
  - "Activity completed with result: Hotel booked successfully"
  - "Hotel booking completed: Hotel booked successfully"
  - "Starting Activity: io.dapr.examples.workflows.compensation.BookCarActivity"
  - "Forcing Failure to trigger compensation for activity: io.dapr.examples.workflows.compensation.BookCarActivity"
  - "******** executing compensation logic ********"
  - "Activity failed: Task 'io.dapr.examples.workflows.compensation.BookCarActivity' (#2) failed with an unhandled exception: Failed to book car"
  - "Starting Activity: io.dapr.examples.workflows.compensation.CancelHotelActivity"
  - "Activity completed with result: Hotel canceled successfully"
  - "Starting Activity: io.dapr.examples.workflows.compensation.CancelFlightActivity"
  - "Activity completed with result: Flight canceled successfully"
background: true
timeout_seconds: 30
-->

Execute the following script in order to run the BookTripWorker:
```sh
dapr run --app-id book-trip-worker --resources-path ./components/workflows --dapr-grpc-port 50003 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.compensation.BookTripWorker 50003
```

<!-- END_STEP -->

<!-- STEP
name: Execute Compensation Pattern workflow
match_order: none
output_match_mode: substring
expected_stdout_lines:
  - 'Workflow failed, compensation applied'
timeout_seconds: 30
-->
Once running, execute the following script to run the BookTripClient:
```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.compensation.BookTripClient 50003
dapr stop --app-id book-trip-worker
```
<!-- END_STEP -->

The output demonstrates:
1. The workflow starts and successfully books a flight
2. Then successfully books a hotel
3. When attempting to book a car, it fails (intentionally)
4. The compensation logic triggers, canceling the hotel and flight in reverse order
5. The workflow completes with a status indicating the compensation was applied

Key Points:
1. Each successful booking step adds its compensation action to an ArrayList
2. If an error occurs, the list of compensations is reversed and executed in reverse order
3. The workflow ensures that all resources are properly cleaned up even if the process fails
4. Each activity simulates work with a short delay for demonstration purposes


### Multi-App Pattern

The multi-app pattern allows workflows to call activities that are hosted in different Dapr applications. This is useful for microservices architectures allowing multiple applications to host activities that can be orchestrated by Dapr Workflows.

The `MultiAppWorkflow` class defines the workflow. It demonstrates calling activities in different apps using the `appId` parameter in `WorkflowTaskOptions`. See the code snippet below:
```java
public class MultiAppWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
      return ctx -> {
          var logger = ctx.getLogger();
          logger.info("=== WORKFLOW STARTING ===");
          logger.info("Starting MultiAppWorkflow: {}", ctx.getName());
          logger.info("Workflow name: {}", ctx.getName());
          logger.info("Workflow instance ID: {}", ctx.getInstanceId());

          String input = ctx.getInput(String.class);
          logger.info("MultiAppWorkflow received input: {}", input);
          logger.info("Workflow input: {}", input);

          // Call an activity in another app by passing in an active appID to the WorkflowTaskOptions
          logger.info("Calling multi-app activity in 'app2'...");
          logger.info("About to call multi-app activity in app2...");
          String multiAppResult = ctx.callActivity(
                  App2TransformActivity.class.getName(),
                  input,
                  new WorkflowTaskOptions("app2"),
                  String.class
          ).await();

          // Call another activity in a different app
          logger.info("Calling multi-app activity in 'app3'...");
          logger.info("About to call multi-app activity in app3...");
          String finalResult = ctx.callActivity(
                  App3FinalizeActivity.class.getName(),
                  multiAppResult,
                  new WorkflowTaskOptions("app3"),
                  String.class
          ).await();
          logger.info("Final multi-app activity result: {}", finalResult);
          logger.info("Final multi-app activity result: {}", finalResult);

          logger.info("MultiAppWorkflow finished with: {}", finalResult);
          logger.info("=== WORKFLOW COMPLETING WITH: {} ===" , finalResult);
          ctx.complete(finalResult);
      };
  }
}

```

The `App2TransformActivity` class defines an activity in app2 that transforms the input string. See the code snippet below:
```java
public class App2TransformActivity implements WorkflowActivity {
  @Override
  public Object run(WorkflowActivityContext ctx) {
    var logger = ctx.getLogger();
    logger.info("=== App2: TransformActivity called ===");
    String input = ctx.getInput(String.class);
    logger.info("Input: {}", input);
    
    // Transform the input
    String result = input.toUpperCase() + " [TRANSFORMED BY APP2]";

    logger.info("Output: {}", result);
    return result;
  }
}
```

The `App3FinalizeActivity` class defines an activity in app3 that finalizes the processing. See the code snippet below:
```java
public class App3FinalizeActivity implements WorkflowActivity {
  @Override
  public Object run(WorkflowActivityContext ctx) {
    var logger = ctx.getLogger();
    logger.info("=== App3: FinalizeActivity called ===");
    String input = ctx.getInput(String.class);
    logger.info("Input: ", input);
    
    // Finalize the processing
    String result = input + " [FINALIZED BY APP3]";

    logger.info("Output: {}", result);
    return result;
  }
}
```

**Key Features:**
- **Cross-app activity calls**: Call activities in different Dapr applications specifying the appID in the WorkflowTaskOptions
- **WorkflowTaskOptions with appId**: Specify which app should handle the activity
- **Combined with retry policies**: Use app ID along with retry policies and handlers
- **Error handling**: Works the same as local activity calls

**Requirements:**
- Multiple Dapr applications running with different app IDs
- Activities registered in the target applications
- Proper Dapr workflow runtime configuration

**Important Limitations:**
- **Cross-app calls are currently supported for activities only**
- **Child workflow multi-app calls (suborchestration) are NOT supported**
- The app ID must match the Dapr application ID of the target service

**Running the Cross-App Example:**

This example requires running multiple Dapr applications simultaneously. You'll need to run the following commands in separate terminals:

1. **Start the main workflow worker (multiapp-worker):**
```sh
dapr run --app-id multiapp-worker --resources-path ./components/workflows --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.multiapp.MultiAppWorker
```

2. **Start app2 worker (handles App2TransformActivity):**
```sh
dapr run --app-id app2 --resources-path ./components/workflows --dapr-grpc-port 50002 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.multiapp.App2Worker
```

3. **Start app3 worker (handles App3FinalizeActivity):**
```sh
dapr run --app-id app3 --resources-path ./components/workflows --dapr-grpc-port 50003 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.multiapp.App3Worker
```

4. **Run the workflow client:**
```sh
java -Djava.util.logging.ConsoleHandler.level=FINE -Dio.dapr.durabletask.level=FINE -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.multiapp.MultiAppWorkflowClient "Hello World"
```

**Expected Output:**

The client will show:
```text
=== Starting Cross-App Workflow Client ===
Input: Hello World
Created DaprWorkflowClient successfully
Attempting to start new workflow...
Started a new multi-app workflow with instance ID: 001113f3-b9d9-438c-932a-a9a9b70b9460
Waiting for workflow completion...
Workflow instance with ID: 001113f3-b9d9-438c-932a-a9a9b70b9460 completed with result: HELLO WORLD [TRANSFORMED BY APP2] [FINALIZED BY APP3]
```

The workflow demonstrates:
1. The workflow starts in the main worker (multiapp-worker)
2. Calls an activity in 'app2' using multi-app functionality
3. Calls an activity in 'app3' using multi-app functionality
4. The workflow completes with the final result from all activities

This pattern is particularly useful for:
- Microservices architectures where activities are distributed across multiple services
- Multi-tenant applications where activities are isolated by app ID

### Suspend/Resume Pattern

Workflow instances can be suspended and resumed. This example shows how to use the suspend and resume commands.

For testing the suspend and resume operations we will use the same workflow definition used by the DemoExternalEventWorkflow.

Start the workflow and client using the following commands:


<!-- STEP
name: Run Suspend/Resume workflow 
match_order: none
output_match_mode: substring
expected_stdout_lines:
  - "Waiting for approval..."
  - "approval granted - do the approved action"
  - "Starting Activity: io.dapr.examples.workflows.externalevent.ApproveActivity"
  - "Running approval activity..."
  - "approval-activity finished"
background: true
timeout_seconds: 30
-->

```sh
dapr run --app-id suspendresumeworker --resources-path ./components/workflows --dapr-grpc-port 50004 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.suspendresume.DemoSuspendResumeWorker 50004
```

<!-- END_STEP -->

<!-- STEP
name: Execute Suspend/Resume workflow 
match_order: none
output_match_mode: substring
expected_stdout_lines:
  - "Suspending Workflow Instance"
  - "Workflow Instance Status: SUSPENDED"
  - "Let's resume the Workflow Instance before sending the external event"
  - "Workflow Instance Status: RUNNING"
  - "Now that the instance is RUNNING again, lets send the external event."
timeout_seconds: 30
-->
```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.suspendresume.DemoSuspendResumeClient 50004
dapr stop --app-id suspendresumeworker
```

<!-- END_STEP -->

The worker logs:
```text
== APP == 2023-11-07 16:01:23,279 {HH:mm:ss.SSS} [main] INFO  io.dapr.workflows.WorkflowContext - Starting Workflow: io.dapr.examples.workflows.suspendresume.DemoExternalEventWorkflow
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