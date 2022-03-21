/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.domain.Metadata;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprError;
import io.dapr.exceptions.DaprException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DaprHttp implements AutoCloseable {

  /**
   * Dapr API used in this client.
   */
  public static final String API_VERSION = "v1.0";

  /**
   * Dapr alpha API used in this client.
   */
  public static final String ALPHA_1_API_VERSION = "v1.0-alpha1";

  /**
   * Header used for request id in Dapr.
   */
  private static final String HEADER_DAPR_REQUEST_ID = "X-DaprRequestId";

  /**
   * Dapr's http default scheme.
   */
  private static final String DEFAULT_HTTP_SCHEME = "http";

  /**
   * Context entries allowed to be in HTTP Headers.
   */
  private static final Set<String> ALLOWED_CONTEXT_IN_HEADERS =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("grpc-trace-bin", "traceparent", "tracestate")));

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
   * @param pathSegments  Array of path segments ("/a/b/c" maps to ["a", "b", "c"]).
   * @param urlParameters URL parameters
   * @param headers       HTTP headers.
   * @param context       OpenTelemetry's Context.
   * @return Asynchronous text
   */
  public Mono<Response> invokeApi(
      String method,
      String[] pathSegments,
      Map<String, List<String>> urlParameters,
      Map<String, String> headers,
      Context context) {
    return this.invokeApi(method, pathSegments, urlParameters, (byte[]) null, headers, context);
  }

  /**
   * Invokes an API asynchronously that returns a text payload.
   *
   * @param method        HTTP method.
   * @param pathSegments  Array of path segments ("/a/b/c" maps to ["a", "b", "c"]).
   * @param urlParameters Parameters in the URL
   * @param content       payload to be posted.
   * @param headers       HTTP headers.
   * @param context       OpenTelemetry's Context.
   * @return Asynchronous response
   */
  public Mono<Response> invokeApi(
      String method,
      String[] pathSegments,
      Map<String, List<String>> urlParameters,
      String content,
      Map<String, String> headers,
      Context context) {

    return this.invokeApi(
        method, pathSegments, urlParameters, content == null
            ? EMPTY_BYTES
            : content.getBytes(StandardCharsets.UTF_8), headers, context);
  }

  /**
   * Invokes an API asynchronously that returns a text payload.
   *
   * @param method        HTTP method.
   * @param pathSegments  Array of path segments ("/a/b/c" maps to ["a", "b", "c"]).
   * @param urlParameters Parameters in the URL
   * @param content       payload to be posted.
   * @param headers       HTTP headers.
   * @param context       OpenTelemetry's Context.
   * @return Asynchronous response
   */
  public Mono<Response> invokeApi(
          String method,
          String[] pathSegments,
          Map<String, List<String>> urlParameters,
          byte[] content,
          Map<String, String> headers,
          Context context) {
    // fromCallable() is needed so the invocation does not happen early, causing a hot mono.
    return Mono.fromCallable(() -> doInvokeApi(method, pathSegments, urlParameters, content, headers, context))
        .flatMap(f -> Mono.fromFuture(f));
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
   * @param pathSegments  Array of path segments (/a/b/c -> ["a", "b", "c"]).
   * @param urlParameters Parameters in the URL
   * @param content       payload to be posted.
   * @param headers       HTTP headers.
   * @param context       OpenTelemetry's Context.
   * @return CompletableFuture for Response.
   */
  private CompletableFuture<Response> doInvokeApi(String method,
                               String[] pathSegments,
                               Map<String, List<String>> urlParameters,
                               byte[] content, Map<String, String> headers,
                               Context context) {
    final String requestId = UUID.randomUUID().toString();
    RequestBody body;

    String contentType = headers != null ? headers.get(Metadata.CONTENT_TYPE) : null;
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
    for (String pathSegment : pathSegments) {
      urlBuilder.addPathSegment(pathSegment);
    }
    Optional.ofNullable(urlParameters).orElse(Collections.emptyMap()).entrySet().stream()
        .forEach(urlParameter ->
            Optional.ofNullable(urlParameter.getValue()).orElse(Collections.emptyList()).stream()
              .forEach(urlParameterValue ->
                  urlBuilder.addQueryParameter(urlParameter.getKey(), urlParameterValue)));

    Request.Builder requestBuilder = new Request.Builder()
        .url(urlBuilder.build())
        .addHeader(HEADER_DAPR_REQUEST_ID, requestId);
    if (context != null) {
      context.stream()
          .filter(entry -> ALLOWED_CONTEXT_IN_HEADERS.contains(entry.getKey().toString().toLowerCase()))
          .forEach(entry -> requestBuilder.addHeader(entry.getKey().toString(), entry.getValue().toString()));
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


    CompletableFuture<Response> future = new CompletableFuture<>();
    this.httpClient.newCall(request).enqueue(new ResponseFutureCallback(future));
    return future;
  }

  /**
   * Tries to parse an error from Dapr response body.
   *
   * @param json Response body from Dapr.
   * @return DaprError or null if could not parse.
   */
  private static DaprError parseDaprError(byte[] json) {
    if ((json == null) || (json.length == 0)) {
      return null;
    }

    try {
      return OBJECT_MAPPER.readValue(json, DaprError.class);
    } catch (IOException e) {
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

  /**
   * Converts the okhttp3 response into the response object expected internally by the SDK.
   */
  private static class ResponseFutureCallback implements Callback {
    private final CompletableFuture<Response> future;

    public ResponseFutureCallback(CompletableFuture<Response> future) {
      this.future = future;
    }

    @Override
    public void onFailure(Call call, IOException e) {
      future.completeExceptionally(e);
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
      if (!response.isSuccessful()) {
        try {
          DaprError error = parseDaprError(getBodyBytesOrEmptyArray(response));
          if ((error != null) && (error.getErrorCode() != null)) {
            if (error.getMessage() != null) {
              future.completeExceptionally(new DaprException(error));
            } else {
              future.completeExceptionally(
                  new DaprException(error.getErrorCode(), "HTTP status code: " + response.code()));
            }
            return;
          }

          future.completeExceptionally(new DaprException("UNKNOWN", "HTTP status code: " + response.code()));
          return;
        } catch (DaprException e) {
          future.completeExceptionally(e);
          return;
        }
      }

      Map<String, String> mapHeaders = new HashMap<>();
      byte[] result = getBodyBytesOrEmptyArray(response);
      response.headers().forEach(pair -> {
        mapHeaders.put(pair.getFirst(), pair.getSecond());
      });
      future.complete(new Response(result, mapHeaders, response.code()));
    }
  }

}
