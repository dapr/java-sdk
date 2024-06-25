package io.dapr.it.api;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiIT extends BaseIT {

    private static final Logger logger = LoggerFactory.getLogger(ApiIT.class);
    private static final int DEFAULT_TIMEOUT = 60000;

    @Test
    public void testShutdownAPI() throws Exception {
        DaprRun run = startDaprApp(this.getClass().getSimpleName(), DEFAULT_TIMEOUT);

        // TODO(artursouza): change this to wait for the sidecar to be healthy (new method needed in DaprClient).
        Thread.sleep(3000);
        try (DaprClient client = new DaprClientBuilder().build()) {
            logger.info("Sending shutdown request.");
            client.shutdown().block();

            logger.info("Ensuring dapr has stopped.");
            run.checkRunState(DEFAULT_TIMEOUT, false);
        }
    }
}
