/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.utils.Constants;
import okhttp3.OkHttpClient;

/**
 * Base class for client builders
 */
public abstract class AbstractClientBuilder {

    /**
     * Default port for Dapr after checking environment variable.
     */
    private int port = AbstractClientBuilder.getEnvPortOrDefault();

    /**
     * Default host for Dapr after checking environment variable.
     */
    private String host = AbstractClientBuilder.getEnvHostOrDefault();


    /**
     * Default thread pool size for Dapr after checking environment variable.
     */
    private int threadPoolSize = AbstractClientBuilder.getEnvThreadPoolSizeOrDefault();

    private OkHttpClient.Builder okHttpClientBuilder = AbstractClientBuilder.getDefaultOkHttpClientBuilder();

    /**
     * Overrides the port.
     *
     * @param port New port.
     * @return This instance.
     */
    public AbstractClientBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    public AbstractClientBuilder withHost(String host) {
        this.host = host;
        return this;
    }

    public AbstractClientBuilder withThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        return this;
    }

    public AbstractClientBuilder withOkHttpClientBuilder(OkHttpClient.Builder okHttpClientBuilder) {
        this.okHttpClientBuilder = okHttpClientBuilder;
        return this;
    }

    /**
     * Returns configured port.
     *
     * @return Port to connect to Dapr.
     */
    protected int getPort() {
        return this.port;
    }

    /**
     * Returns configured host.
     * @return host to connecto to Dapr.
     */
    protected String getHost() {
        return this.host;
    }

    protected int getThreadPoolSize() {
        return this.threadPoolSize;
    }

    public OkHttpClient.Builder getOkHttpClientBuilder() {
        return okHttpClientBuilder;
    }

    /**
     * Tries to get a valid port from environment variable or returns default.
     *
     * @return Port defined in env variable or default.
     */
    private static int getEnvPortOrDefault() {
        String envPort = System.getenv(Constants.ENV_DAPR_HTTP_PORT);
        if (envPort == null || envPort.trim().isEmpty()) {
            return Constants.DEFAULT_PORT;
        }

        try {
            return Integer.parseInt(envPort.trim());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return Constants.DEFAULT_PORT;
    }

    private static String getEnvHostOrDefault() {
        String envHost = System.getenv(Constants.ENV_DAPR_HTTP_HOST);
        if (envHost == null || envHost.trim().isEmpty()) {
            return Constants.DEFAULT_HOSTNAME;
        }

        return envHost;
    }

    private static int getEnvThreadPoolSizeOrDefault() {
        String envThreadPoolSize = System.getenv(Constants.ENV_DAPR_HTTP_HOST);
        if (envThreadPoolSize == null || envThreadPoolSize.trim().isEmpty()) {
            return Constants.DEFAULT_THREAD_POOL_SIZE;
        }
        try {
            return Integer.parseInt(envThreadPoolSize.trim());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return Constants.DEFAULT_THREAD_POOL_SIZE;
    }

    private static OkHttpClient.Builder getDefaultOkHttpClientBuilder() {
        return new OkHttpClient.Builder();
    }

}
