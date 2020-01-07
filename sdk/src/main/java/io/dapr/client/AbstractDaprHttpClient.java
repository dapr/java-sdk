/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.exceptions.DaprError;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.Constants;
import okhttp3.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractDaprHttpClient {

    /**
     * Defines the standard application/json type for HTTP calls in Dapr.
     */
    private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
            MediaType.get("application/json; charset=utf-8");

    /**
     * Shared object representing an empty request body in JSON.
     */
    private static final RequestBody REQUEST_BODY_EMPTY_JSON =
            RequestBody.Companion.create("", MEDIA_TYPE_APPLICATION_JSON);

    /**
     * JSON Object Mapper.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * The base url used for form urls. This is typically "http://localhost:3500".
     */
    private final String baseUrl;

    /**
     * Http client used for all API calls.
     */
    private final OkHttpClient httpClient;

    /**
     * Thread-pool for HTTP calls.
     */
    private final ExecutorService pool;

    /**
     * Creates a new instance of {@link AbstractDaprHttpClient}.
     *
     * @param port           Port for calling Dapr. (e.g. 3500)
     * @param threadPoolSize Number of threads for http calls.
     * @param httpClient     RestClient used for all API calls in this new instance.
     */
    public AbstractDaprHttpClient(int port, int threadPoolSize, OkHttpClient httpClient) {
        this.baseUrl = String.format("http://%s:%d/", Constants.DEFAULT_HOSTNAME, port);
        this.httpClient = httpClient;
        this.pool = Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * Creates a new instance of {@link AbstractDaprHttpClient}.
     *
     * @param port       Port for calling Dapr. (e.g. 3500)
     * @param httpClient RestClient used for all API calls in this new instance.
     */
    public AbstractDaprHttpClient(int port, OkHttpClient httpClient) {
        this(port, 1, httpClient);
    }

    /**
     * Invokes an API asynchronously that returns Void.
     *
     * @param method    HTTP method.
     * @param urlString url as String.
     * @param json      JSON payload or null.
     * @return Asynchronous Void
     */
    protected final Mono<Void> invokeAPIVoid(String method, String urlString, String json) {
        return this.invokeAPI(method, urlString, json).then();
    }

    /**
     * Invokes an API asynchronously that returns a text payload.
     *
     * @param method    HTTP method.
     * @param urlString url as String.
     * @param json      JSON payload or null.
     * @return Asynchronous text
     */
    public final Mono<String> invokeAPI(String method, String urlString, String json) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(
                () -> {
                    try {
                        String requestId = UUID.randomUUID().toString();
                        RequestBody body =
                                json != null ? RequestBody.Companion.create(json, MEDIA_TYPE_APPLICATION_JSON) : REQUEST_BODY_EMPTY_JSON;

                        Request request = new Request.Builder()
                                .url(new URL(this.baseUrl + urlString))
                                .method(method, (json == null && method.equals("GET")) ? null : body)
                                .addHeader(Constants.HEADER_DAPR_REQUEST_ID, requestId)
                                .build();

                        try (Response response = this.httpClient.newCall(request).execute()) {
                            if (!response.isSuccessful()) {
                                DaprError error = parseDaprError(response.body().string());
                                if ((error != null) && (error.getErrorCode() != null) && (error.getMessage() != null)) {
                                    throw new RuntimeException(new DaprException(error));
                                }

                                throw new RuntimeException("Unknown error.");
                            }
                            String result = response.body().string();
                            return result == null ? "" : result;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, this.pool);

        return Mono.fromFuture(future);
    }

    /**
     * Tries to parse an error from Dapr response body.
     *
     * @param json Response body from Dapr.
     * @return DaprError or null if could not parse.
     */
    private static DaprError parseDaprError(String json) {
        if (json == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(json, DaprError.class);
        } catch (IOException e) {
            throw new RuntimeException("Unknown error: could not parse error json.");
        }
    }

}
