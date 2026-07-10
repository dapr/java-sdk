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
 * limitations under the License.
*/

package io.dapr.it.actors;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.client.DaprClient;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.config.Properties;
import io.dapr.config.Property;
import io.dapr.it.AppRun;
import io.dapr.it.actors.services.springboot.DemoActor;
import io.dapr.it.actors.services.springboot.DemoActorService;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test SDK resiliency.
 */
public class ActorSdkResiliencyIT extends BaseContainerIT {

  private static final ActorId ACTOR_ID = new ActorId(UUID.randomUUID().toString());

  private static final int NUM_ITERATIONS = 20;

  private static final Duration TIMEOUT = Duration.ofMillis(1000);

  private static final Duration LATENCY = TIMEOUT.dividedBy(2);

  private static final Duration JITTER = TIMEOUT.multipliedBy(2);

  private static final int MAX_RETRIES = -1;  // Infinity

  private static DaprContainer dapr;

  private static AppRun app;

  private static DaprClient daprClient;

  private static DemoActor demoActor;

  private static ToxiproxyContainer toxiproxy;

  private static DemoActor toxiDemoActor;

  private static DemoActor resilientDemoActor;

  private static DemoActor oneRetryDemoActor;

  @BeforeAll
  public static void init() throws Exception {
    var pair = startAppAndAttach(
        "actor-sdk-resiliency-it",
        DemoActorService.class,
        AppRun.AppProtocol.HTTP,
        appPort -> daprBuilder("actor-sdk-resiliency-it")
            .withNetworkAliases("dapr")
            .withAppPort(appPort)
            .withAppChannelAddress("host.testcontainers.internal")
            .withComponent(redisStateStore(STATE_STORE_NAME)));
    dapr = pair.dapr();
    app = pair.app();
    waitForActorsReady(dapr);

    demoActor = buildDemoActorProxy(newActorClient(dapr));
    daprClient = deferClose(newDaprClient(dapr));

    toxiproxy = newToxiproxy();
    ToxiproxyClient toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
    Proxy grpcProxy = toxiproxyClient.createProxy("dapr_grpc", "0.0.0.0:8666", "dapr:50001");
    grpcProxy.toxics()
        .latency("latency", ToxicDirection.DOWNSTREAM, LATENCY.toMillis())
        .setJitter(JITTER.toMillis());

    toxiDemoActor = buildDemoActorProxy(
        newActorClientViaProxy(new ResiliencyOptions().setTimeout(TIMEOUT)));
    resilientDemoActor = buildDemoActorProxy(
        newActorClientViaProxy(new ResiliencyOptions().setTimeout(TIMEOUT).setMaxRetries(MAX_RETRIES)));
    oneRetryDemoActor = buildDemoActorProxy(
        newActorClientViaProxy(new ResiliencyOptions().setTimeout(TIMEOUT).setMaxRetries(1)));
  }

  private static ActorClient newActorClientViaProxy(ResiliencyOptions resiliencyOptions) {
    Map<Property<?>, String> overrides = new HashMap<>();
    int mappedPort = toxiproxy.getMappedPort(8666);
    overrides.put(Properties.GRPC_ENDPOINT, "127.0.0.1:" + mappedPort);
    overrides.put(Properties.GRPC_PORT, String.valueOf(mappedPort));
    ActorClient client = new ActorClient(new Properties(overrides), resiliencyOptions);
    deferClose(client);
    return client;
  }

  private static DemoActor buildDemoActorProxy(ActorClient actorClient) {
    ActorProxyBuilder<DemoActor> builder = new ActorProxyBuilder(DemoActor.class, actorClient);
    return builder.build(ACTOR_ID);
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
}
