/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.workflows;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WorkflowTaskOptions with cross-app workflow support.
 */
public class WorkflowTaskOptionsMergedTest {

  @Test
  void taskOptionsWithAppID() {
    WorkflowTaskOptions options = WorkflowTaskOptions.withAppID("app1");

    assertTrue(options.hasAppID());
    assertEquals("app1", options.getAppId());
    assertFalse(options.hasRetryPolicy());
    assertFalse(options.hasRetryHandler());
  }

  @Test
  void taskOptionsWithRetryPolicyAndAppID() {
    WorkflowTaskRetryPolicy retryPolicy = new WorkflowTaskRetryPolicy(3, Duration.ofSeconds(1));
    WorkflowTaskOptions options = WorkflowTaskOptions.builder()
            .retryPolicy(retryPolicy)
            .appID("app2")
            .build();

    assertTrue(options.hasAppID());
    assertEquals("app2", options.getAppId());
    assertTrue(options.hasRetryPolicy());
    assertEquals(retryPolicy, options.getRetryPolicy());
    assertFalse(options.hasRetryHandler());
  }

  @Test
  void taskOptionsWithRetryHandlerAndAppID() {
    WorkflowTaskRetryHandler retryHandler = new WorkflowTaskRetryHandler() {
      @Override
      public boolean handle(WorkflowTaskRetryContext context) {
        return context.getLastAttemptNumber() < 2;
      }
    };
    WorkflowTaskOptions options = WorkflowTaskOptions.builder()
            .retryHandler(retryHandler)
            .appID("app3")
            .build();

    assertTrue(options.hasAppID());
    assertEquals("app3", options.getAppId());
    assertFalse(options.hasRetryPolicy());
    assertTrue(options.hasRetryHandler());
    assertEquals(retryHandler, options.getRetryHandler());
  }

  @Test
  void taskOptionsWithoutAppID() {
    WorkflowTaskOptions options = WorkflowTaskOptions.create();

    assertFalse(options.hasAppID());
    assertNull(options.getAppId());
  }

  @Test
  void taskOptionsWithEmptyAppID() {
    WorkflowTaskOptions options = WorkflowTaskOptions.withAppID("");

    assertFalse(options.hasAppID());
    assertEquals("", options.getAppId());
  }

  @Test
  void taskOptionsWithNullAppID() {
    WorkflowTaskOptions options = WorkflowTaskOptions.builder().appID(null).build();

    assertFalse(options.hasAppID());
    assertNull(options.getAppId());
  }

  @Test
  void taskOptionsWithRetryPolicy() {
    WorkflowTaskRetryPolicy retryPolicy = new WorkflowTaskRetryPolicy(5, Duration.ofMinutes(1));
    WorkflowTaskOptions options = WorkflowTaskOptions.withRetryPolicy(retryPolicy);

    assertTrue(options.hasRetryPolicy());
    assertEquals(retryPolicy, options.getRetryPolicy());
    assertFalse(options.hasRetryHandler());
    assertFalse(options.hasAppID());
  }

  @Test
  void taskOptionsWithRetryHandler() {
    WorkflowTaskRetryHandler retryHandler = new WorkflowTaskRetryHandler() {
      @Override
      public boolean handle(WorkflowTaskRetryContext context) {
        return context.getLastAttemptNumber() < 3;
      }
    };
    WorkflowTaskOptions options = WorkflowTaskOptions.withRetryHandler(retryHandler);

    assertTrue(options.hasRetryHandler());
    assertEquals(retryHandler, options.getRetryHandler());
    assertFalse(options.hasRetryPolicy());
    assertFalse(options.hasAppID());
  }

  @Test
  void taskOptionsWithBuilderChaining() {
    WorkflowTaskRetryPolicy retryPolicy = new WorkflowTaskRetryPolicy(3, Duration.ofSeconds(1));
    WorkflowTaskRetryHandler retryHandler = context -> true;

    WorkflowTaskOptions options = WorkflowTaskOptions.builder()
            .retryPolicy(retryPolicy)
            .retryHandler(retryHandler)
            .appID("test-app")
            .build();

    assertNotNull(options);
    assertTrue(options.hasRetryPolicy());
    assertEquals(retryPolicy, options.getRetryPolicy());
    assertTrue(options.hasRetryHandler());
    assertEquals(retryHandler, options.getRetryHandler());
    assertTrue(options.hasAppID());
    assertEquals("test-app", options.getAppId());
  }
} 