/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.workflows.client;

import io.dapr.config.Properties;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DaprWorkflowClientKeepaliveTest {

  private static final String THREAD_NAME = "dapr-workflow-client-keepalive";

  @Test
  public void keepaliveIsOffByDefault() throws InterruptedException {
    DaprWorkflowClient client = new DaprWorkflowClient(new Properties());
    try {
      assertFalse(keepaliveThreadAlive(), "no keepalive thread expected when the property is off");
    } finally {
      client.close();
    }
  }

  @Test
  public void keepaliveStartsWhenEnabledAndStopsOnClose() throws InterruptedException {
    Properties properties = new Properties(Map.of("dapr.workflows.app.keep.alive.enabled", "true"));
    DaprWorkflowClient client = new DaprWorkflowClient(properties);
    try {
      assertTrue(keepaliveThreadAlive(), "keepalive thread expected when the property is on");
    } finally {
      client.close();
    }
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
    while (keepaliveThreadAlive() && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
    assertFalse(keepaliveThreadAlive(), "keepalive thread should terminate after close()");
  }

  private static boolean keepaliveThreadAlive() {
    return Thread.getAllStackTraces().keySet().stream()
        .anyMatch(t -> t.getName().equals(THREAD_NAME) && t.isAlive());
  }
}
