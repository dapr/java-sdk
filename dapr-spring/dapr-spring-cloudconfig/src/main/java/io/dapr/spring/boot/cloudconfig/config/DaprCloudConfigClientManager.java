/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.spring.boot.cloudconfig.config;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.config.Properties;
import io.dapr.spring.boot.autoconfigure.client.DaprClientProperties;
import io.dapr.spring.boot.autoconfigure.client.DaprConnectionDetails;

public class DaprCloudConfigClientManager {

  private static DaprClient daprClient;
  private static DaprPreviewClient daprPreviewClient;
  private final DaprCloudConfigProperties daprCloudConfigProperties;
  private final DaprClientProperties daprClientConfig;

  /**
   * Create a DaprCloudConfigClientManager to create Config-Specific Dapr Client.
   *
   * @param daprCloudConfigProperties Properties of Dapr Cloud Config
   * @param daprClientConfig          Properties of Dapr Client
   */
  public DaprCloudConfigClientManager(DaprCloudConfigProperties daprCloudConfigProperties,
                                      DaprClientProperties daprClientConfig) {
    this.daprCloudConfigProperties = daprCloudConfigProperties;
    this.daprClientConfig = daprClientConfig;

    DaprClientBuilder daprClientBuilder = createDaprClientBuilder(
        createDaprConnectionDetails(daprClientConfig)
    );

    if (DaprCloudConfigClientManager.daprClient == null) {
      DaprCloudConfigClientManager.daprClient = daprClientBuilder.build();
    }

    if (DaprCloudConfigClientManager.daprPreviewClient == null) {
      DaprCloudConfigClientManager.daprPreviewClient = daprClientBuilder.buildPreviewClient();
    }
  }

  public static DaprPreviewClient getDaprPreviewClient() {
    return DaprCloudConfigClientManager.daprPreviewClient;
  }

  public static DaprClient getDaprClient() {
    return DaprCloudConfigClientManager.daprClient;
  }

  private DaprConnectionDetails createDaprConnectionDetails(DaprClientProperties properties) {
    return new CloudConfigPropertiesDaprConnectionDetails(properties);
  }

  DaprClientBuilder createDaprClientBuilder(DaprConnectionDetails daprConnectionDetails) {
    DaprClientBuilder builder = new DaprClientBuilder();
    if (daprConnectionDetails.httpEndpoint() != null) {
      builder.withPropertyOverride(Properties.HTTP_ENDPOINT, daprConnectionDetails.httpEndpoint());
    }
    if (daprConnectionDetails.grpcEndpoint() != null) {
      builder.withPropertyOverride(Properties.GRPC_ENDPOINT, daprConnectionDetails.grpcEndpoint());
    }
    if (daprConnectionDetails.httpPort() != null) {
      builder.withPropertyOverride(Properties.HTTP_PORT, String.valueOf(daprConnectionDetails.httpPort()));
    }
    if (daprConnectionDetails.grpcPort() != null) {
      builder.withPropertyOverride(Properties.GRPC_PORT, String.valueOf(daprConnectionDetails.grpcPort()));
    }
    return builder;
  }

  public DaprCloudConfigProperties getDaprCloudConfigProperties() {
    return daprCloudConfigProperties;
  }

  public DaprClientProperties getDaprClientConfig() {
    return daprClientConfig;
  }
}
