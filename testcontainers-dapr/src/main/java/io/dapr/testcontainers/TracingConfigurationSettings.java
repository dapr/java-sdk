/*
 * Copyright 2024 The Dapr Authors
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

/**
 * Represents a Dapr tracing configuration parameters .
 */
public class TracingConfigurationSettings implements ConfigurationSettings {
  private final String samplingRate;
  private final Boolean stdout;
  private final OtelTracingConfigurationSettings otel;
  private final ZipkinTracingConfigurationSettings zipkin;

  /**
   * Creates a new configuration.
   * @param samplingRate tracing sampling rate
   * @param stdout if it should send traces to the system standard output
   * @param otel if using OpenTelemetry
   * @param zipkin if using Zipkin
   */
  public TracingConfigurationSettings(
      String samplingRate,
      Boolean stdout,
      OtelTracingConfigurationSettings otel,
      ZipkinTracingConfigurationSettings zipkin
  ) {
    this.samplingRate = samplingRate;
    this.stdout = stdout;
    this.otel = otel;
    this.zipkin = zipkin;
  }

  public String getSamplingRate() {
    return this.samplingRate;
  }

  public Boolean getStdout() {
    return this.stdout;
  }

  public OtelTracingConfigurationSettings getOtel() {
    return otel;
  }

  public ZipkinTracingConfigurationSettings getZipkin() {
    return zipkin;
  }
}
