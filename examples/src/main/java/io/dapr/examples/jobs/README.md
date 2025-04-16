## Manage Dapr Jobs via the Jobs API

This example provides the different capabilities provided by Dapr Java SDK for Jobs. For further information about Job APIs please refer to [this link](https://docs.dapr.io/developing-applications/building-blocks/jobs/jobs-overview/)

### Using the Jobs API

The Java SDK exposes several methods for this -
* `client.scheduleJob(...)` for scheduling a job.
* `client.getJob(...)` for retrieving a scheduled job.
* `client.deleteJob(...)` for deleting a job.

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

Then get into the examples directory:

```sh
cd examples
```

### Initialize Dapr

Run `dapr init` to initialize Dapr in Self-Hosted Mode if it's not already initialized.

### Running the example

This example uses the Java SDK Dapr client in order to **Schedule and Get** Jobs.
`DemoJobsClient.java` is the example class demonstrating these features.
Kindly check [DaprPreviewClient.java](https://github.com/dapr/java-sdk/blob/master/sdk/src/main/java/io/dapr/client/DaprPreviewClient.java) for a detailed description of the supported APIs.

```java
public class DemoJobsClient {
  /**
   * The main method of this app to register and fetch jobs.
   */
  public static void main(String[] args) throws Exception {
    Map<Property<?>, String> overrides = Map.of(
        Properties.HTTP_PORT, "3500",
        Properties.GRPC_PORT, "51439"
    );

    try (DaprPreviewClient client = new DaprClientBuilder().withPropertyOverrides(overrides).buildPreviewClient()) {

      // Schedule a job.
      ScheduleJobRequest scheduleJobRequest = new ScheduleJobRequest("dapr-job-1",
          JobSchedule.fromString("* * * * * *")).setData("Hello World!".getBytes());
      client.scheduleJob(scheduleJobRequest).block();

      // Get a job.
      GetJobResponse getJobResponse = client.getJob(new GetJobRequest("dapr-job-1")).block();
    }
  }
}
```

Use the following command to run this example-

<!-- STEP
name: Run Demo Jobs Client example
expected_stdout_lines:
  - "== APP == Job Name: dapr-job-1"
  - "== APP == Job Payload: Hello World!"
background: true
output_match_mode: substring
sleep: 10
-->

```bash
dapr run --resources-path ./components/configuration --app-id myapp --app-port 8080 --dapr-http-port 3500 --dapr-grpc-port 51439  --log-level debug -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.jobs.DemoJobsSpringApplication
```

```bash
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.jobs.DemoJobsClient
```

<!-- END_STEP -->

### Sample output
```
== APP == Job Name: dapr-job-1
== APP == Job Payload: Hello World!
```
### Cleanup

To stop the app, run (or press CTRL+C):

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id myapp
```

<!-- END_STEP -->

