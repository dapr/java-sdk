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

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;

/**
 * Test container for Dapr scheduler service.
 */
public class DaprSchedulerContainer extends GenericContainer<DaprSchedulerContainer> {

  private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("daprio/scheduler");
  private int schedulerPort = 51005;

  /**
   * Creates a new Dapr scheduler container.
   * @param dockerImageName Docker image name.
   */
  public DaprSchedulerContainer(DockerImageName dockerImageName) {
    super(dockerImageName);
    dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    withExposedPorts(schedulerPort);
  }

  /**
   * Creates a new Dapr scheduler container.
   * @param image Docker image name.
   */
  public DaprSchedulerContainer(String image) {
    this(DockerImageName.parse(image));
  }

  @Override
  protected void configure() {
    super.configure();

    withCopyToContainer(Transferable.of("", 0777), "./default-dapr-scheduler-server-0/dapr-0.1/");
    withCopyToContainer(Transferable.of("", 0777), "./dapr-scheduler-existing-cluster/");
    withCommand("./scheduler", "--port", Integer.toString(schedulerPort), "--etcd-data-dir", ".");
  }

  public static DockerImageName getDefaultImageName() {
    return DEFAULT_IMAGE_NAME;
  }

  public DaprSchedulerContainer withPort(Integer port) {
    this.schedulerPort = port;
    return this;
  }

  public int getPort() {
    return schedulerPort;
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
