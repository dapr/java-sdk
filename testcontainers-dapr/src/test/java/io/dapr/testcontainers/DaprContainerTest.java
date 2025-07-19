package io.dapr.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static io.dapr.testcontainers.DaprContainerConstants.DAPR_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DaprContainerTest {

  @Test
  public void schedulerAndPlacementCustomImagesTest() {

    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("dapr-app")
            .withSchedulerImage(DockerImageName.parse("custom/scheduler:" + DAPR_VERSION)
                    .asCompatibleSubstituteFor("daprio/scheduler:" + DAPR_VERSION))
            .withPlacementImage(DockerImageName.parse("custom/placement:"+ DAPR_VERSION)
                    .asCompatibleSubstituteFor("daprio/placement:" + DAPR_VERSION))
            .withAppPort(8081)
            .withDaprLogLevel(DaprLogLevel.DEBUG)
            .withAppChannelAddress("host.testcontainers.internal");


    assertEquals("custom/placement:" + DAPR_VERSION, dapr.getPlacementDockerImageName().asCanonicalNameString());
    assertEquals("custom/scheduler:" + DAPR_VERSION, dapr.getSchedulerDockerImageName().asCanonicalNameString());

  }

  @Test
  public void schedulerAndPlacementCustomImagesStringTest() {

    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("dapr-app")
            .withSchedulerImage("daprio/scheduler:" + DAPR_VERSION)
            .withPlacementImage("daprio/placement:"+ DAPR_VERSION)
            .withAppPort(8081)
            .withDaprLogLevel(DaprLogLevel.DEBUG)
            .withAppChannelAddress("host.testcontainers.internal");


    assertEquals("daprio/placement:" + DAPR_VERSION, dapr.getPlacementDockerImageName().asCanonicalNameString());
    assertEquals("daprio/scheduler:" + DAPR_VERSION, dapr.getSchedulerDockerImageName().asCanonicalNameString());

  }
}
