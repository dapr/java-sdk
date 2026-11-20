/*
 * Copyright 2026 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dapr.it.testcontainers.api;

import io.dapr.client.DaprClient;
import io.dapr.it.testcontainers.support.DaprTestEnvironment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Integration tests for the sidecar-only Dapr API. */
@Tag("testcontainers")
public class ApiIT {

  private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(60);
  private static final DaprTestEnvironment ENVIRONMENT = DaprTestEnvironment.withoutApplication("api-it");

  @BeforeAll
  public static void init() {
    ENVIRONMENT.start();
  }

  @AfterAll
  public static void tearDown() {
    ENVIRONMENT.stop();
  }

  @Test
  public void testShutdownAPI() throws Exception {
    try (DaprClient client = ENVIRONMENT.clientBuilder().build()) {
      client.shutdown().block(SHUTDOWN_TIMEOUT);

      long deadline = System.nanoTime() + SHUTDOWN_TIMEOUT.toNanos();
      while (ENVIRONMENT.isSidecarRunning() && System.nanoTime() < deadline) {
        Thread.sleep(100);
      }
      assertFalse(ENVIRONMENT.isSidecarRunning(), "Dapr container should have exited after client.shutdown()");
    }
  }
}
