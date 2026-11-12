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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Runs a staged test application directly on a matching JVM. */
public final class JavaApplicationContainer extends GenericContainer<JavaApplicationContainer> {
  private static final String DESTINATION = "/it-runtime";
  private final String mainClass;
  private final int port;

  /** Defines a container without reading or generating the runtime payload. */
  public JavaApplicationContainer(String mainClass, int port) {
    this(mainClass, port, Duration.ofMinutes(2));
  }

  JavaApplicationContainer(String mainClass, int port, Duration readinessTimeout) {
    super(DockerImageName.parse(image(Runtime.version().feature(), System.getProperty("dapr.it.java.image"))));
    this.mainClass = mainClass;
    this.port = port;
    withExposedPorts(port);
    waitingFor(Wait.forHttp("/health").forPort(port).forStatusCode(200).withStartupTimeout(readinessTimeout));
  }

  static String image(int major, String override) {
    if (override != null) {
      if (override.isBlank()) {
        throw new IllegalArgumentException("dapr.it.java.image must not be blank");
      }
      DockerImageName.parse(override).assertValid();
      return override;
    }
    if (major != 17 && major != 21 && major != 25) {
      throw new IllegalArgumentException("Unsupported test JVM " + major + "; set -Ddapr.it.java.image explicitly");
    }
    return "eclipse-temurin:" + major + "-jdk-jammy";
  }

  @Override
  protected void configure() {
    super.configure();
    Path runtime = Path.of("target", "it-runtime").toAbsolutePath();
    Path lib = runtime.resolve("lib");
    if (!Files.isDirectory(runtime.resolve("classes")) || !Files.isDirectory(runtime.resolve("test-classes"))
        || !Files.isDirectory(lib)) {
      throw new IllegalStateException("Missing staged IT runtime at " + runtime + "; run Maven verify");
    }
    List<String> classpath = new ArrayList<>();
    classpath.add(DESTINATION + "/test-classes");
    classpath.add(DESTINATION + "/classes");
    try (var files = Files.walk(lib)) {
      classpath.addAll(files.filter(path -> path.toString().endsWith(".jar"))
          .sorted().map(path -> DESTINATION + "/lib/" + lib.relativize(path).toString().replace('\\', '/'))
          .collect(Collectors.toList()));
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read staged IT dependencies", e);
    }
    withCopyToContainer(MountableFile.forHostPath(runtime), DESTINATION);
    withCommand("java", "-cp", String.join(":", classpath), mainClass, Integer.toString(port));
  }
}
