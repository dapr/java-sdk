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
 * limitations under the License.
 */
package io.dapr.it.testcontainers.support;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class MigratedBoundaryTest {
  @Test
  void bindingAndSharedSupportAvoidLegacyHostInfrastructure() throws Exception {
    Path root = Path.of("src/test/java/io/dapr/it/testcontainers");
    List<String> forbidden = List.of("AppRun", "DaprPorts", "BaseContainerIT", "SharedTestInfra",
        "io.dapr.it.Command", "ProcessBuilder", "Runtime.getRuntime().exec", "exposeHostPorts",
        "host.testcontainers.internal", "withNetworkMode(\"host\")", "setPortBindings", "withReuse(true)",
        "withAccessToHost(true)", "api.github.com", "mvn exec:", "withCopyToContainer", "withCopyFileToContainer",
        "ServerSocket", "addFixedExposedPort", "withFixedExposedPort");
    try (var files = Files.walk(root)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java"))
          .filter(p -> p.startsWith(root.resolve("binding")) || p.equals(root.resolve("support/DaprTestEnvironment.java"))
              || p.equals(root.resolve("support/OwnedResources.java"))).toList()) {
        String source = Files.readString(file);
        for (String token : forbidden) {
          assertFalse(source.contains(token), file + " contains forbidden token " + token);
        }
        if (file.startsWith(root.resolve("binding"))) {
          for (String token : List.of(".start()", "@Container\n  private static final KafkaContainer", "target/")) {
            assertFalse(source.contains(token), file + " contains forbidden token " + token);
          }
        }
      }
    }
  }
}
