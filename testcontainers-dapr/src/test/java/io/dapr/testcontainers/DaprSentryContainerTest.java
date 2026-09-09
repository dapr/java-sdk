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

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_SENTRY_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

public class DaprSentryContainerTest {

  @Test
  public void sentryDefaultsTest() {
    try (DaprSentryContainer sentry = new DaprSentryContainer(DAPR_SENTRY_IMAGE_TAG)) {
      sentry.configure();

      assertEquals(DaprSentryContainer.DEFAULT_SENTRY_PORT, sentry.getPort());
      assertEquals(DaprSentryContainer.DEFAULT_HEALTHZ_PORT, sentry.getHealthzPort());
      assertEquals(DaprSentryContainer.DEFAULT_TRUST_DOMAIN, sentry.getTrustDomain());
      assertEquals(DaprSentryContainer.DEFAULT_ISSUER_CREDENTIALS_PATH, sentry.getIssuerCredentialsPath());
      assertNull(sentry.getConfiguration());
      assertEquals("daprio/sentry", DaprSentryContainer.getDefaultImageName().getUnversionedPart());

      List<String> command = List.of(sentry.getCommandParts());
      assertEquals("./sentry", command.get(0));
      assertTrue(command.containsAll(List.of("--port", "50001", "--healthz-port", "8080", "--trust-domain", "localhost")));
      assertFalse(command.contains("--config"));
      assertTrue(sentry.getExposedPorts().containsAll(List.of(50001, 8080)));
    }
  }

  @Test
  public void sentryWithConfigurationTest() {
    MtlsConfigurationSettings mtls = new MtlsConfigurationSettings(true, "24h", "15m");
    Configuration configuration = new Configuration("daprsystem", null, null, mtls);

    try (DaprSentryContainer sentry = new DaprSentryContainer(DAPR_SENTRY_IMAGE_TAG)
        .withConfiguration(configuration)
        .withPort(50002)
        .withHealthzPort(8081)
        .withTrustDomain("cluster.local")
        .withIssuerCredentialsPath("/tmp/dapr-credentials")
        .withDaprLogLevel(DaprLogLevel.DEBUG)) {
      sentry.configure();

      assertNotNull(sentry.getConfiguration());
      assertEquals(50002, sentry.getPort());
      assertEquals(8081, sentry.getHealthzPort());
      assertEquals("cluster.local", sentry.getTrustDomain());
      assertEquals("/tmp/dapr-credentials", sentry.getIssuerCredentialsPath());

      List<String> command = List.of(sentry.getCommandParts());
      assertTrue(command.containsAll(List.of(
          "--port", "50002",
          "--healthz-port", "8081",
          "--trust-domain", "cluster.local",
          "--issuer-credentials", "/tmp/dapr-credentials",
          "--log-level", "DEBUG",
          "--config", "/dapr-resources/daprsystem.yaml")));
    }
  }

  @Test
  public void trustAnchorsRequireRunningContainerTest() {
    try (DaprSentryContainer sentry = new DaprSentryContainer(DAPR_SENTRY_IMAGE_TAG)) {
      assertThrows(IllegalStateException.class, sentry::getTrustAnchors);
    }
  }
}
