/*
 * Copyright 2024 The Dapr Authors
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

import io.dapr.testcontainers.converter.ComponentYamlConverter;
import io.dapr.testcontainers.converter.YamlMapperFactory;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;

public class DaprComponentTest {
  private final Yaml MAPPER = YamlMapperFactory.create();
  private final ComponentYamlConverter converter = new ComponentYamlConverter(MAPPER);

  @Test
  public void containerConfigurationTest() {
    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withDaprLogLevel(DaprLogLevel.DEBUG)
        .withAppChannelAddress("host.testcontainers.internal");

    dapr.configure();

    assertThrows(IllegalStateException.class, dapr::getHttpEndpoint);
    assertThrows(IllegalStateException.class, dapr::getGrpcPort);
  }

  @Test
  public void withComponentFromPath() {
    URL stateStoreYaml = this.getClass().getClassLoader().getResource("dapr-resources/statestore.yaml");
    Path path = Paths.get(stateStoreYaml.getPath());

    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withComponent(path)
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
        + "  type: state.redis\n"
        + "  version: v1\n"
        + "  metadata:\n"
        + "  - name: keyPrefix\n"
        + "    value: name\n"
        + "  - name: redisHost\n"
        + "    value: redis:6379\n"
        + "  - name: redisPassword\n"
        + "    value: ''\n";

    assertEquals(expectedComponentYaml, componentYaml);
  }
}
