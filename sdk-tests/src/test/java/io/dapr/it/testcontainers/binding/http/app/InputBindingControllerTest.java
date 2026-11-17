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
package io.dapr.it.testcontainers.binding.http.app;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputBindingControllerTest {
  @Test
  void probeResetAndSnapshotDoNotLoseOrExposeMessages() {
    InputBindingController controller = new InputBindingController();
    controller.handleInputBinding("\"ping\"");
    assertTrue(controller.initialized());
    assertTrue(controller.getMessages().isEmpty());
    controller.handleInputBinding("\"cat\"");
    List<String> snapshot = controller.getMessages();
    controller.reset();
    assertFalse(controller.initialized());
    assertTrue(controller.getMessages().isEmpty());
    assertEquals(List.of("\"cat\""), snapshot);
    assertEquals(404, controller.upstreamNotFound().getStatusCode().value());
    assertEquals(Map.of("marker", "binding-fixture-not-found"), controller.upstreamNotFound().getBody());
  }
}
