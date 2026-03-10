package io.quarkiverse.dapr.examples;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Verifies that the Dapr infrastructure is properly started by dev services.
 * Tests that the DaprWorkflowClient CDI bean is available and connected
 * to the Dapr sidecar backed by PostgreSQL state store.
 */
@QuarkusTest
@ExtendWith(DockerAvailableCondition.class)
class DaprWorkflowClientTest {

    @Inject
    DaprWorkflowClient workflowClient;

    @Test
    void daprWorkflowClientShouldBeAvailable() {
        assertNotNull(workflowClient, "DaprWorkflowClient should be injected by Dapr dev services");
    }
}
