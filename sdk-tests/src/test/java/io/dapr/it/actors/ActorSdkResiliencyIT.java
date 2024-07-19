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

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.ToxiProxyRun;
import io.dapr.it.actors.services.springboot.DemoActor;
import io.dapr.it.actors.services.springboot.DemoActorService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test SDK resiliency.
 */
public class ActorSdkResiliencyIT extends BaseIT {

  private static final ActorId ACTOR_ID = new ActorId(UUID.randomUUID().toString());

  private static final int NUM_ITERATIONS = 20;

  private static final Duration TIMEOUT = Duration.ofMillis(1000);

  private static final Duration LATENCY = TIMEOUT.dividedBy(2);

  private static final Duration JITTER = TIMEOUT.multipliedBy(2);

  private static final int MAX_RETRIES = -1;  // Infinity

  private static DaprRun daprRun;

  private static DaprClient daprClient;

  private static DemoActor demoActor;

  private static ToxiProxyRun toxiProxyRun;

  private static DemoActor toxiDemoActor;

  private static DemoActor resilientDemoActor;

  private static DemoActor oneRetryDemoActor;

  @BeforeAll
  public static void init() throws Exception {
    daprRun = startDaprApp(
            ActorSdkResiliencyIT.class.getSimpleName(),
            DemoActorService.SUCCESS_MESSAGE,
            DemoActorService.class,
            true,
            60000);

    demoActor = buildDemoActorProxy(null);
    daprClient = new DaprClientBuilder().build();

    toxiProxyRun = new ToxiProxyRun(daprRun, LATENCY, JITTER);
    toxiProxyRun.start();
    toxiProxyRun.use();
    toxiDemoActor = buildDemoActorProxy(new ResiliencyOptions().setTimeout(TIMEOUT));
    resilientDemoActor = buildDemoActorProxy(
            new ResiliencyOptions().setTimeout(TIMEOUT).setMaxRetries(MAX_RETRIES));
    oneRetryDemoActor = buildDemoActorProxy(
            new ResiliencyOptions().setTimeout(TIMEOUT).setMaxRetries(1));
  }

  private static DemoActor buildDemoActorProxy(ResiliencyOptions resiliencyOptions) {
    ActorProxyBuilder<DemoActor> builder =
            new ActorProxyBuilder(DemoActor.class, newActorClient(resiliencyOptions));
    return builder.build(ACTOR_ID);
  }

  @AfterAll
  public static void tearDown() throws Exception {
    if (toxiProxyRun != null) {
      toxiProxyRun.stop();
    }
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
