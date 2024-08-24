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
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.dapr.config.Properties.API_TOKEN;
import static io.dapr.config.Properties.HTTP_CLIENT_MAX_IDLE_CONNECTIONS;
import static io.dapr.config.Properties.HTTP_CLIENT_MAX_REQUESTS;
import static io.dapr.config.Properties.HTTP_CLIENT_READ_TIMEOUT_SECONDS;
import static io.dapr.config.Properties.HTTP_ENDPOINT;
import static io.dapr.config.Properties.HTTP_PORT;
import static io.dapr.config.Properties.SIDECAR_IP;

/**
 * A builder for the DaprHttp.
 */
public class DaprHttpBuilder {

  /**
   * Singleton OkHttpClient.
   */
  private static volatile OkHttpClient OK_HTTP_CLIENT;

  /**
   * Static lock object.
   */
  private static final Object LOCK = new Object();

  /**
   * HTTP keep alive duration in seconds.
   *
   * <p>Just hard code to a reasonable value.
   */
  private static final int KEEP_ALIVE_DURATION = 30;


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
    if (OK_HTTP_CLIENT == null) {
      synchronized (LOCK) {
        if (OK_HTTP_CLIENT == null) {
          OkHttpClient.Builder builder = new OkHttpClient.Builder();
          Duration readTimeout = Duration.ofSeconds(properties.getValue(HTTP_CLIENT_READ_TIMEOUT_SECONDS));
          builder.readTimeout(readTimeout);

          Dispatcher dispatcher = new Dispatcher();
          dispatcher.setMaxRequests(properties.getValue(HTTP_CLIENT_MAX_REQUESTS));
          // The maximum number of requests for each host to execute concurrently.
          // Default value is 5 in okhttp which is totally UNACCEPTABLE!
          // For sidecar case, set it the same as maxRequests.
          dispatcher.setMaxRequestsPerHost(HTTP_CLIENT_MAX_REQUESTS.get());
          builder.dispatcher(dispatcher);

          ConnectionPool pool = new ConnectionPool(properties.getValue(HTTP_CLIENT_MAX_IDLE_CONNECTIONS),
                  KEEP_ALIVE_DURATION, TimeUnit.SECONDS);
          builder.connectionPool(pool);

          OK_HTTP_CLIENT = builder.build();
        }
      }
    }

    String endpoint = properties.getValue(HTTP_ENDPOINT);
    if ((endpoint != null) && !endpoint.isEmpty()) {
      return new DaprHttp(endpoint, properties.getValue(API_TOKEN), OK_HTTP_CLIENT);
    }

    return new DaprHttp(properties.getValue(SIDECAR_IP), properties.getValue(HTTP_PORT), properties.getValue(API_TOKEN),
            OK_HTTP_CLIENT);


  }
}
