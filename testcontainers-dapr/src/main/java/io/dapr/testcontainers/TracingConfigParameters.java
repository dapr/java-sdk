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
public class TracingConfigParameters {

  private String samplingRate;
  private Boolean stdout;
  private String otelEndpoint;
  private Boolean otelIsSecure;
  private String otelProtocol;

  //@TODO: add zipkin parameters


  /**
   * Creates a new configuration.
   */
  public TracingConfigParameters() {
  }

  /**
   * Creates a new configuration.
   * @param samplingRate tracing sampling rate
   * @param stdout if it should send traces to the system standard output
   * @param otelEndpoint if using OpenTelemetry where the collector endpoint is
   * @param otelIsSecure if using OpenTelemetry if the channel is secure
   * @param otelProtocol if using OpenTelemetry which protocol is being used http or grpc
   */
  public TracingConfigParameters(String samplingRate, Boolean stdout,
                                 String otelEndpoint, Boolean otelIsSecure, String otelProtocol) {
    this.samplingRate = samplingRate;
    this.stdout = stdout;
    this.otelEndpoint = otelEndpoint;
    this.otelIsSecure = otelIsSecure;
    this.otelProtocol = otelProtocol;
  }

  public String getSamplingRate() {
    return this.samplingRate;
  }

  public Boolean getStdout() {
    return this.stdout;
  }

  public String getOtelEndpoint() {
    return this.otelEndpoint;
  }

  public Boolean getOtelIsSecure() {
    return this.otelIsSecure;
  }

  public String getOtelProtocol() {
    return this.otelProtocol;
  }
}
