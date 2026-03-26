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
limitations under the License.
*/

package io.dapr.it.testcontainers;

import org.testcontainers.containers.Network;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Scoped Docker networks for integration tests.
 *
 * <p>Creating one network per test class can exhaust Docker address pools in CI.
 * Creating one global network for all tests can cause alias/DNS contention.
 * This class provides a small set of reused networks to balance both concerns.</p>
 */
public final class TestContainerNetworks {

  private TestContainerNetworks() {
  }

  public static final Network ACTORS_NETWORK = Network.newNetwork();
  public static final Network STATE_NETWORK = Network.newNetwork();
  public static final Network DATA_NETWORK = Network.newNetwork();
  public static final Network PUBSUB_NETWORK = Network.newNetwork();
  public static final Network WORKFLOWS_NETWORK = Network.newNetwork();
  public static final Network GENERAL_NETWORK = Network.newNetwork();

  public static int allocateFreePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to allocate free port", e);
    }
  }
}
