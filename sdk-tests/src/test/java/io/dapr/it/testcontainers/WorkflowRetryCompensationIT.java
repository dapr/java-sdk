/*
 * Copyright 2025 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.it.testcontainers;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;
import io.dapr.workflows.WorkflowTaskRetryPolicy;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.dapr.workflows.client.WorkflowFailureDetails;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.config.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dapr.durabletask.TaskFailedException;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {
        TestDaprWorkflowsConfiguration.class,
        TestWorkflowsApplication.class
    }
)
@Testcontainers
@Tag("testcontainers")
public class WorkflowRetryCompensationIT {

    private static final Network DAPR_NETWORK = Network.newNetwork();

    @Container
    private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("workflow-retry-compensation-app")
        .withNetwork(DAPR_NETWORK)
        .withComponent(new Component("kvstore", "state.in-memory", "v1",
            Map.of("actorStateStore", "true")))
        .withComponent(new Component("pubsub", "pubsub.in-memory", "v1", Collections.emptyMap()))
        .withDaprLogLevel(DaprLogLevel.DEBUG)
        .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
        .withAppChannelAddress("host.testcontainers.internal");

    @DynamicPropertySource
    static void daprProperties(DynamicPropertyRegistry registry) {
        registry.add("dapr.http.endpoint", DAPR_CONTAINER::getHttpEndpoint);
        registry.add("dapr.grpc.endpoint", DAPR_CONTAINER::getGrpcEndpoint);
    }

    @Autowired
    private DaprWorkflowClient workflowClient;

    @Autowired
    private WorkflowRuntimeBuilder workflowRuntimeBuilder;

    private WorkflowRuntime runtime;

    @BeforeEach
    public void init() throws InterruptedException {
        // Reset attempt counts before each test
        BookFlightActivity.attemptCount = 0;
        BookHotelActivity.attemptCount = 0;
        BookCarActivity.attemptCount = 0;
        CancelFlightActivity.attemptCount = 0;
        CancelHotelActivity.attemptCount = 0;
        CancelCarActivity.attemptCount = 0;
        
        // Reset failure flags
        BookCarActivity.alwaysFail = false;
        CancelHotelActivity.shouldFail = false;
        
        // Wait for Dapr sidecar to be ready before starting workflow runtime
        Map<Property<?>, String> overrides = Map.of(
            Properties.HTTP_ENDPOINT, DAPR_CONTAINER.getHttpEndpoint(),
            Properties.GRPC_ENDPOINT, DAPR_CONTAINER.getGrpcEndpoint()
        );
        
        while (true) {
            try (DaprClient client = new DaprClientBuilder()
                    .withPropertyOverrides(overrides).build()) {
                client.waitForSidecar(10000).block(); // 10 seconds
                break;
            } catch (Exception e) {
                System.out.println("Sidecar not ready yet, retrying in 10 seconds...");
                Thread.sleep(1000);
            }
        }
        
        runtime = workflowRuntimeBuilder.build();
        System.out.println("Starting new workflow runtime for test");
        runtime.start(false);
    }

    public static class WorkflowInput {
        private boolean failCarBooking;
        private boolean failHotelCancellation;

        // Default constructor
        public WorkflowInput() {
            this.failCarBooking = false;
            this.failHotelCancellation = false;
        }

        public WorkflowInput(boolean failCarBooking, boolean failHotelCancellation) {
            this.failCarBooking = failCarBooking;
            this.failHotelCancellation = failHotelCancellation;
        }

        public boolean isFailCarBooking() {
            return failCarBooking;
        }

        public void setFailCarBooking(boolean failCarBooking) {
            this.failCarBooking = failCarBooking;
        }

        public boolean isFailHotelCancellation() {
            return failHotelCancellation;
        }

        public void setFailHotelCancellation(boolean failHotelCancellation) {
            this.failHotelCancellation = failHotelCancellation;
        }
    }

    public static class BookTripWorkflow implements Workflow {
        @Override
        public WorkflowStub create() {
            return ctx -> {
                ctx.getLogger().info("Starting BookTripWorkflow");
                List<String> compensations = new ArrayList<>();

                // Get workflow input
                WorkflowInput input = ctx.getInput(WorkflowInput.class);
                ctx.getLogger().info("Workflow input: failCarBooking={}, failHotelCancellation={}", 
                    input.isFailCarBooking(), input.isFailHotelCancellation());

                WorkflowTaskRetryPolicy retryPolicy = WorkflowTaskRetryPolicy.newBuilder()
                    .setMaxNumberOfAttempts(3)
                    .setFirstRetryInterval(Duration.ofSeconds(1))
                    .setRetryTimeout(Duration.ofSeconds(30))
                    .build();

                WorkflowTaskOptions options = new WorkflowTaskOptions(retryPolicy);

                try {
                    // Book flight (should succeed)
                    ctx.getLogger().info("Attempting to book flight...");
                    String flightResult = ctx.callActivity(
                        BookFlightActivity.class.getCanonicalName(), 
                        null, 
                        options,
                        String.class).await();
                    ctx.getLogger().info("Flight booking completed: {}", flightResult);
                    compensations.add("CancelFlight");
                    ctx.getLogger().info("Added flight cancellation to compensation list. Current compensations: {}", compensations);

                    // Book hotel (should succeed)
                    ctx.getLogger().info("Attempting to book hotel...");
                    String hotelResult = ctx.callActivity(
                        BookHotelActivity.class.getCanonicalName(), 
                        null, 
                        options,
                        String.class).await();
                    ctx.getLogger().info("Hotel booking completed: {}", hotelResult);
                    compensations.add("CancelHotel");
                    ctx.getLogger().info("Added hotel cancellation to compensation list. Current compensations: {}", compensations);

                    // Book car (should fail if configured)
                    ctx.getLogger().info("Attempting to book car with shouldFail={}...", input.isFailCarBooking());
                    String carResult = ctx.callActivity(
                        BookCarActivity.class.getCanonicalName(), 
                        null,
                        options,
                        String.class).await();
                    ctx.getLogger().info("Car booking completed: {}", carResult);
                    compensations.add("CancelCar");
                    ctx.getLogger().info("Added car cancellation to compensation list. Current compensations: {}", compensations);

                    String result = String.format("%s, %s, %s", flightResult, hotelResult, carResult);
                    ctx.getLogger().info("Trip booked successfully: {}", result);
                    ctx.complete(result);

                } catch (TaskFailedException e) {

                    ctx.getLogger().error("Activity failed: {}", e.getMessage());
                    ctx.getLogger().info("******** executing compensation logic ********");
                    ctx.getLogger().info("Compensation list before reversal: {}", compensations);
                    
                    // Execute compensations in reverse order
                    Collections.reverse(compensations);
                    ctx.getLogger().info("Compensation list after reversal: {}", compensations);
                    
                    for (String compensation : compensations) {
                        try {
                            ctx.getLogger().info("Executing compensation: {}", compensation);
                            switch (compensation) {
                                case "CancelCar":
                                    ctx.getLogger().info("Calling CancelCarActivity...");
                                    String carCancelResult = ctx.callActivity(
                                        CancelCarActivity.class.getCanonicalName(), 
                                        null, 
                                        options,
                                        String.class).await();
                                    ctx.getLogger().info("Car cancellation completed: {}", carCancelResult);
                                    break;

                                case "CancelHotel":
                                    ctx.getLogger().info("Calling CancelHotelActivity with shouldFail={}...", input.isFailHotelCancellation());
                                    String hotelCancelResult = ctx.callActivity(
                                        CancelHotelActivity.class.getCanonicalName(), 
                                        null,  // No input needed, use static flag
                                        options,
                                        String.class).await();
                                    ctx.getLogger().info("Hotel cancellation completed: {}", hotelCancelResult);
                                    break;

                                case "CancelFlight":
                                    ctx.getLogger().info("Calling CancelFlightActivity...");
                                    String flightCancelResult = ctx.callActivity(
                                        CancelFlightActivity.class.getCanonicalName(), 
                                        null, 
                                        options,
                                        String.class).await();
                                    ctx.getLogger().info("Flight cancellation completed: {}", flightCancelResult);
                                    break;
                            }
                        } catch (TaskFailedException ex) {
                            ctx.getLogger().error("Compensation activity {} failed: {}", compensation, ex.getMessage());
                        }
                    }
                    ctx.getLogger().info("All compensations executed. Completing workflow.");
                    ctx.complete("Workflow failed, compensation applied");
                }
            };
        }
    }

    public static class BookFlightActivity implements WorkflowActivity {
        private static final Logger logger = LoggerFactory.getLogger(BookFlightActivity.class);
        private static int attemptCount = 0;

        @Override
        public Object run(WorkflowActivityContext ctx) {
            attemptCount++;
            logger.info("BookFlightActivity attempt #{}", attemptCount);
            return "Flight booked successfully";
        }
    }

    public static class BookHotelActivity implements WorkflowActivity {
        private static final Logger logger = LoggerFactory.getLogger(BookHotelActivity.class);
        private static int attemptCount = 0;

        @Override
        public Object run(WorkflowActivityContext ctx) {
            attemptCount++;
            logger.info("BookHotelActivity attempt #{}", attemptCount);
            return "Hotel booked successfully";
        }
    }

    public static class BookCarActivity implements WorkflowActivity {
        private static final Logger logger = LoggerFactory.getLogger(BookCarActivity.class);
        private static int attemptCount = 0;
        private static boolean alwaysFail = false;

        @Override
        public Object run(WorkflowActivityContext ctx) {
            attemptCount++;
            logger.info("BookCarActivity attempt #{} (alwaysFail={})", attemptCount, alwaysFail);
            
            if (alwaysFail) {
                String errorMsg = String.format("Car booking failed on attempt %d (alwaysFail=true)", attemptCount);
                logger.info("BookCarActivity failing: {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            if (attemptCount < 3) {
                String errorMsg = String.format("Car booking failed on attempt %d", attemptCount);
                logger.info("BookCarActivity failing: {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            logger.info("BookCarActivity succeeding on attempt #{}", attemptCount);
            return "Car booked successfully";
        }
    }

    public static class CancelFlightActivity implements WorkflowActivity {
        private static final Logger logger = LoggerFactory.getLogger(CancelFlightActivity.class);
        private static int attemptCount = 0;

        @Override
        public Object run(WorkflowActivityContext ctx) {
            attemptCount++;
            logger.info("CancelFlightActivity attempt #{}", attemptCount);
            return "Flight cancelled successfully";
        }
    }

    public static class CancelHotelActivity implements WorkflowActivity {
        private static final Logger logger = LoggerFactory.getLogger(CancelHotelActivity.class);
        private static int attemptCount = 0;
        private static boolean shouldFail = false;

        @Override
        public Object run(WorkflowActivityContext ctx) {
            attemptCount++;
            logger.info("CancelHotelActivity attempt #{} (shouldFail={})", attemptCount, shouldFail);
            
            if (shouldFail) {
                String errorMsg = String.format("Hotel cancellation failed on attempt %d", attemptCount);
                logger.info("CancelHotelActivity failing: {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            logger.info("CancelHotelActivity succeeding on attempt #{}", attemptCount);
            return "Hotel cancelled successfully";
        }
    }

    public static class CancelCarActivity implements WorkflowActivity {
        private static final Logger logger = LoggerFactory.getLogger(CancelCarActivity.class);
        private static int attemptCount = 0;

        @Override
        public Object run(WorkflowActivityContext ctx) {
            attemptCount++;
            logger.info("CancelCarActivity attempt #{}", attemptCount);
            return "Car cancelled successfully";
        }
    }

    @Test
    public void testCompensationWithRetry() throws Exception {
        // Set car booking to fail to trigger compensation
        BookCarActivity.alwaysFail = true;
        
        // Create workflow input to make car booking fail
        WorkflowInput input = new WorkflowInput(true, false);
        System.out.println("Starting testCompensationWithRetry with input: " + input);
        
        // Start the workflow
        String instanceId = workflowClient.scheduleNewWorkflow(BookTripWorkflow.class, input);
        assertNotNull(instanceId, "Workflow instance ID should not be null");
        System.out.println("Started workflow with instance ID: " + instanceId);

        // Wait for workflow to start & complete
        workflowClient.waitForInstanceStart(instanceId, Duration.ofSeconds(30), false);
        WorkflowInstanceStatus status = workflowClient.waitForInstanceCompletion(instanceId, Duration.ofSeconds(120), true);
        assertNotNull(status, "Workflow status should not be null");
        
        // Verify the workflow completed with compensation
        assertEquals(WorkflowRuntimeStatus.COMPLETED, status.getRuntimeStatus(), 
            "Workflow should have completed with compensation");
        
        String result = status.readOutputAs(String.class);
        assertNotNull(result, "Workflow result should not be null");
        assertEquals("Workflow failed, compensation applied", result, 
            "Workflow should indicate compensation was applied");
        
        // Verify compensations were executed (car booking failed so no car cancellation)
        assertEquals(1, CancelFlightActivity.attemptCount, "Flight should be cancelled once");
        assertEquals(1, CancelHotelActivity.attemptCount, "Hotel should be cancelled once");
        assertEquals(0, CancelCarActivity.attemptCount, "Car should not be cancelled since booking failed");
    }

    @Test
    public void testCompensationWithRetryFailure() throws Exception {
        // Set car booking to fail to trigger compensation
        BookCarActivity.alwaysFail = true;
        // Set hotel cancellation to fail during compensation
        CancelHotelActivity.shouldFail = true;
        
        // Create workflow input to make hotel cancellation fail
        WorkflowInput input = new WorkflowInput(false, true);
        System.out.println("Starting testCompensationWithRetryFailure with input: " + input);
        
        // Start the workflow
        String instanceId = workflowClient.scheduleNewWorkflow(BookTripWorkflow.class, input);
        assertNotNull(instanceId, "Workflow instance ID should not be null");
        System.out.println("Started workflow with instance ID: " + instanceId);

        // Wait for workflow to start & complete
        workflowClient.waitForInstanceStart(instanceId, Duration.ofSeconds(30), false);
        WorkflowInstanceStatus status = workflowClient.waitForInstanceCompletion(instanceId, Duration.ofSeconds(120), true);
        assertNotNull(status, "Workflow status should not be null");
        assertEquals(WorkflowRuntimeStatus.COMPLETED, status.getRuntimeStatus(),
            "Workflow should have completed with compensation despite hotel cancellation failure");
        
        String result = status.readOutputAs(String.class);
        assertNotNull(result, "Workflow result should not be null");
        assertEquals("Workflow failed, compensation applied", result, 
            "Workflow should indicate compensation was applied");
        
        // Verify all compensations were attempted
        assertEquals(1, CancelFlightActivity.attemptCount, "Flight should be cancelled once");
        assertEquals(3, CancelHotelActivity.attemptCount, "Hotel cancellation should have retried twice before failing");
        assertEquals(0, CancelCarActivity.attemptCount, "Car should not be cancelled since booking failed");
    }

    @Test
    public void testRetrySuccessNoCompensation() throws Exception {
        // Let car booking retry and succeed (default behavior)
        BookCarActivity.alwaysFail = false;
        
        WorkflowInput input = new WorkflowInput(false, false);
        System.out.println("Starting testRetrySuccessNoCompensation with input: " + input);
        
        // Start the workflow
        String instanceId = workflowClient.scheduleNewWorkflow(BookTripWorkflow.class, input);
        assertNotNull(instanceId, "Workflow instance ID should not be null");
        System.out.println("Started workflow with instance ID: " + instanceId);

        // Wait for workflow to start & complete
        workflowClient.waitForInstanceStart(instanceId, Duration.ofSeconds(30), false);
        WorkflowInstanceStatus status = workflowClient.waitForInstanceCompletion(instanceId, Duration.ofSeconds(120), true);
        assertNotNull(status, "Workflow status should not be null");
        assertEquals(WorkflowRuntimeStatus.COMPLETED, status.getRuntimeStatus(), 
            "Workflow should have completed successfully");
        
        String result = status.readOutputAs(String.class);
        assertNotNull(result, "Workflow result should not be null");
        assertEquals("Flight booked successfully, Hotel booked successfully, Car booked successfully", result,
            "All bookings should have succeeded");
        
        // Assert all booking attempts & no compensations ran
        assertEquals(1, BookFlightActivity.attemptCount, "Flight should succeed on first attempt");
        assertEquals(1, BookHotelActivity.attemptCount, "Hotel should succeed on first attempt");  
        assertEquals(3, BookCarActivity.attemptCount, "Car should succeed on 3rd attempt after 2 retries");
        assertEquals(0, CancelFlightActivity.attemptCount, "No flight cancellation should occur");
        assertEquals(0, CancelHotelActivity.attemptCount, "No hotel cancellation should occur");
        assertEquals(0, CancelCarActivity.attemptCount, "No car cancellation should occur");
    }
}

