package io.dapr.testcontainers.converter;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ComponentYamlConverterTest {
  private final Yaml MAPPER = YamlMapperFactory.create();
  private final ComponentYamlConverter converter = new ComponentYamlConverter(MAPPER);

  @Test
  public void testComponentToYaml() {
    DaprContainer dapr = new DaprContainer("daprio/daprd")
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withComponent(new Component(
            "statestore",
            "state.in-memory",
            "v1",
            Map.of("actorStateStore", "true")))
        .withAppChannelAddress("host.testcontainers.internal");

    Set<Component> components = dapr.getComponents();
    assertEquals(1, components.size());

    Component kvstore = components.iterator().next();
    assertFalse(kvstore.getMetadata().isEmpty());

    String componentYaml = converter.convert(kvstore);
    String expectedComponentYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Component\n"
        + "metadata:\n"
        + "  name: statestore\n"
        + "spec:\n"
        + "  type: state.in-memory\n"
        + "  version: v1\n"
        + "  metadata:\n"
        + "  - name: actorStateStore\n"
        + "    value: 'true'\n";

    assertEquals(expectedComponentYaml, componentYaml);
  }
}
