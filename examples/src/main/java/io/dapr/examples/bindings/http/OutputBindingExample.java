/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.bindings.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.serializer.DefaultObjectSerializer;

/**
 * Service for output binding example.
 * 1. From your repo root, build and install jars:
 *  mvn clean install
 * 2. cd to [repo-root]/examples
 * 3. Run the program:
 * dapr run --app-id outputbinding --port 3006 -- mvn exec:java -Dexec.mainClass=io.dapr.examples.bindings.http.OutputBindingExample
 */
public class OutputBindingExample {

  public static class MyClass {
    public MyClass(){}
    public String message;
  }

  public static void main(String[] args) {
    DaprClient client = new DaprClientBuilder(new DefaultObjectSerializer(), new DefaultObjectSerializer()).build();

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

    try {
      Thread.sleep((long) (10000 * Math.random()));
    } catch (InterruptedException e) {
      e.printStackTrace();
      Thread.currentThread().interrupt();
      return;
    }

    System.out.println("Done.");
  }
}
