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

package io.dapr.testcontainers.spring;

import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JUnit 5 extension that handles Dapr container setup for Spring Boot tests.
 *
 * <p>This extension:</p>
 * <ul>
 *   <li>Discovers fields annotated with {@link DaprSidecarContainer}</li>
 *   <li>Registers the container for property injection by {@link DaprSpringBootContextInitializer}</li>
 * </ul>
 *
 * <p>This extension is automatically registered when using {@link DaprSpringBootTest}.</p>
 */
public class DaprSpringBootExtension implements BeforeAllCallback {

  /**
   * Registry of DaprContainers by test class. Used by {@link DaprSpringBootContextInitializer}
   * to configure Spring properties.
   */
  static final Map<Class<?>, DaprContainer> CONTAINER_REGISTRY = new ConcurrentHashMap<>();

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Class<?> testClass = context.getRequiredTestClass();

    // Find fields annotated with @DaprSidecarContainer
    List<Field> containerFields = AnnotationSupport.findAnnotatedFields(
        testClass,
        DaprSidecarContainer.class,
        field -> DaprContainer.class.isAssignableFrom(field.getType())
    );

    if (containerFields.isEmpty()) {
      throw new IllegalStateException(
          "No @DaprSidecarContainer annotated field of type DaprContainer found in " + testClass.getName()
              + ". Add a static field like: @DaprSidecarContainer private static final DaprContainer DAPR = "
              + "DaprContainer.createForSpringBootTest(\"my-app\");"
      );
    }

    if (containerFields.size() > 1) {
      throw new IllegalStateException(
          "Multiple @DaprSidecarContainer annotated fields found in " + testClass.getName()
              + ". Only one DaprContainer per test class is supported."
      );
    }

    Field containerField = containerFields.get(0);
    containerField.setAccessible(true);

    DaprContainer container = (DaprContainer) containerField.get(null);

    if (container == null) {
      throw new IllegalStateException(
          "@DaprSidecarContainer field '" + containerField.getName() + "' is null in " + testClass.getName()
      );
    }

    // Register container for the context initializer
    CONTAINER_REGISTRY.put(testClass, container);

    // Note: Testcontainers.exposeHostPorts() is NOT called here because of timing requirements.
    // It must be called in @BeforeEach, after the container starts to ensure proper Dapr-to-app communication.
  }
}
