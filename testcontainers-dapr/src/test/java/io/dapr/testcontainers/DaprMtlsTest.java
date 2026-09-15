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
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static io.dapr.testcontainers.DaprContainerConstants.DAPR_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DaprMtlsTest {

  @Test
  public void mtlsDisabledByDefaultTest() {
    try (DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG).withAppName("dapr-app")) {
      assertFalse(dapr.isMtlsEnabled());
    }

    MtlsConfigurationSettings mtls = new MtlsConfigurationSettings(false, "24h", "15m");
    try (DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withConfiguration(new Configuration("my-config", null, null, mtls))) {
      assertFalse(dapr.isMtlsEnabled());
      dapr.configure();

      assertNull(dapr.getSentryContainer());
      assertFalse(List.of(dapr.getCommandParts()).contains("--enable-mtls"));
    }
  }

  @Test
  public void sentrySettingsTest() {
    try (DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withConfiguration(new Configuration("my-config", null, null))) {
      assertFalse(dapr.isMtlsEnabled());
      assertEquals("sentry", dapr.getSentryService());
      assertEquals("scheduler", dapr.getSchedulerService());
      assertEquals(DaprContainerConstants.DAPR_SENTRY_IMAGE_TAG,
          dapr.getSentryDockerImageName().asCanonicalNameString());
    }

    try (DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withSentryService("my-sentry")
        .withSentryImage("daprio/sentry:" + DAPR_VERSION)
        .withReusableSentry(true)) {
      assertEquals("my-sentry", dapr.getSentryService());
      assertEquals("daprio/sentry:" + DAPR_VERSION, dapr.getSentryDockerImageName().asCanonicalNameString());
    }

    try (DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withSentryImage(DockerImageName.parse("custom/sentry:" + DAPR_VERSION)
            .asCompatibleSubstituteFor("daprio/sentry:" + DAPR_VERSION))) {
      assertEquals("custom/sentry:" + DAPR_VERSION, dapr.getSentryDockerImageName().asCanonicalNameString());
    }
  }

  @Test
  public void mtlsEnabledStartsSentryAndSecuresControlPlaneTest() throws IOException, InterruptedException {
    MtlsConfigurationSettings mtls = new MtlsConfigurationSettings(true, "24h", "15m");
    Configuration configuration = new Configuration("daprsystem", null, null, mtls);

    try (DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-mtls-app")
        .withConfiguration(configuration)
        .withDaprLogLevel(DaprLogLevel.DEBUG)) {
      assertTrue(dapr.isMtlsEnabled());

      dapr.start();

      DaprSentryContainer sentry = dapr.getSentryContainer();
      assertNotNull(sentry);
      assertTrue(sentry.isRunning());
      assertEquals(configuration, sentry.getConfiguration());

      String trustAnchors = sentry.getTrustAnchors();
      assertTrue(trustAnchors.startsWith("-----BEGIN CERTIFICATE-----"));
      assertEquals(trustAnchors, dapr.getEnvMap().get("DAPR_TRUST_ANCHORS"));

      List<String> command = List.of(dapr.getCommandParts());
      assertTrue(command.contains("--enable-mtls"));
      assertTrue(command.containsAll(List.of("--sentry-address", "sentry:50001")));
      assertTrue(command.containsAll(List.of("--control-plane-trust-domain", "localhost")));

      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(dapr.getHttpEndpoint() + "/v1.0/metadata"))
          .GET()
          .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      assertEquals(200, response.statusCode());
      assertTrue(response.body().contains("dapr-mtls-app"));
    }
  }

  @Test
  public void mtlsWithCustomSentryContainerTest() {
    MtlsConfigurationSettings mtls = new MtlsConfigurationSettings(
        true, "24h", "15m", "my-sentry:50001", "cluster.local");
    Configuration configuration = new Configuration("daprsystem", null, null, mtls);

    try (Network network = Network.newNetwork();
        DaprSentryContainer sentry = new DaprSentryContainer(DaprContainerConstants.DAPR_SENTRY_IMAGE_TAG)
            .withNetwork(network)
            .withNetworkAliases("my-sentry")
            .withConfiguration(configuration)
            .withTrustDomain("cluster.local");
        DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withNetwork(network)
            .withAppName("dapr-mtls-app")
            .withConfiguration(configuration)
            .withSentryContainer(sentry)) {
      dapr.start();

      assertTrue(sentry.isRunning());
      assertEquals(sentry, dapr.getSentryContainer());

      List<String> command = List.of(dapr.getCommandParts());
      assertTrue(command.containsAll(List.of("--sentry-address", "my-sentry:50001")));
      assertTrue(command.containsAll(List.of("--control-plane-trust-domain", "cluster.local")));
    }
  }
}
