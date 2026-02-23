/*
 * Copyright 2021 The Dapr Authors
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

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.config.Properties;
import io.dapr.it.testcontainers.ContainerConstants;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.DaprPlacementContainer;
import io.dapr.testcontainers.DaprSchedulerContainer;
import io.dapr.testcontainers.wait.strategy.DaprWait;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Testcontainers
public class ActorReminderFailoverIT {

  private static final Logger logger = LoggerFactory.getLogger(ActorReminderFailoverIT.class);

  private static final String ACTOR_TYPE = "MyActorTest";
  private static final int APP_PORT = 8080;
  private static final String CONTAINER_CLASSPATH = prepareContainerClasspath();
  private static final String FIRST_ACTOR_IDENTIFIER = "4111";
  private static final String SECOND_ACTOR_IDENTIFIER = "4222";
  private static final Network DAPR_NETWORK = io.dapr.it.testcontainers.TestContainerNetworks.SHARED_NETWORK;

  @Container
  private static final DaprPlacementContainer SHARED_PLACEMENT_CONTAINER = new DaprPlacementContainer(
      io.dapr.testcontainers.DaprContainerConstants.DAPR_PLACEMENT_IMAGE_TAG)
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("placement")
      .withReuse(false);

  @Container
  private static final DaprSchedulerContainer SHARED_SCHEDULER_CONTAINER = new DaprSchedulerContainer(
      io.dapr.testcontainers.DaprContainerConstants.DAPR_SCHEDULER_IMAGE_TAG)
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("scheduler")
      .withReuse(false);

  @Container
  private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("redis");

  @Container
  private static final GenericContainer<?> FIRST_ACTOR_APP = new GenericContainer<>(ContainerConstants.JDK_17_TEMURIN_JAMMY)
      .withCopyFileToContainer(MountableFile.forHostPath("target"), "/app")
      .withWorkingDirectory("/app")
      .withCommand(
          "java", "-cp", CONTAINER_CLASSPATH,
          "io.dapr.it.actors.app.MyActorService", Integer.toString(APP_PORT))
      .withEnv("DAPR_HTTP_PORT", FIRST_ACTOR_IDENTIFIER)
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("actor-app-one")
      .waitingFor(Wait.forLogMessage(".*Started MyActorService.*", 1))
      .dependsOn(REDIS);

  @Container
  private static final GenericContainer<?> SECOND_ACTOR_APP = new GenericContainer<>(ContainerConstants.JDK_17_TEMURIN_JAMMY)
      .withCopyFileToContainer(MountableFile.forHostPath("target"), "/app")
      .withWorkingDirectory("/app")
      .withCommand(
          "java", "-cp", CONTAINER_CLASSPATH,
          "io.dapr.it.actors.app.MyActorService", Integer.toString(APP_PORT))
      .withEnv("DAPR_HTTP_PORT", SECOND_ACTOR_IDENTIFIER)
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("actor-app-two")
      .waitingFor(Wait.forLogMessage(".*Started MyActorService.*", 1))
      .dependsOn(REDIS);

  @Container
  private static final DaprContainer FIRST_DAPR_CONTAINER = new DaprContainer(ContainerConstants.DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("actor-reminder-failover-one")
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("actor-dapr-one")
      .withPlacementContainer(SHARED_PLACEMENT_CONTAINER)
      .withSchedulerContainer(SHARED_SCHEDULER_CONTAINER)
      .withAppPort(APP_PORT)
      .withAppChannelAddress("actor-app-one")
      .withComponent(new Component(
          "statestore",
          "state.redis",
          "v1",
          Map.of("redisHost", "redis:6379", "redisPassword", "", "actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .dependsOn(SHARED_PLACEMENT_CONTAINER, SHARED_SCHEDULER_CONTAINER, REDIS, FIRST_ACTOR_APP)
      .withLogConsumer(outputFrame -> logger.info("ACTOR_DAPR_ONE {}", outputFrame.getUtf8String()));

  @Container
  private static final DaprContainer SECOND_DAPR_CONTAINER = new DaprContainer(ContainerConstants.DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("actor-reminder-failover-two")
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("actor-dapr-two")
      .withPlacementContainer(SHARED_PLACEMENT_CONTAINER)
      .withSchedulerContainer(SHARED_SCHEDULER_CONTAINER)
      .withAppPort(APP_PORT)
      .withAppChannelAddress("actor-app-two")
      .withComponent(new Component(
          "statestore",
          "state.redis",
          "v1",
          Map.of("redisHost", "redis:6379", "redisPassword", "", "actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .dependsOn(SHARED_PLACEMENT_CONTAINER, SHARED_SCHEDULER_CONTAINER, REDIS, SECOND_ACTOR_APP)
      .withLogConsumer(outputFrame -> logger.info("ACTOR_DAPR_TWO {}", outputFrame.getUtf8String()));

  @Container
  private static final DaprContainer CLIENT_DAPR_CONTAINER = new DaprContainer(ContainerConstants.DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("actor-reminder-failover-client")
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("actor-dapr-client")
      .withPlacementContainer(SHARED_PLACEMENT_CONTAINER)
      .withSchedulerContainer(SHARED_SCHEDULER_CONTAINER)
      .withComponent(new Component(
          "statestore",
          "state.redis",
          "v1",
          Map.of("redisHost", "redis:6379", "redisPassword", "", "actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .dependsOn(SHARED_PLACEMENT_CONTAINER, SHARED_SCHEDULER_CONTAINER, REDIS, FIRST_DAPR_CONTAINER, SECOND_DAPR_CONTAINER)
      .withLogConsumer(outputFrame -> logger.info("ACTOR_DAPR_CLIENT {}", outputFrame.getUtf8String()));

  private ActorProxy proxy;
  private ActorClient actorClient;

  @BeforeEach
  public void init() {
    DaprWait.forActors().waitUntilReady(FIRST_DAPR_CONTAINER);
    DaprWait.forActors().waitUntilReady(SECOND_DAPR_CONTAINER);

    this.actorClient = new ActorClient(new Properties(Map.of(
        "dapr.http.endpoint", CLIENT_DAPR_CONTAINER.getHttpEndpoint(),
        "dapr.grpc.endpoint", CLIENT_DAPR_CONTAINER.getGrpcEndpoint()
    )));
    ActorId actorId = new ActorId(UUID.randomUUID().toString());
    ActorProxyBuilder<ActorProxy> proxyBuilder = new ActorProxyBuilder<>(ACTOR_TYPE, ActorProxy.class, actorClient);
    this.proxy = proxyBuilder.build(actorId);
  }

  @AfterEach
  public void tearDown() {
    if (actorClient != null) {
      actorClient.close();
    }
  }

  /**
   * Create an actor, register a reminder, validates its content, restarts the runtime and confirms reminder continues.
   * @throws Exception This test is not expected to throw.  Thrown exceptions are bugs.
   */
  @Test
  public void reminderRecoveryTest() throws Exception {
    final String[] warmUpResponse = new String[1];
    callWithRetry(() -> warmUpResponse[0] = proxy.invokeMethod("say", "warm-up", String.class).block(), 60000);
    assertNotNull(warmUpResponse[0]);

    int originalActorHostIdentifier = Integer.parseInt(
        proxy.invokeMethod("getIdentifier", String.class).block());
    if (originalActorHostIdentifier == Integer.parseInt(FIRST_ACTOR_IDENTIFIER)) {
      FIRST_DAPR_CONTAINER.stop();
      FIRST_ACTOR_APP.stop();
    } else if (originalActorHostIdentifier == Integer.parseInt(SECOND_ACTOR_IDENTIFIER)) {
      SECOND_DAPR_CONTAINER.stop();
      SECOND_ACTOR_APP.stop();
    }

    logger.debug("Pausing 10 seconds to allow failover to take place");
    Thread.sleep(10000);
    final String[] postFailoverResponse = new String[1];
    callWithRetry(() -> postFailoverResponse[0] = proxy.invokeMethod("say", "after-failover", String.class).block(), 60000);
    assertNotNull(postFailoverResponse[0]);

    int newActorHostIdentifier = Integer.parseInt(
        proxy.invokeMethod("getIdentifier", String.class).block());
    assertNotEquals(originalActorHostIdentifier, newActorHostIdentifier);
  }

  private static String prepareContainerClasspath() {
    Path dependencyDirectory = Path.of("target", "dependency");
    try {
      Files.createDirectories(dependencyDirectory);
      String[] classPathEntries = System.getProperty("java.class.path", "").split(File.pathSeparator);
      for (String classPathEntry : classPathEntries) {
        Path source = Path.of(classPathEntry);
        if (Files.isRegularFile(source) && classPathEntry.endsWith(".jar")) {
          Path target = dependencyDirectory.resolve(source.getFileName());
          Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to prepare dependency jars for actor app containers", e);
    }

    return "test-classes:classes:dependency/*";
  }
}
