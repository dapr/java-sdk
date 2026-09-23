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

package io.dapr.testcontainers.converter;

import io.dapr.testcontainers.ApiLoggingConfigurationSettings;
import io.dapr.testcontainers.AppHttpPipeline;
import io.dapr.testcontainers.ComponentsConfigurationSettings;
import io.dapr.testcontainers.Configuration;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.HttpMetricsConfigurationSettings;
import io.dapr.testcontainers.ListEntry;
import io.dapr.testcontainers.LoggingConfigurationSettings;
import io.dapr.testcontainers.MetricsConfigurationSettings;
import io.dapr.testcontainers.MetricsLabel;
import io.dapr.testcontainers.MetricsRule;
import io.dapr.testcontainers.MtlsConfigurationSettings;
import io.dapr.testcontainers.MtlsTokenValidator;
import io.dapr.testcontainers.NameResolutionConfigurationSettings;
import io.dapr.testcontainers.OtelTracingConfigurationSettings;
import io.dapr.testcontainers.TracingConfigurationSettings;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ConfigurationYamlConverterTest {
  private final Yaml MAPPER = YamlMapperFactory.create();
  private final ConfigurationYamlConverter converter = new ConfigurationYamlConverter(MAPPER);

  @Test
  public void testConfigurationToYaml() {
    OtelTracingConfigurationSettings otel = new OtelTracingConfigurationSettings(
        "localhost:4317",
        false,
        "grpc"
    );
    TracingConfigurationSettings tracing = new TracingConfigurationSettings(
        "1",
        true,
        otel,
        null
    );

    
    List<ListEntry> handlers = new ArrayList<>();
    handlers.add(new ListEntry("alias", "middleware.http.routeralias"));

    AppHttpPipeline appHttpPipeline = new AppHttpPipeline(handlers);

    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withConfiguration(new Configuration("my-config", tracing, appHttpPipeline))
        .withAppChannelAddress("host.testcontainers.internal");

    Configuration configuration = dapr.getConfiguration();
    assertNotNull(configuration);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  tracing:\n"
        + "    samplingRate: '1'\n"
        + "    stdout: true\n"
        + "    otel:\n"
        + "      endpointAddress: localhost:4317\n"
        + "      isSecure: false\n"
        + "      protocol: grpc\n"
        + "  appHttpPipeline:\n"
        + "    handlers:\n"
        + "    - name: alias\n"
        + "      type: middleware.http.routeralias\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMtlsToYaml() {
    Map<String, String> jwksOptions = new LinkedHashMap<>();
    jwksOptions.put("minRefreshInterval", "2m");
    jwksOptions.put("requestTimeout", "1m");
    jwksOptions.put("source", "https://localhost:1234/");

    List<MtlsTokenValidator> tokenValidators = new ArrayList<>();
    tokenValidators.add(MtlsTokenValidator.jwks(jwksOptions));

    MtlsConfigurationSettings mtls = new MtlsConfigurationSettings(
        true,
        "24h",
        "15m",
        "localhost:50001",
        "cluster.local",
        tokenValidators
    );

    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withConfiguration(new Configuration("my-config", null, null, mtls))
        .withAppChannelAddress("host.testcontainers.internal");

    Configuration configuration = dapr.getConfiguration();
    assertNotNull(configuration);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  mtls:\n"
        + "    enabled: true\n"
        + "    workloadCertTTL: 24h\n"
        + "    allowedClockSkew: 15m\n"
        + "    sentryAddress: localhost:50001\n"
        + "    controlPlaneTrustDomain: cluster.local\n"
        + "    tokenValidators:\n"
        + "    - name: jwks\n"
        + "      options:\n"
        + "        minRefreshInterval: 2m\n"
        + "        requestTimeout: 1m\n"
        + "        source: https://localhost:1234/\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMinimalMtlsToYaml() {
    MtlsConfigurationSettings mtls = new MtlsConfigurationSettings(true, "24h", "15m");

    Configuration configuration = new Configuration("my-config", null, null, mtls);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  mtls:\n"
        + "    enabled: true\n"
        + "    workloadCertTTL: 24h\n"
        + "    allowedClockSkew: 15m\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMtlsWithoutTokenValidatorsToYaml() {
    MtlsConfigurationSettings mtls = new MtlsConfigurationSettings(
        true,
        "24h",
        "15m",
        "localhost:50001",
        "cluster.local"
    );

    Configuration configuration = new Configuration("my-config", null, null, mtls);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  mtls:\n"
        + "    enabled: true\n"
        + "    workloadCertTTL: 24h\n"
        + "    allowedClockSkew: 15m\n"
        + "    sentryAddress: localhost:50001\n"
        + "    controlPlaneTrustDomain: cluster.local\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMtlsTokenValidatorWithoutOptionsToYaml() {
    List<MtlsTokenValidator> tokenValidators = new ArrayList<>();
    tokenValidators.add(new MtlsTokenValidator(MtlsTokenValidator.JWKS, null));

    MtlsConfigurationSettings mtls = new MtlsConfigurationSettings(true, null, null, null, null, tokenValidators);

    Configuration configuration = new Configuration("my-config", null, null, mtls);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  mtls:\n"
        + "    enabled: true\n"
        + "    tokenValidators:\n"
        + "    - name: jwks\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithEmptyMtlsTokenValidatorsToYaml() {
    MtlsConfigurationSettings mtls = new MtlsConfigurationSettings(
        false,
        null,
        null,
        null,
        null,
        new ArrayList<>()
    );

    Configuration configuration = new Configuration("my-config", null, null, mtls);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  mtls:\n"
        + "    enabled: false\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithLoggingToYaml() {
    ApiLoggingConfigurationSettings apiLogging = new ApiLoggingConfigurationSettings(true, true, true);
    LoggingConfigurationSettings logging = new LoggingConfigurationSettings(apiLogging);

    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withConfiguration(new Configuration("my-config", null, null, null, logging))
        .withAppChannelAddress("host.testcontainers.internal");

    Configuration configuration = dapr.getConfiguration();
    assertNotNull(configuration);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  logging:\n"
        + "    apiLogging:\n"
        + "      enabled: true\n"
        + "      obfuscateURLs: true\n"
        + "      omitHealthChecks: true\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMinimalLoggingToYaml() {
    LoggingConfigurationSettings logging = new LoggingConfigurationSettings(
        new ApiLoggingConfigurationSettings(true)
    );

    Configuration configuration = new Configuration("my-config", null, null, null, logging);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  logging:\n"
        + "    apiLogging:\n"
        + "      enabled: true\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithLoggingAndMtlsToYaml() {
    MtlsConfigurationSettings mtls = new MtlsConfigurationSettings(true, "24h", "15m");
    LoggingConfigurationSettings logging = new LoggingConfigurationSettings(
        new ApiLoggingConfigurationSettings(true, false, true)
    );

    Configuration configuration = new Configuration("my-config", null, null, mtls, logging);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  mtls:\n"
        + "    enabled: true\n"
        + "    workloadCertTTL: 24h\n"
        + "    allowedClockSkew: 15m\n"
        + "  logging:\n"
        + "    apiLogging:\n"
        + "      enabled: true\n"
        + "      obfuscateURLs: false\n"
        + "      omitHealthChecks: true\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithLoggingWithoutApiLoggingToYaml() {
    LoggingConfigurationSettings logging = new LoggingConfigurationSettings(null);

    Configuration configuration = new Configuration("my-config", null, null, null, logging);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  logging: {}\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }
  @Test
  public void testConfigurationWithMetricsToYaml() {
    Map<String, String> regex = new LinkedHashMap<>();
    regex.put("orders/", "orders/.+");

    List<MetricsLabel> labels = new ArrayList<>();
    labels.add(new MetricsLabel("method", regex));

    List<MetricsRule> rules = new ArrayList<>();
    rules.add(new MetricsRule("dapr_runtime_service_invocation_req_sent_total", labels));

    HttpMetricsConfigurationSettings http = new HttpMetricsConfigurationSettings(
        false,
        Arrays.asList("/items", "/orders/{orderID}", "/orders/{orderID}/items/{itemID}"),
        true
    );

    MetricsConfigurationSettings metrics = new MetricsConfigurationSettings(
        true,
        rules,
        Arrays.asList(5, 10, 50, 100, 500),
        http,
        true
    );

    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withConfiguration(new Configuration("my-config", null, null, null, null, metrics))
        .withAppChannelAddress("host.testcontainers.internal");

    Configuration configuration = dapr.getConfiguration();
    assertNotNull(configuration);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  metrics:\n"
        + "    enabled: true\n"
        + "    rules:\n"
        + "    - name: dapr_runtime_service_invocation_req_sent_total\n"
        + "      labels:\n"
        + "      - name: method\n"
        + "        regex:\n"
        + "          orders/: orders/.+\n"
        + "    latencyDistributionBuckets:\n"
        + "    - 5\n"
        + "    - 10\n"
        + "    - 50\n"
        + "    - 100\n"
        + "    - 500\n"
        + "    http:\n"
        + "      increasedCardinality: false\n"
        + "      pathMatching:\n"
        + "      - /items\n"
        + "      - /orders/{orderID}\n"
        + "      - /orders/{orderID}/items/{itemID}\n"
        + "      excludeVerbs: true\n"
        + "    recordErrorCodes: true\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMinimalMetricsToYaml() {
    MetricsConfigurationSettings metrics = new MetricsConfigurationSettings(false);

    Configuration configuration = new Configuration("my-config", null, null, null, null, metrics);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  metrics:\n"
        + "    enabled: false\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMetricsHttpOnlyToYaml() {
    HttpMetricsConfigurationSettings http = new HttpMetricsConfigurationSettings(false);
    MetricsConfigurationSettings metrics = new MetricsConfigurationSettings(true, http);

    Configuration configuration = new Configuration("my-config", null, null, null, null, metrics);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  metrics:\n"
        + "    enabled: true\n"
        + "    http:\n"
        + "      increasedCardinality: false\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithEmptyMetricsListsToYaml() {
    MetricsConfigurationSettings metrics = new MetricsConfigurationSettings(
        true,
        new ArrayList<>(),
        new ArrayList<>(),
        new HttpMetricsConfigurationSettings(null, new ArrayList<>(), null),
        null
    );

    Configuration configuration = new Configuration("my-config", null, null, null, null, metrics);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  metrics:\n"
        + "    enabled: true\n"
        + "    http: {}\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMetricsRuleWithoutLabelsToYaml() {
    List<MetricsRule> rules = new ArrayList<>();
    rules.add(new MetricsRule("dapr_http_server_request_count", null));

    MetricsConfigurationSettings metrics = new MetricsConfigurationSettings(true, rules, null, null, null);

    Configuration configuration = new Configuration("my-config", null, null, null, null, metrics);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  metrics:\n"
        + "    enabled: true\n"
        + "    rules:\n"
        + "    - name: dapr_http_server_request_count\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMetricsRuleWithEmptyLabelsToYaml() {
    List<MetricsRule> rules = new ArrayList<>();
    rules.add(new MetricsRule("dapr_http_server_request_count", new ArrayList<>()));

    MetricsConfigurationSettings metrics = new MetricsConfigurationSettings(true, rules, null, null, null);

    Configuration configuration = new Configuration("my-config", null, null, null, null, metrics);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  metrics:\n"
        + "    enabled: true\n"
        + "    rules:\n"
        + "    - name: dapr_http_server_request_count\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMetricsEnabledNullToYaml() {
    MetricsConfigurationSettings metrics = new MetricsConfigurationSettings(
        null,
        null,
        null,
        new HttpMetricsConfigurationSettings(null, null, true),
        false
    );

    Configuration configuration = new Configuration("my-config", null, null, null, null, metrics);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  metrics:\n"
        + "    http:\n"
        + "      excludeVerbs: true\n"
        + "    recordErrorCodes: false\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithEmptyMetricsToYaml() {
    MetricsConfigurationSettings metrics = new MetricsConfigurationSettings(null);

    Configuration configuration = new Configuration("my-config", null, null, null, null, metrics);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  metrics: {}\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMetricsLabelWithoutRegexToYaml() {
    List<MetricsLabel> labels = new ArrayList<>();
    labels.add(new MetricsLabel("method", null));

    List<MetricsRule> rules = new ArrayList<>();
    rules.add(new MetricsRule("dapr_http_server_request_count", labels));

    MetricsConfigurationSettings metrics = new MetricsConfigurationSettings(true, rules, null, null, null);

    Configuration configuration = new Configuration("my-config", null, null, null, null, metrics);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  metrics:\n"
        + "    enabled: true\n"
        + "    rules:\n"
        + "    - name: dapr_http_server_request_count\n"
        + "      labels:\n"
        + "      - name: method\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithLoggingAndMetricsToYaml() {
    LoggingConfigurationSettings logging = new LoggingConfigurationSettings(
        new ApiLoggingConfigurationSettings(true)
    );
    MetricsConfigurationSettings metrics = new MetricsConfigurationSettings(true, null, null, null, true);

    Configuration configuration = new Configuration("my-config", null, null, null, logging, metrics);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  logging:\n"
        + "    apiLogging:\n"
        + "      enabled: true\n"
        + "  metrics:\n"
        + "    enabled: true\n"
        + "    recordErrorCodes: true\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithNameResolutionToYaml() {
    Map<String, Object> nameResolutionConfiguration = new LinkedHashMap<>();
    nameResolutionConfiguration.put("connectionString", "/home/user/.dapr/nr.db");

    NameResolutionConfigurationSettings nameResolution =
        new NameResolutionConfigurationSettings("sqlite", "v1", nameResolutionConfiguration);

    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withConfiguration(new Configuration("my-config", null, null, null, null, null, nameResolution))
        .withAppChannelAddress("host.testcontainers.internal");

    Configuration configuration = dapr.getConfiguration();
    assertNotNull(configuration);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  nameResolution:\n"
        + "    component: sqlite\n"
        + "    version: v1\n"
        + "    configuration:\n"
        + "      connectionString: /home/user/.dapr/nr.db\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMinimalNameResolutionToYaml() {
    NameResolutionConfigurationSettings nameResolution = new NameResolutionConfigurationSettings("mdns");

    Configuration configuration = new Configuration("my-config", null, null, null, null, null, nameResolution);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  nameResolution:\n"
        + "    component: mdns\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithNameResolutionWithoutConfigurationToYaml() {
    NameResolutionConfigurationSettings nameResolution =
        new NameResolutionConfigurationSettings("kubernetes", "v1", new LinkedHashMap<>());

    Configuration configuration = new Configuration("my-config", null, null, null, null, null, nameResolution);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  nameResolution:\n"
        + "    component: kubernetes\n"
        + "    version: v1\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithNameResolutionNestedConfigurationToYaml() {
    Map<String, Object> consulClient = new LinkedHashMap<>();
    consulClient.put("address", "127.0.0.1:8500");

    Map<String, Object> nameResolutionConfiguration = new LinkedHashMap<>();
    nameResolutionConfiguration.put("client", consulClient);
    nameResolutionConfiguration.put("selfRegister", true);

    NameResolutionConfigurationSettings nameResolution =
        new NameResolutionConfigurationSettings("consul", "v1", nameResolutionConfiguration);

    Configuration configuration = new Configuration("my-config", null, null, null, null, null, nameResolution);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  nameResolution:\n"
        + "    component: consul\n"
        + "    version: v1\n"
        + "    configuration:\n"
        + "      client:\n"
        + "        address: 127.0.0.1:8500\n"
        + "      selfRegister: true\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithMetricsAndNameResolutionToYaml() {
    MetricsConfigurationSettings metrics = new MetricsConfigurationSettings(true);
    NameResolutionConfigurationSettings nameResolution = new NameResolutionConfigurationSettings("mdns", "v1");

    Configuration configuration = new Configuration("my-config", null, null, null, null, metrics, nameResolution);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  metrics:\n"
        + "    enabled: true\n"
        + "  nameResolution:\n"
        + "    component: mdns\n"
        + "    version: v1\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithDisallowedComponentsToYaml() {
    ComponentsConfigurationSettings components = new ComponentsConfigurationSettings(
        List.of("bindings.smtp", "secretstores.local.file", "state.redis/v1"));

    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withConfiguration(new Configuration("my-config", null, null, null, null, null, null, components))
        .withAppChannelAddress("host.testcontainers.internal");

    Configuration configuration = dapr.getConfiguration();
    assertNotNull(configuration);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  components:\n"
        + "    deny:\n"
        + "    - bindings.smtp\n"
        + "    - secretstores.local.file\n"
        + "    - state.redis/v1\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithEmptyDisallowedComponentsToYaml() {
    ComponentsConfigurationSettings components = new ComponentsConfigurationSettings(List.of());

    Configuration configuration = new Configuration("my-config", null, null, null, null, null, null, components);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  components: {}\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithNullDisallowedComponentsToYaml() {
    ComponentsConfigurationSettings components = new ComponentsConfigurationSettings(null);
    assertNull(components.getDeny());

    Configuration configuration = new Configuration("my-config", null, null, null, null, null, null, components);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  components: {}\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }

  @Test
  public void testConfigurationWithNameResolutionAndDisallowedComponentsToYaml() {
    NameResolutionConfigurationSettings nameResolution = new NameResolutionConfigurationSettings("mdns");
    ComponentsConfigurationSettings components = new ComponentsConfigurationSettings(List.of("bindings.smtp"));

    Configuration configuration =
        new Configuration("my-config", null, null, null, null, null, nameResolution, components);

    String configurationYaml = converter.convert(configuration);
    String expectedConfigurationYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Configuration\n"
        + "metadata:\n"
        + "  name: my-config\n"
        + "spec:\n"
        + "  nameResolution:\n"
        + "    component: mdns\n"
        + "  components:\n"
        + "    deny:\n"
        + "    - bindings.smtp\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }
}
