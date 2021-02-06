/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.config.Properties;
import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A builder for the DaprHttp.
 */
public class DaprHttpBuilder {

  /**
   * Singleton OkHttpClient.
   */
  private static final AtomicReference<OkHttpClient> OK_HTTP_CLIENT = new AtomicReference<>();

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
    if (OK_HTTP_CLIENT.get() == null) {
      synchronized (LOCK) {
        if (OK_HTTP_CLIENT.get() == null) {
          OkHttpClient.Builder builder = new OkHttpClient.Builder();
          Duration readTimeout = Duration.ofSeconds(Properties.HTTP_CLIENT_READ_TIMEOUT_SECONDS.get());
          builder.readTimeout(readTimeout);
          OkHttpClient okHttpClient = builder.build();
          OK_HTTP_CLIENT.set(okHttpClient);
        }
      }
    }

    return new DaprHttp(Properties.SIDECAR_IP.get(), Properties.HTTP_PORT.get(), OK_HTTP_CLIENT.get());
  }
}
