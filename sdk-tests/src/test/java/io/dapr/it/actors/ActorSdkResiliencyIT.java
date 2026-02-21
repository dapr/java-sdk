/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.it.actors;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.config.Properties;
import io.dapr.it.actors.services.springboot.DaprApplication;
import io.dapr.it.actors.services.springboot.DemoActor;
import io.dapr.it.testcontainers.actors.TestDaprActorsConfiguration;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.internal.DaprContainerFactory;
import io.dapr.testcontainers.internal.DaprSidecarContainer;
import io.dapr.testcontainers.internal.spring.DaprSpringBootTest;
import io.dapr.testcontainers.wait.strategy.DaprWait;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.dapr.it.testcontainers.ContainerConstants.TOXI_PROXY_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test SDK resiliency.
 */
@DaprSpringBootTest(classes = {
    DaprApplication.class,
    TestDaprActorsConfiguration.class,
    DemoActorRuntimeRegistrationConfiguration.class
})
public class ActorSdkResiliencyIT {

  private static final Logger logger = LoggerFactory.getLogger(ActorSdkResiliencyIT.class);

  private static final ActorId ACTOR_ID = new ActorId(UUID.randomUUID().toString());

  private static final int NUM_ITERATIONS = 20;

  private static final Duration TIMEOUT = Duration.ofMillis(1000);

  private static final Duration LATENCY = TIMEOUT.dividedBy(2);

  private static final Duration JITTER = TIMEOUT.multipliedBy(2);

  private static final int MAX_RETRIES = -1;  // Infinity

  private static final Network NETWORK = Network.newNetwork();

  private static final int GRPC_PROXY_PORT = 8666;
  private static final int HTTP_PROXY_PORT = 8667;

  @DaprSidecarContainer
  private static final DaprContainer DAPR_CONTAINER = DaprContainerFactory.createForSpringBootTest("actor-sdk-resiliency-it")
      .withNetwork(NETWORK)
      .withNetworkAliases("dapr")
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> logger.info(outputFrame.getUtf8String()));

  @Container
  private static final ToxiproxyContainer TOXIPROXY = new ToxiproxyContainer(TOXI_PROXY_IMAGE_TAG)
      .withNetwork(NETWORK);

  private static Proxy grpcProxy;
  private static Proxy httpProxy;

  private static ActorClient actorClient;
  private static ActorClient toxiActorClient;
  private static ActorClient resilientActorClient;
  private static ActorClient oneRetryActorClient;

  private static DemoActor demoActor;
  private static DemoActor toxiDemoActor;
  private static DemoActor resilientDemoActor;
  private static DemoActor oneRetryDemoActor;

  @BeforeAll
  public static void init() throws IOException {
    ToxiproxyClient toxiproxyClient = new ToxiproxyClient(TOXIPROXY.getHost(), TOXIPROXY.getControlPort());
    grpcProxy = toxiproxyClient.createProxy("dapr-grpc", "0.0.0.0:" + GRPC_PROXY_PORT, "dapr:50001");
    httpProxy = toxiproxyClient.createProxy("dapr-http", "0.0.0.0:" + HTTP_PROXY_PORT, "dapr:3500");
  }

  @BeforeEach
  public void setUp() throws Exception {
    org.testcontainers.Testcontainers.exposeHostPorts(DAPR_CONTAINER.getAppPort());
    DaprWait.forActorType("DemoActorTest").waitUntilReady(DAPR_CONTAINER);

    removeToxics(grpcProxy);
    removeToxics(httpProxy);

    grpcProxy.toxics().latency("latency-grpc", ToxicDirection.DOWNSTREAM, LATENCY.toMillis())
        .setJitter(JITTER.toMillis());
    httpProxy.toxics().latency("latency-http", ToxicDirection.DOWNSTREAM, LATENCY.toMillis())
        .setJitter(JITTER.toMillis());

    actorClient = new ActorClient(new Properties(Map.of(
        "dapr.http.endpoint", DAPR_CONTAINER.getHttpEndpoint(),
        "dapr.grpc.endpoint", DAPR_CONTAINER.getGrpcEndpoint()
    )));
    toxiActorClient = newActorClientViaToxiProxy(new ResiliencyOptions().setTimeout(TIMEOUT));
    resilientActorClient = newActorClientViaToxiProxy(new ResiliencyOptions().setTimeout(TIMEOUT).setMaxRetries(MAX_RETRIES));
    oneRetryActorClient = newActorClientViaToxiProxy(new ResiliencyOptions().setTimeout(TIMEOUT).setMaxRetries(1));

    demoActor = buildDemoActorProxy(actorClient);
    toxiDemoActor = buildDemoActorProxy(toxiActorClient);
    resilientDemoActor = buildDemoActorProxy(resilientActorClient);
    oneRetryDemoActor = buildDemoActorProxy(oneRetryActorClient);
  }

  private static DemoActor buildDemoActorProxy(ActorClient actorClient) {
    ActorProxyBuilder<DemoActor> builder = new ActorProxyBuilder<>(DemoActor.class, actorClient);
    return builder.build(ACTOR_ID);
  }

  @AfterAll
  public static void tearDown() throws Exception {
    closeQuietly(oneRetryActorClient);
    closeQuietly(resilientActorClient);
    closeQuietly(toxiActorClient);
    closeQuietly(actorClient);
  }

  @Test
  @Disabled("Flaky when running on GitHub actions")
  public void retryAndTimeout() {
    AtomicInteger toxiClientErrorCount = new AtomicInteger();
    AtomicInteger retryOneClientErrorCount = new AtomicInteger();
    String message = "hello world";
    for (int i = 0; i < NUM_ITERATIONS; i++) {
      try {
        toxiDemoActor.writeMessage(message);
      } catch (Exception e) {
        // This call should fail sometimes. So, we count.
        toxiClientErrorCount.incrementAndGet();
      }
      try {
        oneRetryDemoActor.writeMessage(message);
      } catch (Exception e) {
        // This call should fail sometimes. So, we count.
        retryOneClientErrorCount.incrementAndGet();
      }

      // We retry forever so that the call below should always work.
      resilientDemoActor.writeMessage(message);
      // Makes sure the value was actually saved.
      String savedValue = demoActor.readMessage();
      assertEquals(message, savedValue);
    }

    // This assertion makes sure that toxicity is on
    assertTrue(toxiClientErrorCount.get() > 0);
    assertTrue(retryOneClientErrorCount.get() > 0);
    // A client without retries should have more errors than a client with one retry.
    assertTrue(toxiClientErrorCount.get() > retryOneClientErrorCount.get());
  }

  private static ActorClient newActorClientViaToxiProxy(ResiliencyOptions resiliencyOptions) {
    return new ActorClient(new Properties(Map.of(
        "dapr.http.endpoint", "http://localhost:" + TOXIPROXY.getMappedPort(HTTP_PROXY_PORT),
        "dapr.grpc.endpoint", "localhost:" + TOXIPROXY.getMappedPort(GRPC_PROXY_PORT)
    )), resiliencyOptions);
  }

  private static void removeToxics(Proxy proxy) {
    try {
      proxy.toxics().getAll().forEach(toxic -> {
        try {
          toxic.remove();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void closeQuietly(ActorClient client) {
    if (client != null) {
      client.close();
    }
  }
}
