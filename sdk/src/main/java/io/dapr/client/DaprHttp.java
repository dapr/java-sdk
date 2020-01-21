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
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DaprHttp {

  /**
   * HTTP Methods supported.
   */
  public enum HttpMethods {GET, PUT, POST, DELETE;}

  public static class Response {
    private byte[] body;
    private Map<String, String> headers;
    private int statusCode;

    public Response(byte[] body, Map<String, String> headers, int statusCode) {
      this.body = body;
      this.headers = headers;
      this.statusCode = statusCode;
    }

    public byte[] getBody() {
      return body;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }

    public int getStatusCode() {
      return statusCode;
    }
  }

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
   * Port used to communicate to Dapr's HTTP endpoint.
   */
  private final int port;

  /**
   * Http client used for all API calls.
   */
  private final OkHttpClient httpClient;

  /**
   * Creates a new instance of {@link DaprHttp}.
   *
   * @param port       Port for calling Dapr. (e.g. 3500)
   * @param httpClient RestClient used for all API calls in this new instance.
   */
  DaprHttp(int port, OkHttpClient httpClient) {
    this.port = port;
    this.httpClient = httpClient;
  }

  /**
   * Invokes an API asynchronously without payload that returns a text payload.
   *
   * @param method    HTTP method.
   * @param urlString url as String.
   * @param urlParameters URL parameters
   * @param headers HTTP headers.
   * @return Asynchronous text
   */
  public Mono<Response> invokeAPI(String method, String urlString, Map<String, String> urlParameters, Map<String, String> headers) {
    return this.invokeAPI(method, urlString, urlParameters, (byte[]) null, headers);
  }

  /**
   * Invokes an API asynchronously that returns a text payload.
   *
   * @param method    HTTP method.
   * @param urlString url as String.
   * @param urlParameters Parameters in the URL
   * @param content   payload to be posted.
   * @param headers HTTP headers.
   * @return Asynchronous response
   */
  public Mono<Response> invokeAPI(String method, String urlString, Map<String, String> urlParameters, String content, Map<String, String> headers) {
    return this.invokeAPI(method, urlString, urlParameters, content == null ? EMPTY_BYTES : content.getBytes(StandardCharsets.UTF_8), headers);
  }

  /**
   * Invokes an API asynchronously that returns a text payload.
   *
   * @param method    HTTP method.
   * @param urlString url as String.
   * @param urlParameters Parameters in the URL
   * @param content   payload to be posted.
   * @param headers HTTP headers.
   * @return Asynchronous response
   */
  public Mono<Response> invokeAPI(String method, String urlString, Map<String, String> urlParameters, byte[] content, Map<String, String> headers) {
    return Mono.fromCallable(
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
              body = RequestBody.Companion.create(content, mediaType);
            }
            HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
            urlBuilder.scheme("http").host(Constants.DEFAULT_HOSTNAME).port(this.port).addPathSegments(urlString);
            Optional.ofNullable(urlParameters).orElse(Collections.emptyMap()).entrySet().stream()
                .forEach(urlParameter -> urlBuilder.addQueryParameter(urlParameter.getKey(), urlParameter.getValue()));

            Request.Builder requestBuilder = new Request.Builder()
                .url(urlBuilder.build())
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

            try (okhttp3.Response response = this.httpClient.newCall(request).execute()) {
              if (!response.isSuccessful()) {
                DaprError error = parseDaprError(response.body().bytes());
                if ((error != null) && (error.getErrorCode() != null) && (error.getMessage() != null)) {
                  throw new RuntimeException(new DaprException(error));
                }

                throw new RuntimeException("Unknown error.");
              }

              Map<String, String> mapHeaders = new HashMap<>();
              byte[] result = response.body().bytes();
              response.headers().forEach(pair -> {
                mapHeaders.put(pair.getFirst(), pair.getSecond());
              });
              return new Response(result == null ? EMPTY_BYTES : result, mapHeaders, response.code());
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
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
