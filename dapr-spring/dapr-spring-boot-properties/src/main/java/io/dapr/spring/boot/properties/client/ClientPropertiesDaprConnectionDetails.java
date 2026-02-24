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

public class ClientPropertiesDaprConnectionDetails implements DaprConnectionDetails {

  private final DaprClientProperties daprClientProperties;

  public ClientPropertiesDaprConnectionDetails(DaprClientProperties daprClientProperties) {
    this.daprClientProperties = daprClientProperties;
  }

  @Override
  public String getHttpEndpoint() {
    return this.daprClientProperties.getHttpEndpoint();
  }

  @Override
  public String getGrpcEndpoint() {
    return this.daprClientProperties.getGrpcEndpoint();
  }

  @Override
  public Integer getHttpPort() {
    return this.daprClientProperties.getHttpPort();
  }

  @Override
  public Integer getGrpcPort() {
    return this.daprClientProperties.getGrpcPort();
  }

  @Override
  public String getApiToken() {
    return this.daprClientProperties.getApiToken();
  }

}
