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

package io.dapr.it.binding.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.exceptions.DaprException;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.internal.DaprContainerFactory;
import io.dapr.testcontainers.internal.DaprSidecarContainer;
import io.dapr.testcontainers.internal.spring.DaprSpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Service for input and output binding example.
 */
@DaprSpringBootTest(classes = InputBindingService.class)
@Tag("testcontainers")
public class BindingIT {

  private static final String APP_ID = "binding-http-it";
  private static final String BINDING_NAME = "sample123";

  @DaprSidecarContainer
  private static final DaprContainer DAPR_CONTAINER = createDaprContainer();

  private static DaprContainer createDaprContainer() {
    DaprContainer container = DaprContainerFactory.createForSpringBootTest(APP_ID);
    String appUrl = "http://host.testcontainers.internal:" + container.getAppPort();
    return container
        .withComponent(new Component(
            "github-http-binding-404",
            "bindings.http",
            "v1",
            Map.of("url", appUrl + "/github404")))
        .withComponent(new Component(
            "github-http-binding-404-success",
            "bindings.http",
            "v1",
            Map.of(
                "url", appUrl + "/github404",
                "errorIfNot2XX", "false")))
        .withComponent(new Component(
            BINDING_NAME,
            "bindings.http",
            "v1",
            Map.of("url", appUrl + "/" + BINDING_NAME)));
  }

  private DaprClient daprClient;

  @BeforeEach
  public void setup() {
    org.testcontainers.Testcontainers.exposeHostPorts(DAPR_CONTAINER.getAppPort());
    daprClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build();
    daprClient.waitForSidecar(10000).block();
    daprClient.invokeMethod(APP_ID, "messages/clear", "", HttpExtension.POST).block();
  }

  @AfterEach
  public void closeClient() throws Exception {
    daprClient.close();
  }

  @Test
  public void httpOutputBindingError() throws Exception {
    callWithRetry(() -> {
      System.out.println("Checking exception handling for output binding ...");
      try {
        daprClient.invokeBinding("github-http-binding-404", "get", "").block();
        fail("Should throw an exception");
      } catch (DaprException e) {
        assertEquals(404, e.getHttpStatusCode());
        assertTrue(new String(e.getPayload()).contains("received status code 404"));
      }
    }, 10000);
  }

  @Test
  public void httpOutputBindingErrorIgnoredByComponent() throws Exception {
    callWithRetry(() -> {
      System.out.println("Checking exception handling for output binding ...");
      try {
        daprClient.invokeBinding("github-http-binding-404-success", "get", "").block();
        fail("Should throw an exception");
      } catch (DaprException e) {
        assertEquals(404, e.getHttpStatusCode());
        assertTrue(new String(e.getPayload()).contains("message"));
        assertTrue(new String(e.getPayload()).contains("Not Found"));
        assertTrue(new String(e.getPayload()).contains("documentation_url"));
        assertTrue(new String(e.getPayload()).contains("https://docs.github.com/rest"));
      }
    }, 10000);
  }

  @Test
  public void inputOutputBinding() throws Exception {
    callWithRetry(() -> {
      System.out.println("Checking if input binding is up before publishing events ...");
      daprClient.invokeBinding(BINDING_NAME, "create", "ping").block();

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }

      daprClient.invokeMethod(APP_ID, "initialized", "", HttpExtension.GET).block();
    }, 120000);

    MyClass myClass = new MyClass();
    myClass.message = "hello";

    daprClient.invokeBinding(BINDING_NAME, "create", myClass, Map.of("MyMetadata", "MyValue"), Void.class).block();
    final String m = "cat";
    daprClient.invokeBinding(BINDING_NAME, "create", m, Map.of("MyMetadata", "MyValue"), Void.class).block();

    callWithRetry(() -> {
      final List<String> messages = daprClient.invokeMethod(APP_ID, "messages", null, HttpExtension.GET, List.class).block();
      assertEquals(2, messages.size());

      MyClass resultClass;
      try {
        resultClass = new ObjectMapper().readValue(messages.get(0), MyClass.class);
      } catch (Exception ex) {
        throw new RuntimeException("Error on decode message 1", ex);
      }

      try {
        assertEquals("cat", new ObjectMapper().readValue(messages.get(1), String.class));
      } catch (Exception ex) {
        throw new RuntimeException("Error on decode message 2", ex);
      }
      assertEquals("hello", resultClass.message);
    }, 8000);
  }

  public static class MyClass {
    public String message;
  }
}
