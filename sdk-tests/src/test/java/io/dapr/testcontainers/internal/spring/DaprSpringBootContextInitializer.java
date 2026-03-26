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

package io.dapr.testcontainers.internal.spring;

import io.dapr.testcontainers.DaprContainer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Spring {@link ApplicationContextInitializer} that configures Dapr-related properties
 * based on the {@link DaprContainer} registered by {@link DaprSpringBootExtension}.
 *
 * <p>This initializer sets the following properties:</p>
 * <ul>
 *   <li>{@code server.port} - The port allocated for the Spring Boot application</li>
 *   <li>{@code dapr.http.endpoint} - The HTTP endpoint of the Dapr sidecar</li>
 *   <li>{@code dapr.grpc.endpoint} - The gRPC endpoint of the Dapr sidecar</li>
 * </ul>
 *
 * <p>This initializer is automatically registered when using {@link DaprSpringBootTest}.</p>
 */
public class DaprSpringBootContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private static final String PROPERTY_SOURCE_NAME = "daprTestcontainersProperties";
  private static final String CURRENT_TEST_CLASS_PROPERTY = "dapr.testcontainers.current-test-class";

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    DaprContainer container = findContainer();

    if (container == null) {
      throw new IllegalStateException(
          "No DaprContainer found in registry. Ensure you are using @DaprSpringBootTest "
              + "with a @DaprSidecarContainer annotated field."
      );
    }

    // Create a property source with lazy resolution for endpoints
    // server.port can be resolved immediately since it's set at container creation time
    // Dapr endpoints are resolved lazily since the container may not be started yet
    applicationContext.getEnvironment().getPropertySources()
        .addFirst(new DaprLazyPropertySource(PROPERTY_SOURCE_NAME, container));
  }

  private DaprContainer findContainer() {
    String currentTestClass = System.getProperty(CURRENT_TEST_CLASS_PROPERTY);
    if (currentTestClass != null) {
      return DaprSpringBootExtension.CONTAINER_REGISTRY.entrySet().stream()
          .filter(entry -> entry.getKey().getName().equals(currentTestClass))
          .map(Map.Entry::getValue)
          .findFirst()
          .orElse(null);
    }

    // Fallback for unexpected bootstrap order.
    return DaprSpringBootExtension.CONTAINER_REGISTRY.values().stream().findFirst().orElse(null);
  }

  /**
   * Custom PropertySource that lazily resolves Dapr container endpoints.
   * This allows the endpoints to be resolved after the container has started.
   */
  private static class DaprLazyPropertySource extends MapPropertySource {
    private final Map<String, Supplier<Object>> lazyProperties;

    DaprLazyPropertySource(String name, DaprContainer container) {
      super(name, new HashMap<>());

      this.lazyProperties = new HashMap<>();
      lazyProperties.put("server.port", container::getAppPort);
      lazyProperties.put("dapr.http.endpoint", container::getHttpEndpoint);
      lazyProperties.put("dapr.grpc.endpoint", container::getGrpcEndpoint);
    }

    @Override
    public Object getProperty(String name) {
      Supplier<Object> supplier = lazyProperties.get(name);
      if (supplier != null) {
        return supplier.get();
      }
      return null;
    }

    @Override
    public boolean containsProperty(String name) {
      return lazyProperties.containsKey(name);
    }

    @Override
    public String[] getPropertyNames() {
      return lazyProperties.keySet().toArray(new String[0]);
    }
  }
}
