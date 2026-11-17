## Workflow Schedules

A workflow schedule starts a new instance of a workflow repeatedly, on a cron expression or an
`@every <duration>` interval. This example creates a schedule, triggers a run by hand, watches the
schedule until its repeats are used up, counts the runs and deletes the schedule.

### Using the Workflow Schedules API

The `DaprWorkflowClient` exposes these methods:
* `client.createWorkflowSchedule(...)` to create or overwrite a schedule.
* `client.getWorkflowSchedule(...)` to read a schedule with its status and last run.
* `client.listWorkflowSchedules(...)` to page through the app's schedules.
* `client.triggerWorkflowSchedule(...)` to start a run now.
* `client.deleteWorkflowSchedule(...)` to delete a schedule.
* `client.listInstanceIds(null, null, "<name>_")` to list the runs of a schedule.

## Pre-requisites

* Dapr 1.20 or later (dapr master until 1.20.0-rc.1 is released).
* [Dapr CLI](https://docs.dapr.io/getting-started/install-dapr-cli/).
* Java JDK 17 (or greater):
    * [Microsoft JDK 17](https://docs.microsoft.com/en-us/java/openjdk/download#openjdk-17)
    * [Oracle JDK 17](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK17)
    * [OpenJDK 17](https://jdk.java.net/17/)
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

Then get into the examples directory:

```sh
cd examples
```

### Initialize Dapr

Run `dapr init` to initialize Dapr in Self-Hosted Mode if it's not already initialized.

### Running the example

<!-- STEP
name: Run Workflow Schedule Worker
match_order: none
output_match_mode: substring
expected_stdout_lines:
  - "Start workflow runtime"
background: true
sleep: 20
timeout_seconds: 45
-->

Run the worker:

```sh
dapr run --app-id scheduleworker --resources-path ./components/workflows --dapr-grpc-port 50007 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.schedule.DemoScheduleWorker 50007
```

<!-- END_STEP -->

<!-- STEP
name: Run Workflow Schedule Client
match_order: none
output_match_mode: substring
expected_stdout_lines:
  - "Created schedule 'daily-report'"
  - "Triggered run"
  - "Schedule state: COMPLETED"
  - "Runs:"
  - "Deleted schedule 'daily-report'"
timeout_seconds: 90
-->

In a separate terminal, run the client. It connects to the worker's sidecar on
gRPC port 50007, so every run of the schedule executes on the worker:

```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.schedule.DemoScheduleClient 50007
dapr stop --app-id scheduleworker
```

<!-- END_STEP -->

The client creates `daily-report` with three repeats, triggers one run by hand and then polls the
schedule every two seconds. The triggered run counts towards the repeats, so two scheduled runs
follow it and the schedule completes. The output shows the schedule state and run count on each
poll, the number of run instances and the deletion.
