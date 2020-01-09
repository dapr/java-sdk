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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class DaprHttp {

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
   * Empty input or output.
   */
  private static final byte[] EMPTY_BYTES = new byte[0];

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
   * @param baseUrl    Base url calling Dapr (e.g. http://localhost)
   * @param port       Port for calling Dapr. (e.g. 3500)
   * @param httpClient RestClient used for all API calls in this new instance.
   */
  DaprHttp(String baseUrl, int port, OkHttpClient httpClient) {
    this.baseUrl = String.format("%s:%d/", baseUrl, port);
    this.httpClient = httpClient;
    this.pool = Executors.newWorkStealingPool();
  }

  /**
   * Invokes an API asynchronously without payload that returns a text payload.
   *
   * @param method    HTTP method.
   * @param urlString url as String.
   * @return Asynchronous text
   */
  public final Mono<String> invokeAPI(String method, String urlString, Map<String, String> headers) {
    return this.invokeAPI(method, urlString, (byte[])null, headers);
  }

  /**
   * Invokes an API asynchronously that returns a text payload.
   *
   * @param method    HTTP method.
   * @param urlString url as String.
   * @param content   payload to be posted.
   * @return Asynchronous text
   */
  public final Mono<String> invokeAPI(String method, String urlString, String content, Map<String, String> headers) {
    return this.invokeAPI(method, urlString, content == null ? EMPTY_BYTES : content.getBytes(StandardCharsets.UTF_8), headers);
  }

  /**
   * Invokes an API asynchronously that returns a text payload.
   *
   * @param method    HTTP method.
   * @param urlString url as String.
   * @param content   payload to be posted.
   * @return Asynchronous text
   */
  public final Mono<String> invokeAPI(String method, String urlString, byte[] content, Map<String, String> headers) {
    return Mono.fromFuture(CompletableFuture.supplyAsync(
        () -> {
          try {
            String requestId = UUID.randomUUID().toString();
            RequestBody body = REQUEST_BODY_EMPTY_JSON;

            String contentType = headers != null ? headers.get("content-type") : null;
            MediaType mediaType = contentType == null ? MEDIA_TYPE_APPLICATION_JSON : MediaType.get(contentType);
            if (content == null) {
              body = mediaType.equals(MEDIA_TYPE_APPLICATION_JSON) ?
                  REQUEST_BODY_EMPTY_JSON : RequestBody.Companion.create(new byte[0], mediaType);
            } else {
              body =  RequestBody.Companion.create(content, mediaType);
            }

            Request.Builder requestBuilder = new Request.Builder()
                .url(new URL(this.baseUrl + urlString))
                .addHeader(Constants.HEADER_DAPR_REQUEST_ID, requestId);
            if (Constants.defaultHttpMethodSupported.GET.name().equals(method)) {
              requestBuilder.get();
            } else if (Constants.defaultHttpMethodSupported.DELETE.name().equals(method)) {
              requestBuilder.delete();
            } else {
              requestBuilder.method(method, body);
            }
            if (headers != null) {
              Optional.ofNullable(headers.entrySet()).orElse(Collections.emptySet()).stream()
                  .forEach(header -> {
                    requestBuilder.addHeader(header.getKey(), header.getValue());
                  });
            }

            Request request = requestBuilder.build();

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
        }, this.pool));
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
