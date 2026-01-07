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

package io.dapr.testcontainers;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DaprWorkflowDashboardTest {

  @Test
  public void dashboardTest() {
    Component stateStoreComponent = new Component("kvstore",
        "state.in-memory", "v1", Collections.singletonMap("actorStateStore", "true"));
    try (WorkflowDashboardContainer dashboard =
        new WorkflowDashboardContainer(WorkflowDashboardContainer.DEFAULT_IMAGE_NAME)
            .withStateStoreComponent(stateStoreComponent).withPort(8080)) {
      dashboard.configure();
      assertNotNull(dashboard.getEnvMap().get("COMPONENT_FILE"));
      assertFalse(dashboard.getEnvMap().get("COMPONENT_FILE").isEmpty());
      assertEquals(8080, dashboard.getPort());
      assertEquals(WorkflowDashboardContainer.DEFAULT_IMAGE_NAME, dashboard.getDefaultImageName());
      assertEquals(8080, dashboard.getExposedPorts().get(0));
      assertEquals(stateStoreComponent, dashboard.getStateStoreComponent());
    }
  }
}
