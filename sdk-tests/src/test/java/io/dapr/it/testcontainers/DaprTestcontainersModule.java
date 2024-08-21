package io.dapr.it.testcontainers;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.Testcontainers;
import org.testcontainers.junit.jupiter.Container;

import java.util.Collections;

public interface DaprTestcontainersModule {

  @Container
  DaprContainer dapr = new DaprContainer("daprio/daprd:1.13.2")
      .withAppName("workflow-dapr-app")
      //Enable Workflows
      .withComponent(new Component("kvstore", "state.in-memory", "v1",
          Collections.singletonMap("actorStateStore", "true")))
      .withComponent(new Component("pubsub", "pubsub.in-memory", "v1", Collections.emptyMap()))
      .withAppPort(8080)
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withAppChannelAddress("host.testcontainers.internal");

  /**
   * Expose the Dapr ports to the host.
   *
   * @param registry the dynamic property registry
   */
  @DynamicPropertySource
  static void daprProperties(DynamicPropertyRegistry registry) {
    Testcontainers.exposeHostPorts(8080);
    dapr.start();
    registry.add("dapr.grpc.port", dapr::getGrpcPort);
    registry.add("dapr.http.port", dapr::getHttpPort);
  }

}
