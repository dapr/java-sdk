package io.dapr.testcontainers.converter;

import io.dapr.testcontainers.Configuration;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.OtelTracingConfigurationSettings;
import io.dapr.testcontainers.TracingConfigurationSettings;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    DaprContainer dapr = new DaprContainer("daprio/daprd")
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withConfiguration(new Configuration("my-config", tracing))
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
        + "      protocol: grpc\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }
}
