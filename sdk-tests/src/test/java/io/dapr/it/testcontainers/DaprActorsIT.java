package io.dapr.it.testcontainers;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot.DaprController;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
    webEnvironment = WebEnvironment.DEFINED_PORT,
    classes = {
        TestActorsApplication.class,
        TestDaprActorsConfiguration.class,
        DaprAutoConfiguration.class,
            DaprController.class
    }
)
@Testcontainers
@Tag("testcontainers")
public class DaprActorsIT {
  private static final Network DAPR_NETWORK = Network.newNetwork();

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer("daprio/daprd:1.14.1")
      .withAppName("actor-dapr-app")
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component("kvstore", "state.in-memory", "v1",
          Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal");

  /**
   * Expose the Dapr ports to the host.
   *
   * @param registry the dynamic property registry
   */
  @DynamicPropertySource
  static void daprProperties(DynamicPropertyRegistry registry) {
    registry.add("dapr.http.endpoint", DAPR_CONTAINER::getHttpEndpoint);
    registry.add("dapr.grpc.endpoint", DAPR_CONTAINER::getGrpcEndpoint);
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
  }

  @Autowired
  private ActorClient daprActorClient;


  @Test
  public void testActors() throws Exception {
    ActorProxyBuilder<TestActor> builder = new ActorProxyBuilder<>(TestActor.class, daprActorClient);
    ActorId actorId = ActorId.createRandom();
    TestActor actor = builder.build(actorId);

    String message = UUID.randomUUID().toString();

    String echoedMessage = actor.echo(message);

    assertEquals(echoedMessage, message);
  }
}
