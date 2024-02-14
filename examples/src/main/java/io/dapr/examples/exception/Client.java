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
import io.dapr.exceptions.DaprErrorDetails;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.TypeRef;

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
        client.publishEvent("unknown_pubsub", "mytopic", "mydata").block();
      } catch (DaprException exception) {
        System.out.println("Error code: " + exception.getErrorCode());
        System.out.println("Error message: " + exception.getMessage());
        System.out.println("Reason: " + exception.getErrorDetails().get(
            DaprErrorDetails.ErrorDetailType.ERROR_INFO,
            "reason",
            TypeRef.STRING));
        System.out.println("Error's payload size: " + exception.getPayload().length);
      }
    }
    System.out.println("Done");
  }
}
