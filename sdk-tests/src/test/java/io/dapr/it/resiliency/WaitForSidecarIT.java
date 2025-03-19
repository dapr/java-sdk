/*
 * Copyright 2024 The Dapr Authors
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

import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.ToxiProxyRun;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test SDK resiliency.
 */
public class WaitForSidecarIT extends BaseIT {

  // Use a number large enough to make sure it will respect the entire timeout.
  private static final Duration LATENCY = Duration.ofSeconds(5);

  private static final Duration JITTER = Duration.ofSeconds(0);

  private static DaprRun daprRun;

  private static ToxiProxyRun toxiProxyRun;

  private static DaprRun daprNotRunning;

  @BeforeAll
  public static void init() throws Exception {
    daprRun = startDaprApp(WaitForSidecarIT.class.getSimpleName(), 5000);
    daprNotRunning = startDaprApp(WaitForSidecarIT.class.getSimpleName() + "NotRunning", 5000);
    daprNotRunning.stop();

    toxiProxyRun = new ToxiProxyRun(daprRun, LATENCY, JITTER);
    toxiProxyRun.start();
  }

  @Test
  public void waitSucceeds() throws Exception {
    try(var client = daprRun.newDaprClient()) {
      client.waitForSidecar(5000).block();
    }
  }

  @Test
  public void waitTimeout() {
    int timeoutInMillis = (int)LATENCY.minusMillis(100).toMillis();
    long started = System.currentTimeMillis();

    assertThrows(RuntimeException.class, () -> {
      try(var client = toxiProxyRun.newDaprClientBuilder().build()) {
        client.waitForSidecar(timeoutInMillis).block();
      }
    });

    long duration = System.currentTimeMillis() - started;

    assertThat(duration).isGreaterThanOrEqualTo(timeoutInMillis);
  }

  @Test
  public void waitSlow() throws Exception {
    int timeoutInMillis = (int)LATENCY.plusMillis(100).toMillis();
    long started = System.currentTimeMillis();

    try(var client = toxiProxyRun.newDaprClientBuilder().build()) {
        client.waitForSidecar(timeoutInMillis).block();
    }

    long duration = System.currentTimeMillis() - started;

    assertThat(duration).isGreaterThanOrEqualTo(LATENCY.toMillis());
  }

  @Test
  public void waitNotRunningTimeout() {
    // Does not make this number too smaller since bug does not repro when <= 2.5s.
    // This has to do with a previous bug in the implementation.
    int timeoutMilliseconds = 5000;
    long started = System.currentTimeMillis();

    assertThrows(RuntimeException.class, () -> {
      try(var client = daprNotRunning.newDaprClientBuilder().build()) {
        client.waitForSidecar(timeoutMilliseconds).block();
      }
    });

    long duration = System.currentTimeMillis() - started;

    assertThat(duration).isGreaterThanOrEqualTo(timeoutMilliseconds);
  }
}
