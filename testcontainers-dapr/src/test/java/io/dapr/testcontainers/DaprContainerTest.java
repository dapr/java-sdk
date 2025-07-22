package io.dapr.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static io.dapr.testcontainers.DaprContainerConstants.DAPR_VERSION;
import static org.junit.jupiter.api.Assertions.*;

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

  @Test
  public void appHealthParametersTest(){
    DaprContainer dapr = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("dapr-app")
            .withAppPort(8081)
            .withAppHealthCheckProbeInterval(10)
            .withAppHealthCheckProbeTimeout(600)
            .withAppHealthCheckThreshold(7);

    assertEquals(10, dapr.getAppHealthCheckProbeInterval());
    assertEquals(600, dapr.getAppHealthCheckProbeTimeout());
    assertEquals(7, dapr.getAppHealthCheckThreshold());


    //Check that the defaults are set by default
    DaprContainer dapr2 = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("dapr2-app")
            .withAppPort(8082);

    assertEquals(5, dapr2.getAppHealthCheckProbeInterval());
    assertEquals(500, dapr2.getAppHealthCheckProbeTimeout());
    assertEquals(3, dapr2.getAppHealthCheckThreshold());


  }
}
