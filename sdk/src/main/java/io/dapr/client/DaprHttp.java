/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.exceptions.DaprError;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.Constants;
import io.dapr.utils.ObjectSerializer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class DaprHttp {

  /**
   * ObjectMapper to Serialize data
   */
  private static final ObjectSerializer MAPPER = new ObjectSerializer();

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
   * Creates a new instance of {@link DaprHttp}.
   *
   * @param baseUrl        Base url calling Dapr (e.g. http://localhost)
   * @param port           Port for calling Dapr. (e.g. 3500)
   * @param threadPoolSize Number of threads for http calls.
   * @param httpClient     RestClient used for all API calls in this new instance.
   */
  DaprHttp(String baseUrl, int port, int threadPoolSize, OkHttpClient httpClient) {
    this.baseUrl = String.format("%s:%d/", baseUrl, port);
    this.httpClient = httpClient;
    this.pool = Executors.newFixedThreadPool(threadPoolSize);
  }

  /**
   * Invokes an API asynchronously that returns Void.
   *
   * @param method    HTTP method.
   * @param urlString url as String.
   * @param json      JSON payload or null.
   * @return Asynchronous Void
   */
  protected final CompletableFuture<Void> invokeAPIVoid(String method, String urlString, String json) {
    CompletableFuture<String> future = this.invokeAPI(method, urlString, json);
    return future.thenAcceptAsync(future::complete);
  }

  /**
   * Invokes an API asynchronously that returns a text payload.
   *
   * @param method    HTTP method.
   * @param urlString url as String.
   * @param json      JSON payload or null.
   * @return Asynchronous text
   */
  public final CompletableFuture<String> invokeAPI(String method, String urlString, String json) {
    CompletableFuture<String> future = CompletableFuture.supplyAsync(
        () -> {
          try {
            String requestId = UUID.randomUUID().toString();
            RequestBody body =
                json != null ? RequestBody.Companion.create(json, MEDIA_TYPE_APPLICATION_JSON) : REQUEST_BODY_EMPTY_JSON;

            Request request = new Request.Builder()
                .url(new URL(this.baseUrl + urlString))
                .method(method, body)
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

    return future;
  }

  protected final CompletableFuture<Void> publishEvent(String method, String topic, String data) {
    StringBuilder url = new StringBuilder(Constants.PUBLISH_PATH);
    if (Constants.defaultHttpMethodSupported.PUT.name().equals(method)) {
      url.append("/").append(topic);
    }

    return invokeAPIVoid(method, url.toString(), data);
  }

  /**
   * Creating invokeBinding Method for Http Client
   *
   * @param method         HTTP method.
   * @param topic/name/key entity value
   * @param data           JSON payload or null.
   * @return Mono<String>
   */
  protected final CompletableFuture<Void> invokeBinding(String method, String topic, String data) {

    StringBuilder url = new StringBuilder(Constants.BINDING_PATH);
    if (Constants.defaultHttpMethodSupported.PUT.name().equals(method)) {
      url.append("/").append(topic);
    }

    return invokeAPIVoid(method, url.toString(), data);
  }

  /**
   * Creating invokeBinding Method for Http Client
   *
   * @param key HTTP method.
   * @return Mono<String>
   */
  protected final CompletableFuture<String> getState(String key) {

    String url = Constants.STATE_PATH + "/" + key;
    return invokeAPI(Constants.defaultHttpMethodSupported.GET.name(), url, null);
  }

  /**
   * Creating Save State Method for Http Client
   *
   * @param data data.
   * @return Mono<String>
   */
  protected final CompletableFuture<Void> saveState(String data) throws Exception {

    String url = Constants.STATE_PATH;

    return invokeAPIVoid(Constants.defaultHttpMethodSupported.POST.name(), url, data);
  }

  /**
   * Creating delete State Method for Http Client
   *
   * @param key HTTP method.
   * @return Mono<String>
   */
  protected final CompletableFuture<Void> deleteState(String key) {

    if (key.isEmpty() || key == null) {
      throw new DaprException("500", "Name cannot be null or empty.");
    }

    String url = Constants.STATE_PATH + "/" + key;

    return invokeAPIVoid(Constants.defaultHttpMethodSupported.DELETE.name(), url, null);
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
      throw new DaprException("500", "Unknown error: could not parse error json.");
    }
  }

}
