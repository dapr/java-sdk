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

import io.dapr.testcontainers.AppHttpPipeline;
import io.dapr.testcontainers.Configuration;
import io.dapr.testcontainers.ListEntry;
import io.dapr.testcontainers.OtelTracingConfigurationSettings;
import io.dapr.testcontainers.TracingConfigurationSettings;
import io.dapr.testcontainers.ZipkinTracingConfigurationSettings;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationYamlConverter implements YamlConverter<Configuration> {
  private final Yaml mapper;

  public ConfigurationYamlConverter(Yaml mapper) {
    this.mapper = mapper;
  }

  @Override
  public String convert(Configuration configuration) {
    Map<String, Object> configurationProps = new LinkedHashMap<>();
    configurationProps.put("apiVersion", "dapr.io/v1alpha1");
    configurationProps.put("kind", "Configuration");

    Map<String, String> configurationMetadata = new LinkedHashMap<>();
    configurationMetadata.put("name", configuration.getName());
    configurationProps.put("metadata", configurationMetadata);

    Map<String, Object> configurationSpec = new LinkedHashMap<>();
    TracingConfigurationSettings tracing = configuration.getTracing();

    if (tracing != null) {
      Map<String, Object> tracingMap = new LinkedHashMap<>();

      tracingMap.put("samplingRate", configuration.getTracing().getSamplingRate());
      tracingMap.put("stdout", configuration.getTracing().getStdout());

      OtelTracingConfigurationSettings otel = tracing.getOtel();

      if (otel != null) {
        Map<String, Object> otelMap = new LinkedHashMap<>();

        otelMap.put("endpointAddress", otel.getEndpointAddress());
        otelMap.put("isSecure", otel.getSecure());
        otelMap.put("protocol", otel.getProtocol());

        tracingMap.put("otel", otelMap);
      }

      ZipkinTracingConfigurationSettings zipkin = tracing.getZipkin();

      if (zipkin != null) {
        Map<String, Object> zipkinMap = new LinkedHashMap<>();

        zipkinMap.put("endpointAddress", zipkin.getEndpointAddress());

        tracingMap.put("zipkin", zipkinMap);
      }

      configurationSpec.put("tracing", tracingMap);

    }

    AppHttpPipeline appHttpPipeline = configuration.getAppHttpPipeline();
    if (appHttpPipeline != null) {

      Map<String, Object> appHttpPipelineMap = new LinkedHashMap<>();
      List<ListEntry> handlers = appHttpPipeline.getHandlers();
      appHttpPipelineMap.put("handlers", handlers);
      configurationSpec.put("appHttpPipeline", appHttpPipelineMap);

    }

    configurationProps.put("spec", configurationSpec);

    return mapper.dumpAsMap(configurationProps);
  }
}
