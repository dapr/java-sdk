package io.dapr.testcontainers.converter;

import io.dapr.testcontainers.HttpEndpoint;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpEndpointYamlConverter implements YamlConverter<HttpEndpoint> {
  private final Yaml mapper;

  public HttpEndpointYamlConverter(Yaml mapper) {
    this.mapper = mapper;
  }

  @Override
  public String convert(HttpEndpoint endpoint) {
    Map<String, Object> endpointProps = new LinkedHashMap<>();
    endpointProps.put("apiVersion", "dapr.io/v1alpha1");
    endpointProps.put("kind", "HTTPEndpoint");

    Map<String, String> endpointMetadata = new LinkedHashMap<>();
    endpointMetadata.put("name", endpoint.getName());
    endpointProps.put("metadata", endpointMetadata);

    Map<String, Object> endpointSpec = new LinkedHashMap<>();
    endpointSpec.put("baseUrl", endpoint.getBaseUrl());
    endpointProps.put("spec", endpointSpec);

    return mapper.dumpAsMap(endpointProps);
  }
}
