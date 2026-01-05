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

import io.dapr.testcontainers.internal.DaprSidecarContainer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composed annotation that combines {@link SpringBootTest}, {@link Testcontainers},
 * and the necessary extensions for Dapr integration testing.
 *
 * <p>This annotation simplifies the setup of Spring Boot integration tests with Dapr
 * by handling port allocation, property configuration, and container lifecycle automatically.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @DaprSpringBootTest(classes = MyApplication.class)
 * class MyDaprIT {
 *
 *     @DaprSidecarContainer
 *     private static final DaprContainer DAPR = DaprContainer.createForSpringBootTest("my-app")
 *         .withComponent(new Component("statestore", "state.in-memory", "v1", Map.of()));
 *
 *     @Test
 *     void testSomething() {
 *         // Your test code here
 *     }
 * }
 * }</pre>
 *
 * @see DaprSidecarContainer
 * @see io.dapr.testcontainers.DaprContainer#createForSpringBootTest(String)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DaprSpringBootExtension.class)  // Must be first to register container before Spring starts
@Testcontainers  // Starts containers via @Container/@DaprSidecarContainer
@ContextConfiguration(initializers = DaprSpringBootContextInitializer.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)  // Starts Spring context last
public @interface DaprSpringBootTest {

  /**
   * The application classes to use for the test.
   * Alias for {@link SpringBootTest#classes()}.
   *
   * @return the application classes
   */
  @AliasFor(annotation = SpringBootTest.class, attribute = "classes")
  Class<?>[] classes() default {};

  /**
   * Additional properties to configure the test.
   * Alias for {@link SpringBootTest#properties()}.
   *
   * @return additional properties
   */
  @AliasFor(annotation = SpringBootTest.class, attribute = "properties")
  String[] properties() default {};
}
