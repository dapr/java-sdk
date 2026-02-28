package io.dapr.it.api;

import io.dapr.client.DaprClient;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Testcontainers
@Tag("testcontainers")
public class ApiIT {

    private static final Logger logger = LoggerFactory.getLogger(ApiIT.class);
    private static final int DEFAULT_TIMEOUT = 60000;

    @Container
    private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("api-it");

    @Test
    public void testShutdownAPI() throws Exception {
        try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
            client.waitForSidecar(10000).block();
            logger.info("Sending shutdown request.");
            client.shutdown().block();

            logger.info("Ensuring dapr has stopped.");
            long start = System.currentTimeMillis();
            while (DAPR_CONTAINER.isRunning() && System.currentTimeMillis() - start < DEFAULT_TIMEOUT) {
                Thread.sleep(100);
            }
            assertFalse(DAPR_CONTAINER.isRunning(), "Dapr sidecar is expected to stop after shutdown API");
        }
    }
}
