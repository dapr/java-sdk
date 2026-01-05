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

package io.dapr.testcontainers.internal;

import io.dapr.testcontainers.DaprContainer;

import java.io.IOException;
import java.net.ServerSocket;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;

/**
 * Factory for creating DaprContainer instances configured for Spring Boot integration tests.
 *
 * <p>This class handles the common setup required for bidirectional communication
 * between Spring Boot applications and the Dapr sidecar in test scenarios.</p>
 */
public final class DaprContainerFactory {

  private DaprContainerFactory() {
    // Utility class
  }

  /**
   * Creates a DaprContainer pre-configured for Spring Boot integration tests.
   * This factory method handles the common setup required for bidirectional
   * communication between Spring Boot and the Dapr sidecar:
   * <ul>
   *   <li>Allocates a free port for the Spring Boot application</li>
   *   <li>Configures the app channel address for container-to-host communication</li>
   * </ul>
   *
   * @param appName the Dapr application name
   * @return a pre-configured DaprContainer for Spring Boot tests
   */
  public static DaprContainer createForSpringBootTest(String appName) {
    int port = allocateFreePort();

    return new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName(appName)
        .withAppPort(port)
        .withAppChannelAddress("host.testcontainers.internal");
  }

  private static int allocateFreePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to allocate free port", e);
    }
  }
}
