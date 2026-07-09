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

package io.dapr.it.api;

import io.dapr.client.DaprClient;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ApiIT extends BaseContainerIT {

    private static final Logger logger = LoggerFactory.getLogger(ApiIT.class);
    private static final long SHUTDOWN_TIMEOUT_MS = 60_000;
    private static final long SIDECAR_WARMUP_MS = 3_000;

    private static DaprContainer dapr;

    @BeforeAll
    public static void init() throws Exception {
        dapr = daprBuilder("api-it");
        dapr.start();
        deferStop(dapr);
    }

    @Test
    public void testShutdownAPI() throws Exception {
        // TODO(artursouza): change this to wait for the sidecar to be healthy (new method needed in DaprClient).
        Thread.sleep(SIDECAR_WARMUP_MS);
        try (DaprClient client = newDaprClient(dapr)) {
            logger.info("Sending shutdown request.");
            client.shutdown().block();

            logger.info("Ensuring dapr has stopped.");
            long deadline = System.currentTimeMillis() + SHUTDOWN_TIMEOUT_MS;
            while (dapr.isRunning() && System.currentTimeMillis() < deadline) {
                Thread.sleep(100);
            }
            assertFalse(dapr.isRunning(), "Dapr container should have exited after client.shutdown()");
        }
    }
}
