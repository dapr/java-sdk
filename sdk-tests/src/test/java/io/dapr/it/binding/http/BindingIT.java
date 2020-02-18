/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.binding.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.Verb;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.services.EmptyService;
import io.dapr.serializer.DefaultObjectSerializer;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Service for input and output binding example.
 */
@RunWith(Parameterized.class)
public class BindingIT extends BaseIT {

  public static class MyClass {
    public MyClass() {
    }

    public String message;
  }

  /**
   * Parameters for this test.
   * Param #1: useGrpc.
   * @return Collection of parameter tuples.
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { { false }, { true } });
  }

  @Parameterized.Parameter
  public boolean useGrpc;

  @Test
  public void inputOutputBinding() throws Exception {
    System.out.println("Working Directory = " + System.getProperty("user.dir"));

    DaprRun daprRun = startDaprApp(
        this.getClass().getSimpleName(),
        InputBindingService.SUCCESS_MESSAGE,
        InputBindingService.class,
        true,
        60000);
    // At this point, it is guaranteed that the service above is running and all ports being listened to.
    // TODO: figure out why this wait is needed for this scenario to work end-to-end. Kafka not up yet?
    Thread.sleep(120000);
    if (this.useGrpc) {
      daprRun.switchToGRPC();
    } else {
      daprRun.switchToHTTP();
    }

    DaprClient client = new DaprClientBuilder().build();

    final String BINDING_NAME = "sample123";

    // This is an example of sending data in a user-defined object.  The input binding will receive:
    //   {"message":"hello"}
    MyClass myClass = new MyClass();
    myClass.message = "hello";

    System.out.println("sending first message");
    client.invokeBinding(BINDING_NAME, myClass, Collections.singletonMap("MyMetadata", "MyValue")).block();

    // This is an example of sending a plain string.  The input binding will receive
    //   cat
    final String m = "cat";
    System.out.println("sending " + m);
    client.invokeBinding(BINDING_NAME, m, Collections.singletonMap("MyMetadata", "MyValue")).block();

    // Metadata is not used by Kafka component, so it is not possible to validate.
    callWithRetry(() -> {
      System.out.println("Checking results ...");
      final List<String> messages =
          client.invokeService(
              Verb.GET,
              daprRun.getAppName(),
              "messages",
              null,
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
