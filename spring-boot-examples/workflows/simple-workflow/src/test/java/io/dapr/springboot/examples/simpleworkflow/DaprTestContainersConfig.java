/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.springboot.examples.simpleworkflow;

import io.dapr.testcontainers.DaprContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;

/**
 * Starts the Dapr sidecar and control plane. No components are configured, so the sidecar uses an in-memory
 * state store to keep the workflow state. Use a persistent state store, such as PostgreSQL, outside of local
 * development (see the workflow patterns example).
 */
@TestConfiguration(proxyBeanMethods = false)
public class DaprTestContainersConfig {

  @Bean
  @ServiceConnection
  public DaprContainer daprContainer() {
    return new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("simple-workflow-app")
        .withAppPort(8080)
        .withAppHealthCheckPath("/actuator/health")
        .withAppChannelAddress("host.testcontainers.internal");
  }

}
