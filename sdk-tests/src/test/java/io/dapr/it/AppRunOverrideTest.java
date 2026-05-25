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
