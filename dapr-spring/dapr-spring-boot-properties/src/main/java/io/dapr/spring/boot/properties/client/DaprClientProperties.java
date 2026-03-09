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

package io.dapr.spring.boot.properties.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dapr.client")
public class DaprClientProperties {
  private String httpEndpoint;
  private String grpcEndpoint;
  private Integer httpPort;
  private Integer grpcPort;
  private String apiToken;

  /**
   * Constructs a {@link DaprClientProperties}.
   */
  public DaprClientProperties() {
  }

  /**
   * Constructs a {@link DaprClientProperties}.
   * @param httpEndpoint http endpoint to interact with the Dapr Sidecar
   * @param grpcEndpoint grpc endpoint to interact with the Dapr Sidecar
   * @param httpPort http port to interact with the Dapr Sidecar
   * @param grpcPort grpc port to interact with the Dapr Sidecar
   * @param apiToken dapr API token to interact with the Dapr Sidecar
   */
  public DaprClientProperties(String httpEndpoint, String grpcEndpoint, Integer httpPort, Integer grpcPort,
                              String apiToken) {
    this.httpEndpoint = httpEndpoint;
    this.grpcEndpoint = grpcEndpoint;
    this.httpPort = httpPort;
    this.grpcPort = grpcPort;
    this.apiToken = apiToken;
  }

  public String getHttpEndpoint() {
    return httpEndpoint;
  }

  public String getGrpcEndpoint() {
    return grpcEndpoint;
  }

  public Integer getHttpPort() {
    return httpPort;
  }

  public Integer getGrpcPort() {
    return grpcPort;
  }

  public void setHttpEndpoint(String httpEndpoint) {
    this.httpEndpoint = httpEndpoint;
  }

  public void setGrpcEndpoint(String grpcEndpoint) {
    this.grpcEndpoint = grpcEndpoint;
  }

  public void setHttpPort(Integer httpPort) {
    this.httpPort = httpPort;
  }

  public void setGrpcPort(Integer grpcPort) {
    this.grpcPort = grpcPort;
  }

  public String getApiToken() {
    return apiToken;
  }

  public void setApiToken(String apiToken) {
    this.apiToken = apiToken;
  }
}
