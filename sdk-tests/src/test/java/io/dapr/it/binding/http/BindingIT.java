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

package io.dapr.it.binding.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Service for input and output binding example.
 */
public class BindingIT extends BaseIT {

  private static final String BINDING_NAME = "sample123";

  private static final String BINDING_OPERATION = "create";

  public static class MyClass {
    public MyClass() {
    }

    public String message;
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void inputOutputBinding(boolean useGrpc) throws Exception {
    System.out.println("Working Directory = " + System.getProperty("user.dir"));
    String serviceNameVariant = useGrpc ? "-grpc" : "-http";

    DaprRun daprRun = startDaprApp(
        this.getClass().getSimpleName() + serviceNameVariant,
        InputBindingService.SUCCESS_MESSAGE,
        InputBindingService.class,
        true,
        60000);
    // At this point, it is guaranteed that the service above is running and all ports being listened to.
    if (useGrpc) {
      daprRun.switchToGRPC();
    } else {
      daprRun.switchToHTTP();
    }

    try(DaprClient client = new DaprClientBuilder().build()) {
      callWithRetry(() -> {
        System.out.println("Checking if input binding is up before publishing events ...");
        client.invokeBinding(
                BINDING_NAME, BINDING_OPERATION, "ping").block();

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
          BINDING_NAME, BINDING_OPERATION, myClass, Collections.singletonMap("MyMetadata", "MyValue"), Void.class).block();

      // This is an example of sending a plain string.  The input binding will receive
      //   cat
      final String m = "cat";
      System.out.println("sending " + m);
      client.invokeBinding(
          BINDING_NAME, BINDING_OPERATION, m, Collections.singletonMap("MyMetadata", "MyValue"), Void.class).block();

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
}
