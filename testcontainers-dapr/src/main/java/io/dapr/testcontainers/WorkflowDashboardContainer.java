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

import io.dapr.testcontainers.converter.ComponentYamlConverter;
import io.dapr.testcontainers.converter.YamlConverter;
import io.dapr.testcontainers.converter.YamlMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.yaml.snakeyaml.Yaml;

/**
 * Test container for Dapr Workflow Dashboard.
 */
public class WorkflowDashboardContainer extends GenericContainer<WorkflowDashboardContainer> {
  private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowDashboardContainer.class);
  private static final Yaml YAML_MAPPER = YamlMapperFactory.create();
  private static final YamlConverter<Component> COMPONENT_CONVERTER = new ComponentYamlConverter(YAML_MAPPER);
  public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName
      .parse("public.ecr.aws/diagrid-dev/diagrid-dashboard:latest");
  private int dashboardPort = 8080;
  private Component stateStoreComponent;

  /**
   * Creates a new Dapr scheduler container.
   * @param dockerImageName Docker image name.
   */
  public WorkflowDashboardContainer(DockerImageName dockerImageName) {
    super(dockerImageName);
    dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    withExposedPorts(dashboardPort);
  }

  public WorkflowDashboardContainer withStateStoreComponent(Component stateStoreComponent) {
    this.stateStoreComponent = stateStoreComponent;
    return this;
  }

  /**
   * Creates a new Dapr schedulers container.
   * @param image Docker image name.
   */
  public WorkflowDashboardContainer(String image) {
    this(DockerImageName.parse(image));
  }

  @Override
  protected void configure() {
    super.configure();
    if (stateStoreComponent != null) {
      String componentYaml = COMPONENT_CONVERTER.convert(stateStoreComponent);
      withCopyToContainer(Transferable.of(componentYaml), "/app/components/" + stateStoreComponent.getName() + ".yaml");
      withEnv("COMPONENT_FILE", "/app/components/" + stateStoreComponent.getName() + ".yaml");
    }

  }

  public static DockerImageName getDefaultImageName() {
    return DEFAULT_IMAGE_NAME;
  }

  public WorkflowDashboardContainer withPort(Integer port) {
    this.dashboardPort = port;
    return this;
  }

  @Override
  public void start() {
    super.start();

    LOGGER.info("Dapr Workflow Dashboard container started.");
    LOGGER.info("Access the Dashboard at: http://localhost:{}", this.getMappedPort(dashboardPort));
  }

  public int getPort() {
    return dashboardPort;
  }

  // Required by spotbugs plugin
  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
