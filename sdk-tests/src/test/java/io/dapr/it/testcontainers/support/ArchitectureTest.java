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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArchitectureTest {
  @Test
  void migratedSourcesStayContainerOnlyAndUnique() throws Exception {
    Path root = Path.of("src/test/java/io/dapr/it");
    List<String> forbidden = List.of("AppRun", "DaprPorts", "BaseContainerIT", "SharedTestInfra", "ProcessBuilder",
        "Runtime.getRuntime()", "java.lang.Process", "mvn exec", "exposeHostPorts(",
        "host.testcontainers.internal", "withAccessToHost(true)", "setHostAccessible(true)", "withNetworkMode(",
        "setNetworkMode(", "setPortBindings(", "withPortBindings(", "addFixedExposedPort(",
        "allocateFreePort", "ServerSocket",
        "withReuse(", "withFileSystemBind(", "addFileSystemBind(", "forHostPath(\"target\")",
        "Paths.get(\"target\")", "new File(\"target\")", "io.dapr.it.Command");
    for (Path directory : List.of(root.resolve("testcontainers/support"),
        root.resolve("testcontainers/methodinvoke/http"))) {
      try (var files = Files.walk(directory)) {
        for (Path file : files.filter(path -> path.toString().endsWith(".java"))
            .filter(path -> !path.getFileName().toString().equals("ArchitectureTest.java")).toList()) {
          String source = Files.readString(file);
          for (String token : forbidden) {
            assertFalse(source.contains(token), () -> file + " contains forbidden token " + token);
          }
          assertFalse(source.matches("(?s).*Path\\.of\\(\\s*\"target\"\\s*\\).*"),
              () -> file + " references the whole build directory");
          assertFalse(source.matches("(?s).*\\bCommand\\b.*"),
              () -> file + " references the legacy command helper");
        }
      }
    }
    for (String file : List.of("MethodInvokeIT.java", "MethodInvokeService.java", "MethodInvokeController.java",
        "Person.java")) {
      assertFalse(Files.exists(root.resolve("methodinvoke/http").resolve(file)));
    }
    Class<?> migrated = Class.forName("io.dapr.it.testcontainers.methodinvoke.http.MethodInvokeIT");
    assertEquals(5, java.util.Arrays.stream(migrated.getDeclaredMethods())
        .filter(method -> method.isAnnotationPresent(Test.class)).count());
  }
}
