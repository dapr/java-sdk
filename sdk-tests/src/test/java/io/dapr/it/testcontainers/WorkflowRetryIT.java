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
import java.util.Collections;
import java.util.Map;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {
        TestDaprWorkflowsConfiguration.class,
        TestWorkflowsApplication.class
    }
)
@Testcontainers
@Tag("testcontainers")
public class WorkflowRetryIT {

    private static final Network DAPR_NETWORK = Network.newNetwork();

    @Container
    private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("workflow-retry-app")
        .withNetwork(DAPR_NETWORK)
        .withComponent(new Component("kvstore", "state.in-memory", "v1",
            Map.of("actorStateStore", "true")))
        .withComponent(new Component("pubsub", "pubsub.in-memory", "v1", Collections.emptyMap()))
        .withDaprLogLevel(DaprLogLevel.DEBUG)
        .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
        .withAppChannelAddress("host.testcontainers.internal");

    /**
     * Expose the Dapr ports to the host.
     *
     * @param registry the dynamic property registry
     */
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
        RetryTestActivity.attemptCount = 0;
        RetryTestActivity.alwaysFail = false;
        
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
        System.out.println("Start workflow runtime");
        runtime.start(false);
    }

    public static class RetryTestWorkflowImpl implements Workflow {
        @Override
        public WorkflowStub create() {
            return ctx -> {
                ctx.getLogger().info("Starting RetryTestWorkflowImpl");

                WorkflowTaskRetryPolicy retryPolicy = WorkflowTaskRetryPolicy.newBuilder()
                    .setMaxNumberOfAttempts(3)
                    .setFirstRetryInterval(Duration.ofSeconds(1))
                    .setRetryTimeout(Duration.ofSeconds(30))
                    .build();

                WorkflowTaskOptions options = new WorkflowTaskOptions(retryPolicy);

                try {
                    // Call the test activity with retry policy
                    String result = ctx.callActivity(
                        RetryTestActivity.class.getCanonicalName(),
                        null,
                        options,
                        String.class).await();

                    ctx.getLogger().info("Activity completed with result: {}", result);
                    ctx.complete(result);
                } catch (Exception ex) {
                    ctx.getLogger().error("Workflow caught exception: {}", ex.getMessage());
                    throw ex;
                }
            };
        }
    }

    public static class RetryTestActivity implements WorkflowActivity {
        private static final Logger logger = LoggerFactory.getLogger(RetryTestActivity.class);
        private static int attemptCount = 0;
        private static boolean alwaysFail = false;

        @Override
        public Object run(WorkflowActivityContext ctx) {
            attemptCount++;
            logger.info("RetryTestActivity attempt #{}", attemptCount);
            
            if (alwaysFail || attemptCount < 3) {
                String errorMsg = "Simulated failure on attempt " + attemptCount;
                logger.info("RetryTestActivity failing on attempt #{}: {}", attemptCount, errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            String result = "Activity succeeded after " + attemptCount + " attempts";
            logger.info("RetryTestActivity succeeding on attempt #{} with result: {}", attemptCount, result);
            return result;
        }
    }

    @Test
    public void testWorkflowRetry() throws Exception {
        // Start the workflow
        String instanceId = workflowClient.scheduleNewWorkflow(RetryTestWorkflowImpl.class);
        assertNotNull(instanceId, "Workflow instance ID should not be null");
        
        // Wait for workflow to start & complete
        workflowClient.waitForInstanceStart(instanceId, Duration.ofSeconds(30), false);
        WorkflowInstanceStatus status = workflowClient.waitForInstanceCompletion(instanceId, Duration.ofSeconds(60), true);
        assertNotNull(status, "Workflow status should not be null");
        assertEquals(WorkflowRuntimeStatus.COMPLETED, status.getRuntimeStatus(), 
            "Workflow should have completed successfully");
        
        String result = status.readOutputAs(String.class);
        assertNotNull(result, "Workflow result should not be null");
        assertEquals("Activity succeeded after 3 attempts", result, 
            "Activity should have succeeded after 3 attempts");
        
        assertEquals(3, RetryTestActivity.attemptCount, "Activity should have been attempted 3 times");
    }

    @Test
    public void testWorkflowRetryWithFailure() throws Exception {
        // Set activity to always fail
        RetryTestActivity.alwaysFail = true;

        // Start the workflow
        String instanceId = workflowClient.scheduleNewWorkflow(RetryTestWorkflowImpl.class);
        assertNotNull(instanceId, "Workflow instance ID should not be null");

        // Wait for workflow to start & complete
        workflowClient.waitForInstanceStart(instanceId, Duration.ofSeconds(30), false);
        WorkflowInstanceStatus status = workflowClient.waitForInstanceCompletion(instanceId, Duration.ofSeconds(60), true);
        assertNotNull(status, "Workflow status should not be null");
        
        assertEquals(WorkflowRuntimeStatus.FAILED, status.getRuntimeStatus(), 
            "Workflow should have failed after retries");
        WorkflowFailureDetails failure = status.getFailureDetails();
        assertNotNull(failure, "Failure details should not be null");
        String errorMessage = failure.getErrorMessage();
        System.out.println("Error message: " + errorMessage);
        assertTrue(errorMessage.contains("Simulated failure on attempt 3"), 
            "Error should indicate failure on final attempt. Actual error: " + errorMessage);
        
        assertEquals(3, RetryTestActivity.attemptCount, "Activity should have failed after 3 attempts");
    }
}
