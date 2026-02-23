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

/**
 * Shared Docker network for integration tests.
 *
 * <p>Creating a network per test class can exhaust Docker address pools in CI.
 * Reusing one network per JVM avoids that failure mode while preserving
 * inter-container communication semantics needed by multi-container tests.</p>
 */
public final class TestContainerNetworks {

  private TestContainerNetworks() {
  }

  public static final Network SHARED_NETWORK = Network.newNetwork();
}
