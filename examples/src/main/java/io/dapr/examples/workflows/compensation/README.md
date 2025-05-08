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

1. Make sure you have Dapr installed and initialized:
```bash
dapr init
```

2. Start the workflow worker:

```bash
dapr run --app-id book-trip-worker --resources-path ./components/workflows --dapr-grpc-port 50001 -- java -jar target/dapr-sdk-examples-1.15.0-SNAPSHOT-exec.jar io.dapr.examples.workflows.compensation.BookTripWorker
```

3. In a separate terminal, run the client:

```bash
java -jar target/dapr-sdk-examples-1.15.0-SNAPSHOT-exec.jar io.dapr.examples.workflows.compensation.BookTripClient
```

## Expected Output

When running the compensating workflow, you should see logs showing the booking process:
```
//TODO: markdown
```

## Key Points

1. Each successful booking step pushes its compensation action onto a stack
2. If an error occurs, compensations are executed in reverse order
3. The workflow ensures that all resources are properly cleaned up even if the process fails
4. Each activity simulates work with a short delay for demonstration purposes 