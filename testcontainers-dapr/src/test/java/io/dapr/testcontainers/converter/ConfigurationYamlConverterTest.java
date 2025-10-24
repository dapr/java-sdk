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

import io.dapr.testcontainers.AppHttpPipeline;
import io.dapr.testcontainers.Configuration;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.HttpPipeline;
import io.dapr.testcontainers.ListEntry;
import io.dapr.testcontainers.OtelTracingConfigurationSettings;
import io.dapr.testcontainers.TracingConfigurationSettings;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

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

    
    List<ListEntry> appHttpHandlers = new ArrayList<>();
    appHttpHandlers.add(new ListEntry("alias", "middleware.http.routeralias"));

    AppHttpPipeline appHttpPipeline = new AppHttpPipeline(appHttpHandlers);

    List<ListEntry> httpHandlers = new ArrayList<>();

    //Notice that this needs to be different objects, if not Snake YAML will add a reference to the object
    HttpPipeline httpPipeline = new HttpPipeline(httpHandlers);
    httpHandlers.add(new ListEntry("alias", "middleware.http.routeralias"));

    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withConfiguration(new Configuration("my-config", tracing, appHttpPipeline, httpPipeline))
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
        + "  httpPipeline:\n"
        + "    handlers:\n"
        + "    - name: alias\n"
        + "      type: middleware.http.routeralias\n"
        + "  appHttpPipeline:\n"
        + "    handlers:\n"
        + "    - name: alias\n"
        + "      type: middleware.http.routeralias\n";

    assertEquals(expectedConfigurationYaml, configurationYaml);
  }
}
