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

package io.dapr.it.containers;

import io.dapr.client.DaprClient;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Minimal smoke test that exercises BaseContainerIT's helpers end-to-end.
 * Boots a no-app DaprContainer with no components and verifies that we can
 * build a DaprClient against it and invoke a metadata call.
 */
class BaseContainerITSmokeTest extends BaseContainerIT {

  private static DaprContainer dapr;

  @BeforeAll
  static void init() {
    dapr = daprBuilder("smoke-test");
    dapr.start();
    deferStop(dapr);
  }

  @Test
  void canBuildAndUseDaprClient() throws Exception {
    try (DaprClient client = newDaprClient(dapr)) {
      // waitForSidecar is a cheap healthcheck — it's fine if it returns immediately.
      client.waitForSidecar(5000).block();
      assertNotNull(client);
    }
  }
}
