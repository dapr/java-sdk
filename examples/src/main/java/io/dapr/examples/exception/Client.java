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

package io.dapr.examples.exception;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.exceptions.DaprException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. Go into examples:
 * cd examples
 * 3. send a message to be saved as state:
 * dapr run -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.exception.Client
 */
public class Client {

  /**
   * Executes the sate actions.
   * @param args messages to be sent as state value.
   */
  public static void main(String[] args) throws Exception {
    try (DaprClient client = new DaprClientBuilder().build()) {

      try {
        client.publishEvent("", "", "").block();
        // client.publishEvent("fake", "fake", "someData").block();
        // client.getState("Unknown state store", "myKey", String.class).block();
      } catch (DaprException exception) {
        System.out.println("Error code: " + exception.getErrorCode());
        System.out.println("Error message: " + exception.getMessage());

        try {
          Map<String, Object> detailsMap = exception.getStatusDetails();
          if (detailsMap != null && detailsMap.containsKey("details")) {
            Object detailsObject = detailsMap.get("details");
            if (detailsObject instanceof List) {
              List<Map<String, Object>> innerDetailsList = (List<Map<String, Object>>) detailsObject;
              System.out.println("Error Details: ");

              for (Map<String, Object> innerDetails : innerDetailsList) {

                if (innerDetails.containsKey("@type") && innerDetails.get("@type").equals("type.googleapis.com/google.rpc.ErrorInfo")) {
                  System.out.println("\tError Detail is of type: Error_Info");
                  // Implement specific logic based on specific error type
                }

                for (Map.Entry<String, Object> entry : innerDetails.entrySet()) {
                  System.out.println("\t" + entry.getKey() + ": " + entry.getValue());
                }
                System.out.println(); // separate error details with newline
              }
            }
          }
          System.out.println("Error Details: " + exception.getStatusDetails());
        } catch (RuntimeException e) {
          System.out.println("Error Details: NULL");
        }
        exception.printStackTrace();
      }

      System.out.println("Done");
    }
  }
}
