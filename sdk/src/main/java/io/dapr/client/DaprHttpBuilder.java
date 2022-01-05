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
import okhttp3.OkHttpClient;

import java.time.Duration;

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
   * Build an instance of the Http client based on the provided setup.
   *
   * @return an instance of {@link DaprHttp}
   * @throws IllegalStateException if any required field is missing
   */
  public DaprHttp build() {
    return buildDaprHttp();
  }

  /**
   * Creates an instance of the HTTP Client.
   *
   * @return Instance of {@link DaprHttp}
   */
  private DaprHttp buildDaprHttp() {
    if (OK_HTTP_CLIENT == null) {
      synchronized (LOCK) {
        if (OK_HTTP_CLIENT == null) {
          OkHttpClient.Builder builder = new OkHttpClient.Builder();
          Duration readTimeout = Duration.ofSeconds(Properties.HTTP_CLIENT_READ_TIMEOUT_SECONDS.get());
          builder.readTimeout(readTimeout);
          OK_HTTP_CLIENT = builder.build();
        }
      }
    }

    return new DaprHttp(Properties.SIDECAR_IP.get(), Properties.HTTP_PORT.get(), OK_HTTP_CLIENT);
  }
}
