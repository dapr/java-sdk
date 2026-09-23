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

import io.dapr.testcontainers.ApiLoggingConfigurationSettings;
import io.dapr.testcontainers.AppHttpPipeline;
import io.dapr.testcontainers.ComponentsConfigurationSettings;
import io.dapr.testcontainers.Configuration;
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
import io.dapr.testcontainers.ZipkinTracingConfigurationSettings;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
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

    MtlsConfigurationSettings mtls = configuration.getMtls();
    if (mtls != null) {
      Map<String, Object> mtlsMap = new LinkedHashMap<>();

      putIfNotNull(mtlsMap, "enabled", mtls.getEnabled());
      putIfNotNull(mtlsMap, "workloadCertTTL", mtls.getWorkloadCertTtl());
      putIfNotNull(mtlsMap, "allowedClockSkew", mtls.getAllowedClockSkew());
      putIfNotNull(mtlsMap, "sentryAddress", mtls.getSentryAddress());
      putIfNotNull(mtlsMap, "controlPlaneTrustDomain", mtls.getControlPlaneTrustDomain());

      List<MtlsTokenValidator> tokenValidators = mtls.getTokenValidators();
      if (tokenValidators != null && !tokenValidators.isEmpty()) {
        List<Map<String, Object>> tokenValidatorsList = new ArrayList<>();

        for (MtlsTokenValidator tokenValidator : tokenValidators) {
          Map<String, Object> tokenValidatorMap = new LinkedHashMap<>();
          tokenValidatorMap.put("name", tokenValidator.getName());
          putIfNotNull(tokenValidatorMap, "options", tokenValidator.getOptions());
          tokenValidatorsList.add(tokenValidatorMap);
        }

        mtlsMap.put("tokenValidators", tokenValidatorsList);
      }

      configurationSpec.put("mtls", mtlsMap);
    }

    LoggingConfigurationSettings logging = configuration.getLogging();
    if (logging != null) {
      Map<String, Object> loggingMap = new LinkedHashMap<>();

      ApiLoggingConfigurationSettings apiLogging = logging.getApiLogging();
      if (apiLogging != null) {
        Map<String, Object> apiLoggingMap = new LinkedHashMap<>();

        putIfNotNull(apiLoggingMap, "enabled", apiLogging.getEnabled());
        putIfNotNull(apiLoggingMap, "obfuscateURLs", apiLogging.getObfuscateUrls());
        putIfNotNull(apiLoggingMap, "omitHealthChecks", apiLogging.getOmitHealthChecks());

        loggingMap.put("apiLogging", apiLoggingMap);
      }

      configurationSpec.put("logging", loggingMap);
    }

    MetricsConfigurationSettings metrics = configuration.getMetrics();
    if (metrics != null) {
      Map<String, Object> metricsMap = new LinkedHashMap<>();

      putIfNotNull(metricsMap, "enabled", metrics.getEnabled());

      List<MetricsRule> rules = metrics.getRules();
      if (rules != null && !rules.isEmpty()) {
        List<Map<String, Object>> rulesList = new ArrayList<>();

        for (MetricsRule rule : rules) {
          Map<String, Object> ruleMap = new LinkedHashMap<>();
          ruleMap.put("name", rule.getName());

          List<MetricsLabel> labels = rule.getLabels();
          if (labels != null && !labels.isEmpty()) {
            List<Map<String, Object>> labelsList = new ArrayList<>();

            for (MetricsLabel label : labels) {
              Map<String, Object> labelMap = new LinkedHashMap<>();
              labelMap.put("name", label.getName());
              putIfNotNull(labelMap, "regex", label.getRegex());
              labelsList.add(labelMap);
            }

            ruleMap.put("labels", labelsList);
          }

          rulesList.add(ruleMap);
        }

        metricsMap.put("rules", rulesList);
      }

      List<Integer> latencyDistributionBuckets = metrics.getLatencyDistributionBuckets();
      if (latencyDistributionBuckets != null && !latencyDistributionBuckets.isEmpty()) {
        metricsMap.put("latencyDistributionBuckets", latencyDistributionBuckets);
      }

      HttpMetricsConfigurationSettings http = metrics.getHttp();
      if (http != null) {
        Map<String, Object> httpMap = new LinkedHashMap<>();

        putIfNotNull(httpMap, "increasedCardinality", http.getIncreasedCardinality());

        List<String> pathMatching = http.getPathMatching();
        if (pathMatching != null && !pathMatching.isEmpty()) {
          httpMap.put("pathMatching", pathMatching);
        }

        putIfNotNull(httpMap, "excludeVerbs", http.getExcludeVerbs());

        metricsMap.put("http", httpMap);
      }

      putIfNotNull(metricsMap, "recordErrorCodes", metrics.getRecordErrorCodes());

      configurationSpec.put("metrics", metricsMap);
    }

    NameResolutionConfigurationSettings nameResolution = configuration.getNameResolution();
    if (nameResolution != null) {
      Map<String, Object> nameResolutionMap = new LinkedHashMap<>();

      putIfNotNull(nameResolutionMap, "component", nameResolution.getComponent());
      putIfNotNull(nameResolutionMap, "version", nameResolution.getVersion());

      Map<String, Object> nameResolutionConfiguration = nameResolution.getConfiguration();
      if (nameResolutionConfiguration != null && !nameResolutionConfiguration.isEmpty()) {
        nameResolutionMap.put("configuration", new LinkedHashMap<>(nameResolutionConfiguration));
      }

      configurationSpec.put("nameResolution", nameResolutionMap);
    }

    ComponentsConfigurationSettings components = configuration.getComponents();
    if (components != null) {
      Map<String, Object> componentsMap = new LinkedHashMap<>();

      List<String> deny = components.getDeny();
      if (deny != null && !deny.isEmpty()) {
        componentsMap.put("deny", new ArrayList<>(deny));
      }

      configurationSpec.put("components", componentsMap);
    }

    configurationProps.put("spec", configurationSpec);

    return mapper.dumpAsMap(configurationProps);
  }

  private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
    if (value != null) {
      map.put(key, value);
    }
  }
}
