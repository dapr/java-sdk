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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class DaprHttp {

  /**
   * HTTP Methods supported.
   */
  enum HttpMethods { GET, PUT, POST, DELETE; }

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
  public Mono<String> invokeAPI(String method, String urlString, Map<String, String> headers) {
    return this.invokeAPI(method, urlString, (String) null, headers);
  }

  /**
   * Invokes an API asynchronously that returns a text payload.
   *
   * @param method    HTTP method.
   * @param urlString url as String.
   * @param content   payload to be posted.
   * @return Asynchronous text
   */
  public Mono<String> invokeAPI(String method, String urlString, String content, Map<String, String> headers) {
    return this.invokeAPI(
      method,
      urlString,
      content == null ? EMPTY_BYTES : content.getBytes(StandardCharsets.UTF_8),
      headers)
      .map(s -> new String(s, StandardCharsets.UTF_8));
  }

  /**
   * Invokes an API asynchronously that returns a text payload.
   *
   * @param method    HTTP method.
   * @param urlString url as String.
   * @param content   payload to be posted.
   * @return Asynchronous text
   */
  public Mono<byte[]> invokeAPI(String method, String urlString, byte[] content, Map<String, String> headers) {
    return Mono.fromFuture(CompletableFuture.supplyAsync(
        () -> {
          try {
            String requestId = UUID.randomUUID().toString();
            RequestBody body;

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
            if (HttpMethods.GET.name().equals(method)) {
              requestBuilder.get();
            } else if (HttpMethods.DELETE.name().equals(method)) {
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
              byte[] responseBody = response.body().bytes();
              if (!response.isSuccessful()) {
                DaprError error = this.parseDaprError(responseBody);
                if ((error != null) && (error.getErrorCode() != null) && (error.getMessage() != null)) {
                  throw new DaprException(error);
                }

                throw new IOException("Unknown error.");
              }
              return responseBody == null ? EMPTY_BYTES : responseBody;
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
  private static DaprError parseDaprError(byte[] json) throws IOException {
    if (json == null) {
      return null;
    }

    return OBJECT_MAPPER.readValue(json, DaprError.class);
  }

}
