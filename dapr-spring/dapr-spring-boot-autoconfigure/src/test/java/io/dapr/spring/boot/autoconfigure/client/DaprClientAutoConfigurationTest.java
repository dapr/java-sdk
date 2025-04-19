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

package io.dapr.spring.boot.autoconfigure.client;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DaprClientAutoConfiguration}.
 */
@ExtendWith(MockitoExtension.class)
class DaprClientAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(DaprClientAutoConfiguration.class));

  @Mock
  private DaprConnectionDetails connectionDetails;

  @Mock
  private DaprClientBuilder builder;

  private DaprClientAutoConfiguration configuration;

  @Test
  void daprClientBuilder() {
    contextRunner.run(context -> assertThat(context).hasSingleBean(DaprClientBuilder.class));
  }

  @Test
  void daprClient() {
    contextRunner.run(context -> assertThat(context).hasSingleBean(DaprClient.class));
  }

  @BeforeEach
  void setUp() {
    configuration = new TestDaprClientAutoConfiguration(builder);
  }

  @Test
  @DisplayName("Should override HTTP endpoint if it exists")
  void shouldOverrideHttpEndpointIfExists() {
    String httpEndpoint = "http://localhost:3500";

    when(connectionDetails.getHttpEndpoint()).thenReturn(httpEndpoint);
    when(connectionDetails.getGrpcEndpoint()).thenReturn(null);
    when(connectionDetails.getHttpPort()).thenReturn(null);
    when(connectionDetails.getGrpcPort()).thenReturn(null);

    configuration.daprClientBuilder(connectionDetails);

    verify(builder).withPropertyOverride(Properties.HTTP_ENDPOINT, httpEndpoint);
  }

  @Test
  @DisplayName("Should override GRPC endpoint if it exists")
  void shouldOverrideGrpcEndpointIfExists() {
    String grpcEndpoint = "grpc://localhost:5001";

    when(connectionDetails.getHttpEndpoint()).thenReturn(null);
    when(connectionDetails.getGrpcEndpoint()).thenReturn(grpcEndpoint);
    when(connectionDetails.getHttpPort()).thenReturn(null);
    when(connectionDetails.getGrpcPort()).thenReturn(null);

    configuration.daprClientBuilder(connectionDetails);

    verify(builder).withPropertyOverride(Properties.GRPC_ENDPOINT, grpcEndpoint);
  }

  @Test
  @DisplayName("Should override HTTP port if it exists")
  void shouldOverrideHttpPortIfExists() {
    Integer httpPort = 3600;

    when(connectionDetails.getHttpEndpoint()).thenReturn(null);
    when(connectionDetails.getGrpcEndpoint()).thenReturn(null);
    when(connectionDetails.getHttpPort()).thenReturn(httpPort);
    when(connectionDetails.getGrpcPort()).thenReturn(null);

    configuration.daprClientBuilder(connectionDetails);

    verify(builder).withPropertyOverride(Properties.HTTP_PORT, String.valueOf(httpPort));
  }

  @Test
  @DisplayName("Should override GRPC port if it exists")
  void shouldOverrideGrpcPortIfExists() {
    Integer grpcPort = 6001;

    when(connectionDetails.getHttpEndpoint()).thenReturn(null);
    when(connectionDetails.getGrpcEndpoint()).thenReturn(null);
    when(connectionDetails.getHttpPort()).thenReturn(null);
    when(connectionDetails.getGrpcPort()).thenReturn(grpcPort);

    configuration.daprClientBuilder(connectionDetails);

    verify(builder).withPropertyOverride(Properties.GRPC_PORT, String.valueOf(grpcPort));
  }

  private static class TestDaprClientAutoConfiguration extends DaprClientAutoConfiguration {

    private final DaprClientBuilder daprClientBuilder;

    public TestDaprClientAutoConfiguration(DaprClientBuilder daprClientBuilder) {
      this.daprClientBuilder = daprClientBuilder;
    }

    @Override
    protected DaprClientBuilder createDaprClientBuilder() {
      return daprClientBuilder;
    }
  }

}
