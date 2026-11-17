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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OwnedResourcesTest {
  @Test
  void rollsBackFailedStartInReverseOrderAndClosesNetworkLast() {
    List<String> events = new ArrayList<>();
    OwnedResources resources = new OwnedResources(() -> events.add("network stop"));
    resources.start(resource("kafka", false, events));
    resources.start(resource("app", false, events));
    assertThrows(IllegalStateException.class, () -> resources.start(resource("dapr", true, events)));
    resources.stop();
    resources.stop();
    assertEquals(List.of("kafka start", "app start", "dapr start", "dapr stop", "app stop",
        "kafka stop", "network stop"), events);
  }

  @Test
  void rollsBackFailedPrerequisite() {
    List<String> events = new ArrayList<>();
    OwnedResources resources = new OwnedResources(() -> events.add("network stop"));
    assertThrows(IllegalStateException.class, () -> resources.start(resource("kafka", true, events)));
    resources.stop();
    assertEquals(List.of("kafka start", "kafka stop", "network stop"), events);
  }

  @Test
  void rejectsMissingAliasAndDuplicatePrerequisite() {
    DaprTestEnvironment environment = new DaprTestEnvironment("ownership-unit", getClass());
    GenericContainer<?> dependency = new GenericContainer<>("alpine:3.20");
    assertThrows(IllegalArgumentException.class, () -> environment.withPrerequisite(dependency, "", List.of()));
    environment.withPrerequisite(dependency, "dependency", List.of());
    assertThrows(IllegalStateException.class, () -> environment.withPrerequisite(dependency, "dependency", List.of()));
  }

  private static Startable resource(String name, boolean fail, List<String> events) {
    return new Startable() {
      public void start() {
        events.add(name + " start");
        if (fail) {
          throw new IllegalStateException(name);
        }
      }

      public void stop() {
        events.add(name + " stop");
      }
    };
  }
}
