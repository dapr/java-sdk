/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.utils.Properties;
import okhttp3.OkHttpClient;

import java.time.Duration;

/**
 * A builder for the DaprHttp.
 */
public class DaprHttpBuilder {

    /**
     * Read timeout for http calls.
     */
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Read timeout used to build object.
     */
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;

    /**
     * Sets the read timeout duration for the instance to be built.
     *
     * @param duration Read timeout duration.
     * @return Same builder instance.
     */
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
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.readTimeout(DEFAULT_READ_TIMEOUT);
        OkHttpClient okHttpClient = builder.build();
        return new DaprHttp(Properties.HTTP_PORT.get(), okHttpClient);
    }
}
