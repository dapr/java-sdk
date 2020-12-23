/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprError;
import io.dapr.exceptions.DaprException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.HttpTraceContext;
import io.opentelemetry.context.Context;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DaprHttp implements AutoCloseable {

  /**
   * Dapr API used in this client.
   */
  public static final String API_VERSION = "v1.0";

  /**
   * Header used for request id in Dapr.
   */
  private static final String HEADER_DAPR_REQUEST_ID = "X-DaprRequestId";

  /**
   * Dapr's http default scheme.
   */
  private static final String DEFAULT_HTTP_SCHEME = "http";

  /**
   * Sets the headers for OpenTelemetry SDK.
   */
  private static final HttpTraceContext.Setter<Request.Builder> OPENTELEMETRY_SETTER =
      (requestBuilder, key, value) -> requestBuilder.addHeader(key, value);

  /**
   * HTTP Methods supported.
   */
  public enum HttpMethods {
    NONE,
    GET,
    PUT,
    POST,
    DELETE,
    HEAD,
    CONNECT,
    OPTIONS,
    TRACE
  }

  public static class Response {
    private byte[] body;
    private Map<String, String> headers;
    private int statusCode;

    /**
     * Represents an http response.
     *
     * @param body       The body of the http response.
     * @param headers    The headers of the http response.
     * @param statusCode The status code of the http response.
     */
    public Response(byte[] body, Map<String, String> headers, int statusCode) {
      this.body = body == null ? EMPTY_BYTES : Arrays.copyOf(body, body.length);
      this.headers = headers;
      this.statusCode = statusCode;
    }

    public byte[] getBody() {
      return Arrays.copyOf(this.body, this.body.length);
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
   * Hostname used to communicate to Dapr's HTTP endpoint.
   */
  private final String hostname;

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
   * @param hostname   Hostname for calling Dapr. (e.g. "127.0.0.1")
   * @param port       Port for calling Dapr. (e.g. 3500)
   * @param httpClient RestClient used for all API calls in this new instance.
   */
  DaprHttp(String hostname, int port, OkHttpClient httpClient) {
    this.hostname = hostname;
    this.port = port;
    this.httpClient = httpClient;
  }

  /**
   * Invokes an API asynchronously without payload that returns a text payload.
   *
   * @param method        HTTP method.
   * @param path          Array of path segments ("/a/b/c" maps to ["a", "b", "c"]).
   * @param urlParameters URL parameters
   * @param headers       HTTP headers.
   * @param context       OpenTelemetry's Context.
   * @return Asynchronous text
   */
  public Mono<Response> invokeApi(
      String method,
      String[] path,
      Map<String, String> urlParameters,
      Map<String, String> headers,
      Context context) {
    return this.invokeApi(method, path, urlParameters, (byte[]) null, headers, context);
  }

  /**
   * Invokes an API asynchronously that returns a text payload.
   *
   * @param method        HTTP method.
   * @param path          Array of path segments ("/a/b/c" maps to ["a", "b", "c"]).
   * @param urlParameters Parameters in the URL
   * @param content       payload to be posted.
   * @param headers       HTTP headers.
   * @param context       OpenTelemetry's Context.
   * @return Asynchronous response
   */
  public Mono<Response> invokeApi(
      String method,
      String[] path,
      Map<String, String> urlParameters,
      String content,
      Map<String, String> headers,
      Context context) {

    return this.invokeApi(
        method, path, urlParameters, content == null
            ? EMPTY_BYTES
            : content.getBytes(StandardCharsets.UTF_8), headers, context);
  }

  /**
   * Invokes an API asynchronously that returns a text payload.
   *
   * @param method        HTTP method.
   * @param path          Array of path segments ("/a/b/c" maps to ["a", "b", "c"]).
   * @param urlParameters Parameters in the URL
   * @param content       payload to be posted.
   * @param headers       HTTP headers.
   * @param context       OpenTelemetry's Context.
   * @return Asynchronous response
   */
  public Mono<Response> invokeApi(
          String method,
          String[] path,
          Map<String, String> urlParameters,
          byte[] content,
          Map<String, String> headers,
          Context context) {
    return Mono.fromCallable(() -> doInvokeApi(method, path, urlParameters, content, headers, context));
  }

  /**
   * Shutdown call is not necessary for OkHttpClient.
   * @see OkHttpClient
   */
  @Override
  public void close() {
    // No code needed
  }

  /**
   * Invokes an API that returns a text payload.
   *
   * @param method        HTTP method.
   * @param path          Array of path segments (/a/b/c -> ["a", "b", "c"]).
   * @param urlParameters Parameters in the URL
   * @param content       payload to be posted.
   * @param headers       HTTP headers.
   * @param context       OpenTelemetry's Context.
   * @return Response
   */
  private Response doInvokeApi(String method,
                               String[] path,
                               Map<String, String> urlParameters,
                               byte[] content, Map<String, String> headers,
                               Context context) throws IOException {
    final String requestId = UUID.randomUUID().toString();
    RequestBody body;

    String contentType = headers != null ? headers.get("content-type") : null;
    MediaType mediaType = contentType == null ? MEDIA_TYPE_APPLICATION_JSON : MediaType.get(contentType);
    if (content == null) {
      body = mediaType.equals(MEDIA_TYPE_APPLICATION_JSON)
          ? REQUEST_BODY_EMPTY_JSON
          : RequestBody.Companion.create(new byte[0], mediaType);
    } else {
      body = RequestBody.Companion.create(content, mediaType);
    }
    HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
    urlBuilder.scheme(DEFAULT_HTTP_SCHEME)
        .host(this.hostname)
        .port(this.port);
    for (String pathSegment : path) {
      urlBuilder.addPathSegment(pathSegment);
    }
    Optional.ofNullable(urlParameters).orElse(Collections.emptyMap()).entrySet().stream()
        .forEach(urlParameter -> urlBuilder.addQueryParameter(urlParameter.getKey(), urlParameter.getValue()));

    Request.Builder requestBuilder = new Request.Builder()
        .url(urlBuilder.build())
        .addHeader(HEADER_DAPR_REQUEST_ID, requestId);
    if (context != null) {
      OpenTelemetry.getGlobalPropagators().getTextMapPropagator().inject(context, requestBuilder, OPENTELEMETRY_SETTER);
    }
    if (HttpMethods.GET.name().equals(method)) {
      requestBuilder.get();
    } else if (HttpMethods.DELETE.name().equals(method)) {
      requestBuilder.delete();
    } else {
      requestBuilder.method(method, body);
    }

    String daprApiToken = Properties.API_TOKEN.get();
    if (daprApiToken != null) {
      requestBuilder.addHeader(Headers.DAPR_API_TOKEN, daprApiToken);
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
        DaprError error = parseDaprError(getBodyBytesOrEmptyArray(response));
        if ((error != null) && (error.getErrorCode() != null) && (error.getMessage() != null)) {
          throw new DaprException(error);
        }

        throw new DaprException("UNKNOWN", "HTTP status code: " + response.code());
      }

      Map<String, String> mapHeaders = new HashMap<>();
      byte[] result = getBodyBytesOrEmptyArray(response);
      response.headers().forEach(pair -> {
        mapHeaders.put(pair.getFirst(), pair.getSecond());
      });
      return new Response(result, mapHeaders, response.code());
    }
  }

  /**
   * Tries to parse an error from Dapr response body.
   *
   * @param json Response body from Dapr.
   * @return DaprError or null if could not parse.
   */
  private static DaprError parseDaprError(byte[] json) throws IOException {
    if ((json == null) || (json.length == 0)) {
      return null;
    }

    try {
      return OBJECT_MAPPER.readValue(json, DaprError.class);
    } catch (JsonParseException e) {
      throw new DaprException("UNKNOWN", new String(json, StandardCharsets.UTF_8));
    }
  }

  private static byte[] getBodyBytesOrEmptyArray(okhttp3.Response response) throws IOException {
    ResponseBody body = response.body();
    if (body != null) {
      return body.bytes();
    }

    return EMPTY_BYTES;
  }

}