package io.dapr.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DaprContainerTest {

  @Test
  public void schedulerAndPlacementCustomImagesTest() {

    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("dapr-app")
            .withSchedulerImage(DockerImageName.parse("custom/scheduler:1.15.4")
                    .asCompatibleSubstituteFor("daprio/scheduler:1.15.4"))
            .withPlacementImage(DockerImageName.parse("custom/placement:1.15.4")
                    .asCompatibleSubstituteFor("daprio/placement:1.15.4"))
            .withAppPort(8081)
            .withDaprLogLevel(DaprLogLevel.DEBUG)
            .withAppChannelAddress("host.testcontainers.internal");


    assertEquals("custom/placement:1.15.4", dapr.getPlacementDockerImageName().asCanonicalNameString());
    assertEquals("custom/scheduler:1.15.4", dapr.getSchedulerDockerImageName().asCanonicalNameString());

  }

  @Test
  public void schedulerAndPlacementCustomImagesStringTest() {

    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("dapr-app")
            .withSchedulerImage("daprio/scheduler:1.15.4")
            .withPlacementImage("daprio/placement:1.15.4")
            .withAppPort(8081)
            .withDaprLogLevel(DaprLogLevel.DEBUG)
            .withAppChannelAddress("host.testcontainers.internal");


    assertEquals("daprio/placement:1.15.4", dapr.getPlacementDockerImageName().asCanonicalNameString());
    assertEquals("daprio/scheduler:1.15.4", dapr.getSchedulerDockerImageName().asCanonicalNameString());

  }
}
