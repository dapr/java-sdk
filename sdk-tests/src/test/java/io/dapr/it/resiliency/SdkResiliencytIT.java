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

package io.dapr.it.resiliency;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprClientGrpc;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.ToxiProxyRun;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test SDK resiliency.
 */
public class SdkResiliencytIT extends BaseIT {

  private static final int NUM_ITERATIONS = 25;

  private static final Duration TIMEOUT = Duration.ofMillis(100);

  private static final Duration LATENCY = TIMEOUT.dividedBy(2);

  private static final Duration JITTER = TIMEOUT.multipliedBy(2);

  private static final int MAX_RETRIES = -1;  // Infinity

  private static DaprRun daprRun;

  private static DaprClient daprClient;

  private static ToxiProxyRun toxiProxyRun;

  private static DaprClient daprToxiClient;

  private static DaprClient daprResilientClient;

  private static DaprClient daprRetriesOnceClient;

  private final String randomStateKeyPrefix = UUID.randomUUID().toString();

  @BeforeAll
  public static void init() throws Exception {
    daprRun = startDaprApp(SdkResiliencytIT.class.getSimpleName(), 5000);
    // HTTP client is deprecated, so SDK resiliency is for gRPC client only.
    daprRun.switchToGRPC();
    daprClient = new DaprClientBuilder().build();

    Thread.sleep(5000);
    toxiProxyRun = new ToxiProxyRun(daprRun, LATENCY, JITTER);
    toxiProxyRun.start();
    toxiProxyRun.use();

    daprToxiClient = new DaprClientBuilder()
            .withResiliencyOptions(
                    new ResiliencyOptions().setTimeout(TIMEOUT))
            .build();
    daprResilientClient = new DaprClientBuilder()
            .withResiliencyOptions(
                    new ResiliencyOptions().setTimeout(TIMEOUT).setMaxRetries(MAX_RETRIES))
            .build();
    daprRetriesOnceClient = new DaprClientBuilder()
            .withResiliencyOptions(
                    new ResiliencyOptions().setTimeout(TIMEOUT).setMaxRetries(1))
            .build();

    assertTrue(daprClient instanceof DaprClientGrpc);
    assertTrue(daprToxiClient instanceof DaprClientGrpc);
    assertTrue(daprResilientClient instanceof DaprClientGrpc);
    assertTrue(daprRetriesOnceClient instanceof DaprClientGrpc);
  }

  @AfterAll
  public static void tearDown() throws Exception {
    if (daprClient != null) {
      daprClient.close();
    }
    if (daprToxiClient != null) {
      daprToxiClient.close();
    }
    if (daprResilientClient != null) {
      daprResilientClient.close();
    }
    if (daprRetriesOnceClient != null) {
      daprRetriesOnceClient.close();
    }
    if (toxiProxyRun != null) {
      toxiProxyRun.stop();
    }
  }

  @Test
  public void retryAndTimeout() {
    AtomicInteger toxiClientErrorCount = new AtomicInteger();
    AtomicInteger retryOneClientErrorCount = new AtomicInteger();
    for (int i = 0; i < NUM_ITERATIONS; i++) {
      String key = randomStateKeyPrefix + "_" + i;
      String value = Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
      try {
        daprToxiClient.saveState(STATE_STORE_NAME, key, value).block();
      } catch (Exception e) {
        // This call should fail sometimes. So, we count.
        toxiClientErrorCount.incrementAndGet();
      }
      try {
        daprRetriesOnceClient.saveState(STATE_STORE_NAME, key, value).block();
      } catch (Exception e) {
        // This call should fail sometimes. So, we count.
        retryOneClientErrorCount.incrementAndGet();
      }

      // We retry forever so that the call below should always work.
      daprResilientClient.saveState(STATE_STORE_NAME, key, value).block();
      // Makes sure the value was actually saved.
      String savedValue = daprClient.getState(STATE_STORE_NAME, key, String.class).block().getValue();
      assertEquals(value, savedValue);
    }

    // This assertion makes sure that toxicity is on
    assertTrue(toxiClientErrorCount.get() > 0);
    assertTrue(retryOneClientErrorCount.get() > 0);
    // A client without retries should have more errors than a client with one retry.
    assertTrue(toxiClientErrorCount.get() > retryOneClientErrorCount.get());
  }
}
