package io.dapr.it.spring.data;

import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public class CustomMySQLContainer extends MySQLContainer {

  public CustomMySQLContainer(String dockerImageName) {
    super(DockerImageName.parse(dockerImageName));
  }

  protected void waitUntilContainerStarted() {
    this.getWaitStrategy().waitUntilReady(this);
  }
}
