package io.dapr.it.api;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.actors.ActorReminderRecoveryIT;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ApiIT extends BaseIT {

    private static final Logger logger = LoggerFactory.getLogger(ApiIT.class);
    private static final int DEFAULT_TIMEOUT = 60000;

    /**
     * Parameters for this test.
     * Param #1: useGrpc.
     *
     * @return Collection of parameter tuples.
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{{false}, {true}});
    }

    @Parameterized.Parameter
    public boolean useGrpc;

    @Test
    public void testShutdownAPI() throws Exception {
        DaprRun run = startDaprApp(this.getClass().getSimpleName(), DEFAULT_TIMEOUT);

        if (this.useGrpc) {
            run.switchToGRPC();
        } else {
            run.switchToHTTP();
        }

        try (DaprClient client = new DaprClientBuilder().build()) {
            logger.info("Sending shutdown request.");
            client.shutdown().block();

            logger.info("Ensuring dapr has stopped.");
            run.checkRunState(DEFAULT_TIMEOUT, false);
        }
    }
}
