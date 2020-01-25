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
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Service for output binding example.
 */
public class BindingIT extends BaseIT {

  private static DaprRun daprRun;

  private static DaprClient client;

  @BeforeClass
  public static void init() throws Exception {
    daprRun = startDaprApp(
      "dapr initialized. Status: Running. Init Elapsed",
      InputBindingExample.class,
      true,
      19000);
    client = new DaprClientBuilder(new DefaultObjectSerializer(), new DefaultObjectSerializer()).build();
  }

  public static class MyClass {
    public MyClass() {
    }

    public String message;
  }

  @Test
  public void inputOutputBinding() throws Exception {
    final String BINDING_NAME = "sample123";

    // This is an example of sending data in a user-defined object.  The input binding will receive:
    //   {"message":"hello"}
    MyClass myClass = new MyClass();
    myClass.message = "hello";

    System.out.println("sending first message");
    client.invokeBinding(BINDING_NAME, myClass).block();

    // This is an example of sending a plain string.  The input binding will receive
    //   cat
    final String m = "cat";
    System.out.println("sending " + m);
    client.invokeBinding(BINDING_NAME, m).block();

    callWithRetry(() -> {
      final List<String> messages = client.invokeService(Verb.GET, daprRun.getAppName(), "messages", null, List.class).block();
      assertEquals(2, messages.size());
      MyClass resultClass = null;
      try {
        resultClass = new ObjectMapper().readValue(new String(Base64.getDecoder().decode(messages.get(0))), MyClass.class);
      } catch (Exception ex) {
        ex.printStackTrace();
        fail("Error on decode message 1");
      }

      try {
        assertEquals("cat", new ObjectMapper().readValue(new String(Base64.getDecoder().decode(messages.get(1))), String.class));
      } catch (Exception ex) {
        ex.printStackTrace();
        fail("Error on decode message 2");
      }
      assertEquals("hello", resultClass.message);
    }, 8000);
  }
}
