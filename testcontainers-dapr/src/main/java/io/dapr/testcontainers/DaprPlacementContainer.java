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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test container for Dapr placement service.
 */
public class DaprPlacementContainer extends GenericContainer<DaprPlacementContainer> {

  private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("daprio/placement");
  private int placementPort = 50005;

  /**
   * Creates a new Dapr placement container.
   * @param dockerImageName Docker image name.
   */
  public DaprPlacementContainer(DockerImageName dockerImageName) {
    super(dockerImageName);
    dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

    withExposedPorts(placementPort);
  }

  /**
   * Creates a new Dapr placement container.
   * @param image Docker image name.
   */
  public DaprPlacementContainer(String image) {
    this(DockerImageName.parse(image));
  }

  @Override
  protected void configure() {
    super.configure();
    withCommand("./placement", "-port", Integer.toString(placementPort));
  }

  public static DockerImageName getDefaultImageName() {
    return DEFAULT_IMAGE_NAME;
  }

  public DaprPlacementContainer withPort(Integer port) {
    this.placementPort = port;
    return this;
  }

  public int getPort() {
    return placementPort;
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
