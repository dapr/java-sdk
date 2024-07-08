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
import io.dapr.client.DaprClientImpl;
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
public class SdkResiliencyIT extends BaseIT {

  private static final int NUM_ITERATIONS = 35;

  private static final Duration TIMEOUT = Duration.ofMillis(100);

  private static final Duration LATENCY = TIMEOUT.dividedBy(3);

  private static final Duration JITTER = TIMEOUT.multipliedBy(3);

  private static final int MAX_RETRIES = -1;  // Infinity

  private static DaprClient daprClient;

  private static ToxiProxyRun toxiProxyRun;

  private static DaprClient daprToxiClient;

  private static DaprClient daprResilientClient;

  private static DaprClient daprRetriesOnceClient;

  private final String randomStateKeyPrefix = UUID.randomUUID().toString();

  @BeforeAll
  public static void init() throws Exception {
    DaprRun daprRun = startDaprApp(SdkResiliencyIT.class.getSimpleName(), 5000);
    daprClient = new DaprClientBuilder().build();
    daprClient.waitForSidecar(8000).block();

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

    assertTrue(daprClient instanceof DaprClientImpl);
    assertTrue(daprToxiClient instanceof DaprClientImpl);
    assertTrue(daprResilientClient instanceof DaprClientImpl);
    assertTrue(daprRetriesOnceClient instanceof DaprClientImpl);
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
    AtomicInteger retryOnceClientErrorCount = new AtomicInteger();

    while (true){
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
          retryOnceClientErrorCount.incrementAndGet();
        }

        // We retry forever so that the call below should always work.
        daprResilientClient.saveState(STATE_STORE_NAME, key, value).block();
        // Makes sure the value was actually saved.
        String savedValue = daprClient.getState(STATE_STORE_NAME, key, String.class).block().getValue();
        assertEquals(value, savedValue);
      }

      // We should have at least one success per client, otherwise retry.
      if(toxiClientErrorCount.get() < NUM_ITERATIONS && retryOnceClientErrorCount.get() < NUM_ITERATIONS){
        // This assertion makes sure that toxicity is on
        assertTrue(toxiClientErrorCount.get() > 0, "Toxi client error count is 0");
        assertTrue(retryOnceClientErrorCount.get() > 0, "Retry once client error count is 0");
        // A client without retries should have more errors than a client with one retry.

        String failureMessage = formatFailureMessage(toxiClientErrorCount, retryOnceClientErrorCount);
        assertTrue(toxiClientErrorCount.get() > retryOnceClientErrorCount.get(), failureMessage);
        break;
      }
      toxiClientErrorCount.set(0);
      retryOnceClientErrorCount.set(0);
    }
  }

  private static String formatFailureMessage(
      AtomicInteger toxiClientErrorCount,
      AtomicInteger retryOnceClientErrorCount
  ) {
    return String.format(
        "Toxi client error count: %d, Retry once client error count: %d",
        toxiClientErrorCount.get(),
        retryOnceClientErrorCount.get()
    );
  }
}
