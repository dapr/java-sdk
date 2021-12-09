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

package io.dapr.examples.secrets;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;

import java.util.Map;

/**
 * 1. Build and install jars:
 *   mvn clean install
 * 2. cd to [repo-root]/examples
 * 3. Add secret to vault:
 *   vault kv put secret/dapr/movie title="[my favorite movie]"
 * 4. Read secret from example:
 *   dapr run --components-path ./components/secrets -- \
 *     java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.secrets.SecretClient movie
 */
public class SecretClient {

  /**
   * Identifier in Dapr for the secret store.
   */
  private static final String SECRET_STORE_NAME = "vault";

  /**
   * JSON Serializer to print output.
   */
  private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();

  /**
   * Client to read a secret.
   *
   * @param args Unused arguments.
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("Use one argument: secret's key to be retrieved.");
    }

    String secretKey = args[0];
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      Map<String, String> secret = client.getSecret(SECRET_STORE_NAME, secretKey).block();
      System.out.println(JSON_SERIALIZER.writeValueAsString(secret));

      try {
        secret = client.getSecret(SECRET_STORE_NAME, "randomKey").block();
        System.out.println(JSON_SERIALIZER.writeValueAsString(secret));
      } catch (Exception ex) {
        System.out.println(ex.getMessage());
      }
    }
  }
}
