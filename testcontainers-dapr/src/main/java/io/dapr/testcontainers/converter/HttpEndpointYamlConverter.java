/*
 * Copyright 2021 The Dapr Authors
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
