# Dapr Workflow Compensation Pattern Example

This example demonstrates how to implement the compensation pattern in Dapr Workflows using Java. The compensation pattern is used to "undo" or "roll back" previously completed steps if a later step fails.

## Example Overview

The example simulates a trip booking workflow that books a flight, hotel, and car. If any step fails, the workflow will automatically compensate (cancel) the previously completed bookings in reverse order.

- `BookTripWorkflow`: The main workflow that orchestrates the booking process
- `BookFlightActivity`/`CancelFlightActivity`: Activities for booking and canceling flights
- `BookHotelActivity`/`CancelHotelActivity`: Activities for booking and canceling hotels
- `BookCarActivity`/`CancelCarActivity`: Activities for booking and canceling cars
- `BookTripWorker`: Registers the workflow and activities with Dapr
- `BookTripClient`: Starts the workflow and waits for completion

## Running the Example

### Checking out the code

Clone this repository, if you don't already have it cloned:

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

### Compensation Example

When running the compensating workflow, you should see logs showing the booking process and compensation:

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
  - "Durable Task worker is connecting to sidecar at 127.0.0.1:50001."

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
  - "Error during compensation: The orchestrator is blocked and waiting for new inputs. This Throwable should never be caught by user code."
  - "Starting Activity: io.dapr.examples.workflows.compensation.CancelHotelActivity"
  - "Activity completed with result: Hotel canceled successfully"
  - "Starting Activity: io.dapr.examples.workflows.compensation.CancelFlightActivity"
  - "Activity completed with result: Flight canceled successfully"
background: true
sleep: 60
timeout_seconds: 60
-->

Execute the following script in order to run the BookTripWorker:
```sh
dapr run --app-id book-trip-worker --resources-path ./components/workflows --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.compensation.BookTripWorker
```

Once running, you can see the registered workflow and activities followed by the durabletask grpc worker starting:
```text
== APP == 2025-05-19 16:09:51,487 {HH:mm:ss.SSS} [main] INFO  i.d.w.runtime.WorkflowRuntimeBuilder - Registered Workflow: BookTripWorkflow
== APP == 2025-05-19 16:09:51,490 {HH:mm:ss.SSS} [main] INFO  i.d.w.runtime.WorkflowRuntimeBuilder - Registered Activity: BookFlightActivity
== APP == 2025-05-19 16:09:51,490 {HH:mm:ss.SSS} [main] INFO  i.d.w.runtime.WorkflowRuntimeBuilder - Registered Activity: CancelFlightActivity
== APP == 2025-05-19 16:09:51,490 {HH:mm:ss.SSS} [main] INFO  i.d.w.runtime.WorkflowRuntimeBuilder - Registered Activity: BookHotelActivity
== APP == 2025-05-19 16:09:51,490 {HH:mm:ss.SSS} [main] INFO  i.d.w.runtime.WorkflowRuntimeBuilder - Registered Activity: CancelHotelActivity
== APP == 2025-05-19 16:09:51,490 {HH:mm:ss.SSS} [main] INFO  i.d.w.runtime.WorkflowRuntimeBuilder - Registered Activity: BookCarActivity
== APP == 2025-05-19 16:09:51,491 {HH:mm:ss.SSS} [main] INFO  i.d.w.runtime.WorkflowRuntimeBuilder - Registered Activity: CancelCarActivity
== APP == 2025-05-19 16:09:51,619 {HH:mm:ss.SSS} [main] INFO  i.d.w.runtime.WorkflowRuntimeBuilder - Successfully built dapr workflow runtime
== APP == Start workflow runtime
== APP == May 19, 2025 4:09:51 PM io.dapr.durabletask.DurableTaskGrpcWorker startAndBlock
== APP == INFO: Durable Task worker is connecting to sidecar at 127.0.0.1:50001.
```

Once running, execute the following script to run the BookTripClient:
```sh
java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.workflows.compensation.BookTripClient
```
<!-- END_STEP -->

The client logs show the workflow being started, then completes with `RuntimeStatus: COMPLETED` and output showing `Workflow failed, compensation applied`, meaning that our workflow compensation applied because we forced an error in the book car activity:
```text
Started a new trip booking workflow with instance ID: aec453a6-8055-46dd-a976-f41a38d13558
Workflow instance with ID: aec453a6-8055-46dd-a976-f41a38d13558 completed with status: [Name: 'io.dapr.examples.workflows.compensation.BookTripWorkflow', ID: 'aec453a6-8055-46dd-a976-f41a38d13558', RuntimeStatus: COMPLETED, CreatedAt: 2025-05-19T20:51:28.714Z, LastUpdatedAt: 2025-05-19T20:51:34.876Z, Input: '', Output: '"Workflow failed, compensation applied"']
Workflow output: "Workflow failed, compensation applied"
```

The output demonstrates:
1. The workflow starts and successfully books a flight
2. Then successfully books a hotel
3. When attempting to book a car, it fails (intentionally)
4. The compensation logic triggers, canceling the hotel and flight in reverse order
5. The workflow completes with a status indicating the compensation was applied

## Key Points

1. Each successful booking step adds its compensation action to an ArrayList
2. If an error occurs, the list of compensations is reversed and executed in reverse order
3. The workflow ensures that all resources are properly cleaned up even if the process fails
4. Each activity simulates work with a short delay for demonstration purposes