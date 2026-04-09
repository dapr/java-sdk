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

package io.dapr.spring.boot.autoconfigure.client;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the observation wiring in {@link DaprClientAutoConfiguration}.
 *
 * <p>Verifies two key requirements:
 * <ol>
 *   <li>When a non-noop {@link ObservationRegistry} is present, both {@code DaprClient} and
 *       {@code DaprWorkflowClient} beans are wrapped with observation decorators.</li>
 *   <li>Consumers inject the beans by their base types ({@code DaprClient},
 *       {@code DaprWorkflowClient}) — no code changes needed.</li>
 * </ol>
 */
class DaprClientObservationAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(DaprClientAutoConfiguration.class));

  // -------------------------------------------------------------------------
  // Without ObservationRegistry — plain beans
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("DaprClient is a plain client when no ObservationRegistry is present")
  void daprClientIsPlainWhenNoRegistry() {
    contextRunner
        .withBean(DaprClientBuilder.class, () -> mockBuilderReturningMockClient())
        .run(context -> {
          assertThat(context).hasSingleBean(DaprClient.class);
          assertThat(context.getBean(DaprClient.class))
              .isNotInstanceOf(ObservationDaprClient.class);
        });
  }

  // -------------------------------------------------------------------------
  // With ObservationRegistry — wrapped beans
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("DaprClient is ObservationDaprClient when a non-noop ObservationRegistry is present")
  void daprClientIsObservationWrappedWhenRegistryPresent() {
    contextRunner
        .withBean(DaprClientBuilder.class, () -> mockBuilderReturningMockClient())
        .withBean(ObservationRegistry.class, TestObservationRegistry::create)
        .run(context -> {
          assertThat(context).hasSingleBean(DaprClient.class);
          assertThat(context.getBean(DaprClient.class))
              .isInstanceOf(ObservationDaprClient.class);
        });
  }

  @Test
  @DisplayName("DaprWorkflowClient is ObservationDaprWorkflowClient when a non-noop registry is present")
  void daprWorkflowClientIsObservationWrappedWhenRegistryPresent() {
    contextRunner
        .withBean(DaprClientBuilder.class, () -> mockBuilderReturningMockClient())
        .withBean(ObservationRegistry.class, TestObservationRegistry::create)
        .run(context -> {
          assertThat(context).hasSingleBean(DaprWorkflowClient.class);
          assertThat(context.getBean(DaprWorkflowClient.class))
              .isInstanceOf(ObservationDaprWorkflowClient.class);
        });
  }

  // -------------------------------------------------------------------------
  // Transparency — beans remain injectable by base type
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Consumers can inject DaprClient by its base type regardless of wrapping")
  void daprClientInjectableByBaseType() {
    contextRunner
        .withBean(DaprClientBuilder.class, () -> mockBuilderReturningMockClient())
        .withBean(ObservationRegistry.class, TestObservationRegistry::create)
        .run(context -> {
          // Injecting as DaprClient works — no ClassCastException, no code changes needed
          DaprClient client = context.getBean(DaprClient.class);
          assertThat(client).isNotNull();
          assertThat(client).isInstanceOf(DaprClient.class);
        });
  }

  @Test
  @DisplayName("Consumers can inject DaprWorkflowClient by its base type regardless of wrapping")
  void daprWorkflowClientInjectableByBaseType() {
    contextRunner
        .withBean(DaprClientBuilder.class, () -> mockBuilderReturningMockClient())
        .withBean(ObservationRegistry.class, TestObservationRegistry::create)
        .run(context -> {
          // Injecting as DaprWorkflowClient works — ObservationDaprWorkflowClient IS-A DaprWorkflowClient
          DaprWorkflowClient workflowClient = context.getBean(DaprWorkflowClient.class);
          assertThat(workflowClient).isNotNull();
          assertThat(workflowClient).isInstanceOf(DaprWorkflowClient.class);
        });
  }

  @Test
  @DisplayName("Noop ObservationRegistry results in plain (unwrapped) DaprClient")
  void noopRegistryResultsInPlainClient() {
    contextRunner
        .withBean(DaprClientBuilder.class, () -> mockBuilderReturningMockClient())
        .withBean(ObservationRegistry.class, ObservationRegistry::create) // NOOP registry
        .run(context -> {
          assertThat(context).hasSingleBean(DaprClient.class);
          assertThat(context.getBean(DaprClient.class))
              .isNotInstanceOf(ObservationDaprClient.class);
        });
  }

  @Test
  @DisplayName("User-provided DaprClient bean is not replaced by autoconfiguration")
  void userProvidedDaprClientIsNotReplaced() {
    DaprClient userClient = mock(DaprClient.class);
    contextRunner
        .withBean(ObservationRegistry.class, TestObservationRegistry::create)
        .withBean(DaprClient.class, () -> userClient)
        .run(context -> {
          assertThat(context).hasSingleBean(DaprClient.class);
          assertThat(context.getBean(DaprClient.class)).isSameAs(userClient);
        });
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static DaprClientBuilder mockBuilderReturningMockClient() {
    DaprClientBuilder builder = mock(DaprClientBuilder.class);
    when(builder.build()).thenReturn(mock(DaprClient.class));
    return builder;
  }
}
