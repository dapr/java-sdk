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

package io.dapr.testcontainers;

import org.junit.jupiter.api.Test;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_SCHEDULER_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

public class DaprSchedulerContainerTest {

  private static final String TRUST_ANCHORS = "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----\n";

  @Test
  public void tlsDisabledByDefaultTest() {
    try (DaprSchedulerContainer container = new DaprSchedulerContainer(DAPR_SCHEDULER_IMAGE_TAG)) {
      container.configure();

      assertFalse(container.isTlsEnabled());
      assertNull(container.getSentryAddress());
      assertNull(container.getTrustDomain());
      assertEquals(51005, container.getPort());

      List<String> command = List.of(container.getCommandParts());
      assertEquals("./scheduler", command.get(0));
      assertFalse(command.contains("--tls-enabled"));
      assertFalse(command.contains("--sentry-address"));
      assertFalse(command.contains("--trust-anchors-file"));
    }
  }

  @Test
  public void tlsEnabledTest() {
    try (DaprSchedulerContainer container = new DaprSchedulerContainer(DAPR_SCHEDULER_IMAGE_TAG)
        .withTlsEnabled(true)
        .withSentryAddress("sentry:50001")
        .withTrustDomain("cluster.local")
        .withTrustAnchors(TRUST_ANCHORS)) {
      container.configure();

      assertTrue(container.isTlsEnabled());
      assertEquals("sentry:50001", container.getSentryAddress());
      assertEquals("cluster.local", container.getTrustDomain());

      List<String> command = List.of(container.getCommandParts());
      assertTrue(command.contains("--tls-enabled"));
      assertTrue(command.containsAll(List.of("--sentry-address", "sentry:50001")));
      assertTrue(command.containsAll(List.of("--trust-domain", "cluster.local")));
      assertTrue(command.containsAll(List.of("--trust-anchors-file", "/var/run/secrets/dapr.io/tls/ca.crt")));
    }
  }

  @Test
  public void tlsEnabledWithoutOptionalSettingsTest() {
    try (DaprSchedulerContainer container = new DaprSchedulerContainer(DaprSchedulerContainer.getDefaultImageName().withTag(DaprContainerConstants.DAPR_VERSION))
        .withTlsEnabled(true)) {
      container.configure();

      List<String> command = List.of(container.getCommandParts());
      assertTrue(command.contains("--tls-enabled"));
      assertFalse(command.contains("--sentry-address"));
      assertFalse(command.contains("--trust-domain"));
      assertFalse(command.contains("--trust-anchors-file"));
    }
  }
}
