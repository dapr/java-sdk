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

package io.dapr.it;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppRunOverrideTest {

  /**
   * Verifies that when we construct AppRun with explicit Dapr port overrides,
   * the DAPR_HTTP_PORT / DAPR_GRPC_PORT env vars on the spawned command point
   * at the override values, not at the DaprPorts-allocated ones.
   */
  @Test
  void daprPortOverridesAreUsedInEnv() throws Exception {
    DaprPorts ports = DaprPorts.build(true, true, true);
    AppRun app = new AppRun(ports, "ready", Object.class, 1000, 12345, 67890);

    Field commandField = AppRun.class.getDeclaredField("command");
    commandField.setAccessible(true);
    Command command = (Command) commandField.get(app);

    Field envField = Command.class.getDeclaredField("env");
    envField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, String> env = (Map<String, String>) envField.get(command);

    assertEquals("12345", env.get("DAPR_HTTP_PORT"));
    assertEquals("67890", env.get("DAPR_GRPC_PORT"));
  }
}
