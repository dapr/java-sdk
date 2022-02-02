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

package io.dapr.examples.bindings.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;

/**
 * Service for output binding example.
 * 1. From your repo root, build and install jars:
 * mvn clean install
 * 2. cd to [repo-root]/examples
 * 3. Run the program:
 * dapr run --components-path ./components/bindings --app-id outputbinding \
 *   -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.bindings.http.OutputBindingExample
 */
public class OutputBindingExample {

  public static class MyClass {
    public MyClass() {
    }

    public String message;
  }

  static final String BINDING_NAME = "sample123";

  static final String BINDING_OPERATION = "create";

  /**
   * The main method of this app.
   *
   * @param args Not used.
   */
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  public static void main(String[] args) throws Exception {
    try (DaprClient client = new DaprClientBuilder().build()) {

      int count = 0;
      while (!Thread.currentThread().isInterrupted()) {
        String message = "Message #" + (count);

        // On even number, send class message
        if (count % 2 == 0) {
          // This is an example of sending data in a user-defined object.  The input binding will receive:
          //   {"message":"hello"}
          MyClass myClass = new MyClass();
          myClass.message = message;

          System.out.println("sending a class with message: " + myClass.message);
          client.invokeBinding(BINDING_NAME, BINDING_OPERATION, myClass).block();
        } else {
          System.out.println("sending a plain string: " + message);
          client.invokeBinding(BINDING_NAME, BINDING_OPERATION, message).block();
        }
        count++;

        try {
          Thread.sleep((long) (10000 * Math.random()));
        } catch (InterruptedException e) {
          e.printStackTrace();
          Thread.currentThread().interrupt();
        }
      }

      System.out.println("Done.");
    }
  }
}
