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

package io.dapr.testcontainers;

/**
 * Configuration settings for Otel tracing.
 */
public class OtelTracingConfigurationSettings implements ConfigurationSettings {
  private final String endpointAddress;
  private final Boolean isSecure;
  private final String protocol;

  /**
   * Creates a new configuration.
   * @param endpointAddress tracing endpoint address
   * @param isSecure if the endpoint is secure
   * @param protocol tracing protocol
   */
  public OtelTracingConfigurationSettings(String endpointAddress, Boolean isSecure, String protocol) {
    this.endpointAddress = endpointAddress;
    this.isSecure = isSecure;
    this.protocol = protocol;
  }

  public String getEndpointAddress() {
    return endpointAddress;
  }

  public Boolean getSecure() {
    return isSecure;
  }

  public String getProtocol() {
    return protocol;
  }
}
