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

import io.dapr.config.Properties;
import org.junit.jupiter.api.Test;
import org.testcontainers.lifecycle.Startable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoundationTest {
  @Test
  void endpointsDistinguishNetworkAndMappedHosts() {
    assertEquals("http://dapr:3500", Endpoints.http("dapr", 3500).toString());
    assertEquals("http://docker.example:12345", Endpoints.http("docker.example", 12345).toString());
    assertEquals("http://[::1]:12345", Endpoints.http("::1", 12345).toString());
    var overrides = Endpoints.overrides("docker.example", 12345, 23456);
    assertEquals("http://docker.example:12345", overrides.get(Properties.HTTP_ENDPOINT));
    assertEquals("docker.example:23456", overrides.get(Properties.GRPC_ENDPOINT));
    assertEquals("[::1]:23456", Endpoints.overrides("::1", 12345, 23456).get(Properties.GRPC_ENDPOINT));
  }

  @Test
  void selectMatchingJvmOrExplicitImage() {
    for (int version : List.of(17, 21, 25)) {
      assertEquals("eclipse-temurin:" + version + "-jdk-jammy", JavaApplicationContainer.image(version, null));
    }
    assertEquals("example/java:custom", JavaApplicationContainer.image(99, "example/java:custom"));
    assertThrows(IllegalArgumentException.class, () -> JavaApplicationContainer.image(99, null));
    assertThrows(IllegalArgumentException.class, () -> JavaApplicationContainer.image(17, " "));
    assertThrows(IllegalArgumentException.class, () -> JavaApplicationContainer.image(17, "bad image name"));
  }

  @Test
  void rollbackStopsAttemptedChildrenAndNetworkInReverseOrderOnce() {
    List<String> events = new ArrayList<>();
    OwnedResources resources = new OwnedResources(() -> events.add("network stop"));
    resources.start(resource("placement", events, false));
    resources.start(resource("scheduler", events, false));
    assertThrows(IllegalStateException.class, () -> resources.start(resource("app", events, true)));
    resources.stop();
    resources.stop();
    assertEquals(List.of("placement start", "scheduler start", "app start", "app stop", "scheduler stop",
        "placement stop", "network stop"), events);
  }

  @Test
  void teardownContinuesWhenAChildFailsToStop() {
    List<String> events = new ArrayList<>();
    OwnedResources resources = new OwnedResources(() -> events.add("network stop"));
    resources.start(resource("placement", events, false));
    resources.start(new Startable() {
      @Override
      public void start() { }

      @Override
      public void stop() {
        events.add("app stop");
        throw new IllegalStateException("stop failure");
      }
    });
    assertThrows(IllegalStateException.class, resources::stop);
    resources.stop();
    assertEquals(List.of("placement start", "app stop", "placement stop", "network stop"), events);
  }

  @Test
  void teardownClosesNetworkEvenWhenAChildThrowsAnError() {
    List<String> events = new ArrayList<>();
    OwnedResources resources = new OwnedResources(() -> events.add("network stop"));
    resources.start(new Startable() {
      @Override
      public void start() { }

      @Override
      public void stop() {
        throw new AssertionError("stop failure");
      }
    });
    assertThrows(AssertionError.class, resources::stop);
    assertEquals(List.of("network stop"), events);
  }

  @Test
  void constructionDoesNotStartResourcesOrRequireStagedPayload() {
    DaprTestEnvironment environment = new DaprTestEnvironment("construction-test", "example.DoesNotExist");
    assertTrue(environment.identity().startsWith("construction-test-"));
  }

  private static Startable resource(String name, List<String> events, boolean fail) {
    return new Startable() {
      @Override
      public void start() {
        events.add(name + " start");
        if (fail) {
          throw new IllegalStateException("induced failure");
        }
      }

      @Override
      public void stop() {
        events.add(name + " stop");
      }
    };
  }
}
