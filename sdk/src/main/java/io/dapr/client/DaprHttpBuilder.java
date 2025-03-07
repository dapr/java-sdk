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

package io.dapr.client;

import io.dapr.config.Properties;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static io.dapr.config.Properties.API_TOKEN;
import static io.dapr.config.Properties.HTTP_CLIENT_MAX_REQUESTS;
import static io.dapr.config.Properties.HTTP_CLIENT_READ_TIMEOUT_SECONDS;
import static io.dapr.config.Properties.HTTP_ENDPOINT;
import static io.dapr.config.Properties.HTTP_PORT;
import static io.dapr.config.Properties.SIDECAR_IP;

/**
 * A builder for the DaprHttp.
 */
public class DaprHttpBuilder {

  private static volatile HttpClient HTTP_CLIENT;

  /**
   * Static lock object.
   */
  private static final Object LOCK = new Object();

  /**
   * Build an instance of the Http client based on the provided setup.
   * @param properties to configure the DaprHttp client
   * @return an instance of {@link DaprHttp}
   * @throws IllegalStateException if any required field is missing
   */
  public DaprHttp build(Properties properties) {
    return buildDaprHttp(properties);
  }

  /**
   * Creates an instance of the HTTP Client.
   * @param properties to configure the DaprHttp client
   * @return Instance of {@link DaprHttp}
   */
  private DaprHttp buildDaprHttp(Properties properties) {
    if (HTTP_CLIENT == null) {
      synchronized (LOCK) {
        if (HTTP_CLIENT == null) {
          int maxRequests = properties.getValue(HTTP_CLIENT_MAX_REQUESTS);
          Executor executor = Executors.newFixedThreadPool(maxRequests);
          HTTP_CLIENT = HttpClient.newBuilder()
              .executor(executor)
              .version(HttpClient.Version.HTTP_1_1)
              .build();
        }
      }
    }

    String endpoint = properties.getValue(HTTP_ENDPOINT);
    String apiToken = properties.getValue(API_TOKEN);
    Duration readTimeout = Duration.ofSeconds(properties.getValue(HTTP_CLIENT_READ_TIMEOUT_SECONDS));

    if ((endpoint != null) && !endpoint.isEmpty()) {
      return new DaprHttp(endpoint, apiToken, readTimeout, HTTP_CLIENT);
    }

    String sidecarIp = properties.getValue(SIDECAR_IP);
    int port = properties.getValue(HTTP_PORT);

    return new DaprHttp(sidecarIp, port, apiToken, readTimeout, HTTP_CLIENT);
  }
}
