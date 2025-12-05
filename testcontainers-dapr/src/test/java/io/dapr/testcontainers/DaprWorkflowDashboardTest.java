package io.dapr.testcontainers;

import org.junit.jupiter.api.Test;

import java.util.Collections;

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
            .withStateStoreComponent(stateStoreComponent)) {
      dashboard.configure();
      assertNotNull(dashboard.getEnvMap().get("COMPONENT_FILE"));
      assertFalse(dashboard.getEnvMap().get("COMPONENT_FILE").isEmpty());
    }
  }
}
