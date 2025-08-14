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
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.exceptions.DaprException;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Service for input and output binding example.
 */
public class BindingIT extends BaseIT {

  @Test
  public void httpOutputBindingError() throws Exception {
    var run = startDaprApp(
        this.getClass().getSimpleName() + "-httpoutputbinding-exception",
        60000);
    try(DaprClient client = run.newDaprClientBuilder().build()) {
      // Validate error message
      callWithRetry(() -> {
        System.out.println("Checking exception handling for output binding ...");
        try {
          client.invokeBinding("github-http-binding-404", "get", "").block();
          fail("Should throw an exception");
        } catch (DaprException e) {
          assertEquals(404, e.getHttpStatusCode());
          // This HTTP binding did not set `errorIfNot2XX` to false in component metadata, so the error payload is not
          // consistent between HTTP and gRPC.
          assertTrue(new String(e.getPayload()).contains(
              "error invoking output binding github-http-binding-404: received status code 404"));
        }
      }, 10000);
    }
  }

  @Test
  public void httpOutputBindingErrorIgnoredByComponent() throws Exception {
    var run = startDaprApp(
        this.getClass().getSimpleName() + "-httpoutputbinding-ignore-error",
        60000);
    try(DaprClient client = run.newDaprClientBuilder().build()) {
      // Validate error message
      callWithRetry(() -> {
        System.out.println("Checking exception handling for output binding ...");
        try {
          client.invokeBinding("github-http-binding-404-success", "get", "").block();
          fail("Should throw an exception");
        } catch (DaprException e) {
          assertEquals(404, e.getHttpStatusCode());
          // The HTTP binding must set `errorIfNot2XX` to false in component metadata for the error payload to be
          // consistent between HTTP and gRPC.
          assertTrue(new String(e.getPayload()).contains("message"));
          assertTrue(new String(e.getPayload()).contains("Not Found"));
          assertTrue(new String(e.getPayload()).contains("documentation_url"));
          assertTrue(new String(e.getPayload()).contains("https://docs.github.com/rest"));
        }
      }, 10000);
    }
  }

  @Test
  public void inputOutputBinding() throws Exception {
    DaprRun daprRun = startDaprApp(
        this.getClass().getSimpleName() + "-grpc",
        InputBindingService.SUCCESS_MESSAGE,
        InputBindingService.class,
        true,
        60000);

    var bidingName = "sample123";

    try(DaprClient client = daprRun.newDaprClientBuilder().build()) {
      callWithRetry(() -> {
        System.out.println("Checking if input binding is up before publishing events ...");
        client.invokeBinding(
            bidingName, "create", "ping").block();

        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }

        client.invokeMethod(daprRun.getAppName(), "initialized", "", HttpExtension.GET).block();
      }, 120000);

      // This is an example of sending data in a user-defined object.  The input binding will receive:
      //   {"message":"hello"}
      MyClass myClass = new MyClass();
      myClass.message = "hello";

      System.out.println("sending first message");
      client.invokeBinding(
          bidingName, "create", myClass, Map.of("MyMetadata", "MyValue"), Void.class).block();

      // This is an example of sending a plain string.  The input binding will receive
      //   cat
      final String m = "cat";
      System.out.println("sending " + m);
      client.invokeBinding(
          bidingName, "create", m, Map.of("MyMetadata", "MyValue"), Void.class).block();

      // Metadata is not used by Kafka component, so it is not possible to validate.
      callWithRetry(() -> {
        System.out.println("Checking results ...");
        final List<String> messages =
            client.invokeMethod(
                daprRun.getAppName(),
                "messages",
                null,
                HttpExtension.GET,
                List.class).block();
        assertEquals(2, messages.size());

        MyClass resultClass = null;
        try {
          resultClass = new ObjectMapper().readValue(messages.get(0), MyClass.class);
        } catch (Exception ex) {
          ex.printStackTrace();
          fail("Error on decode message 1");
        }

        try {
          assertEquals("cat", new ObjectMapper().readValue(messages.get(1), String.class));
        } catch (Exception ex) {
          ex.printStackTrace();
          fail("Error on decode message 2");
        }
        assertEquals("hello", resultClass.message);
      }, 8000);
    }
  }

  public static class MyClass {
    public MyClass() {
    }

    public String message;
  }
}
