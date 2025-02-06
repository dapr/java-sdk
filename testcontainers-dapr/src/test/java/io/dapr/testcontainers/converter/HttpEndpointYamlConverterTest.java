package io.dapr.testcontainers.converter;

import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.HttpEndpoint;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpEndpointYamlConverterTest {
  private final Yaml MAPPER = YamlMapperFactory.create();

  private final HttpEndpointYamlConverter converter = new HttpEndpointYamlConverter(MAPPER);

  @Test
  void testHttpEndpointToYaml() {
     DaprContainer dapr = new DaprContainer("daprio/daprd")
         .withAppName("dapr-app")
         .withAppPort(8081)
         .withHttpEndpoint(new HttpEndpoint("my-endpoint", "http://localhost:8080"))
         .withAppChannelAddress("host.testcontainers.internal");

     Set<HttpEndpoint> endpoints = dapr.getHttpEndpoints();
     assertEquals(1, endpoints.size());

     HttpEndpoint endpoint = endpoints.iterator().next();
     String endpointYaml = converter.convert(endpoint);
     String expectedEndpointYaml =
           "apiVersion: dapr.io/v1alpha1\n"
         + "kind: HTTPEndpoint\n"
         + "metadata:\n"
         + "  name: my-endpoint\n"
         + "spec:\n"
         + "  baseUrl: http://localhost:8080\n";

     assertEquals(expectedEndpointYaml, endpointYaml);
  }
}
