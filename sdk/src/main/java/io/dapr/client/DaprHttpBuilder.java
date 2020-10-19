/*
 * Copyright (c) Microsoft Corporation.
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
   * Read timeout used to build object.
   */
  private Duration readTimeout = Duration.ofSeconds(Properties.HTTP_CLIENT_READTIMEOUTSECONDS.get());

  /**
   * Sets the read timeout duration for the instance to be built.
   *
   * <p>Instead, set environment variable "DAPR_HTTP_CLIENT_READTIMEOUTSECONDS",
   *   or system property "dapr.http.client.readtimeoutseconds".
   *
   * @param duration Read timeout duration.
   * @return Same builder instance.
   */
  @Deprecated
  public DaprHttpBuilder withReadTimeout(Duration duration) {
    this.readTimeout = duration;
    return this;
  }

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
   * Creates and instance of the HTTP Client.
   *
   * @return Instance of {@link DaprHttp}
   */
  private DaprHttp buildDaprHttp() {
    if (OK_HTTP_CLIENT.get() == null) {
      synchronized (LOCK) {
        if (OK_HTTP_CLIENT.get() == null) {
          OkHttpClient.Builder builder = new OkHttpClient.Builder();
          builder.readTimeout(this.readTimeout);
          OkHttpClient okHttpClient = builder.build();
          OK_HTTP_CLIENT.set(okHttpClient);
        }
      }
    }

    return new DaprHttp(Properties.SIDECAR_IP.get(), Properties.HTTP_PORT.get(), OK_HTTP_CLIENT.get());
  }
}
