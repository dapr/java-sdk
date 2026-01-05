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

package io.dapr.testcontainers.internal;

import io.dapr.testcontainers.internal.spring.DaprSpringBootTest;
import org.testcontainers.junit.jupiter.Container;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static field containing a {@link io.dapr.testcontainers.DaprContainer}
 * for automatic integration with Spring Boot tests.
 *
 * <p>This annotation combines the Testcontainers {@link Container} annotation
 * with Dapr-specific configuration. When used with {@link DaprSpringBootTest},
 * it automatically:</p>
 * <ul>
 *   <li>Manages the container lifecycle via Testcontainers</li>
 *   <li>Configures Spring properties (server.port, dapr.http.endpoint, dapr.grpc.endpoint)</li>
 * </ul>
 *
 * <p><b>Important:</b> For tests that require Dapr-to-app communication (like actor tests),
 * you must call {@code Testcontainers.exposeHostPorts(container.getAppPort())}
 * in your {@code @BeforeEach} method before registering actors or making Dapr calls.</p>
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
 *     @BeforeEach
 *     void setUp() {
 *         Testcontainers.exposeHostPorts(DAPR.getAppPort());
 *     }
 *
 *     @Test
 *     void testSomething() {
 *         // Your test code here
 *     }
 * }
 * }</pre>
 *
 * @see DaprSpringBootTest
 * @see io.dapr.testcontainers.DaprContainer#createForSpringBootTest(String)
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Container
public @interface DaprSidecarContainer {
}
