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
import io.dapr.exceptions.DaprError;
import io.dapr.exceptions.DaprException;
import io.dapr.internal.exceptions.DaprHttpException;
import io.dapr.utils.Version;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
      Set.of("grpc-trace-bin", "traceparent", "tracestate", "baggage");

  /**
   * Object mapper to parse DaprError with or without details.
   */
  private static final ObjectMapper DAPR_ERROR_DETAILS_OBJECT_MAPPER = new ObjectMapper();

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
    TRACE,
    PATCH
  }

  public static class Response {
    private final byte[] body;
    private final Map<String, String> headers;
    private final int statusCode;

    /**
     * Represents a HTTP response.
     *
     * @param body       The body of the http response.
     * @param headers    The headers of the http response.
     * @param statusCode The status code of the http response.
     */
    public Response(byte[] body, Map<String, String> headers, int statusCode) {
      this.body = body == null ? EMPTY_BYTES : Arrays.copyOf(body, body.length);
      this.headers = headers == null ? null : Collections.unmodifiableMap(headers);
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
  private static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=utf-8";

  /**
   * Empty input or output.
   */
  private static final byte[] EMPTY_BYTES = new byte[0];

  /**
   * Empty Body Publisher.
   */
  private static final HttpRequest.BodyPublisher EMPTY_BODY_PUBLISHER = HttpRequest.BodyPublishers.noBody();

  /**
   * Endpoint used to communicate to Dapr's HTTP endpoint.
   */
  private final URI uri;

  /**
   * Dapr API Token required to interact with DAPR APIs.
   */
  private final String daprApiToken;

  /**
   * Http client request read timeout.
   */
  private final Duration readTimeout;

  /**
   * Http client used for all API calls.
   */
  private final HttpClient httpClient;

  /**
   * Creates a new instance of {@link DaprHttp}.
   *
   * @param hostname   Hostname for calling Dapr. (e.g. "127.0.0.1")
   * @param port       Port for calling Dapr. (e.g. 3500)
   * @param readTimeout HTTP request read timeout
   * @param httpClient RestClient used for all API calls in this new instance.
   */
  DaprHttp(String hostname, int port, String daprApiToken, Duration readTimeout, HttpClient httpClient) {
    this.uri = URI.create(DEFAULT_HTTP_SCHEME + "://" + hostname + ":" + port);
    this.daprApiToken = daprApiToken;
    this.readTimeout = readTimeout;
    this.httpClient = httpClient;
  }

  /**
   * Creates a new instance of {@link DaprHttp}.
   *
   * @param uri        Endpoint for calling Dapr.
   * @param readTimeout HTTP request read timeout
   * @param httpClient RestClient used for all API calls in this new instance.
   */
  DaprHttp(String uri, String daprApiToken, Duration readTimeout, HttpClient httpClient) {
    this.uri = URI.create(uri);
    this.daprApiToken = daprApiToken;
    this.readTimeout = readTimeout;
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
      ContextView context) {
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
      ContextView context) {

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
      ContextView context) {
    // fromCallable() is needed so the invocation does not happen early, causing a hot mono.
    return Mono.fromCallable(() -> doInvokeApi(method, headers, pathSegments, urlParameters, content, context))
        .flatMap(Mono::fromFuture);
  }

  /**
   * Shutdown call is not necessary for HttpClient.
   * @see HttpClient
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
  private CompletableFuture<Response> doInvokeApi(
      String method,
      Map<String, String> headers,
      String[] pathSegments,
      Map<String, List<String>> urlParameters,
      byte[] content,
      ContextView context) {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();

    requestBuilder.uri(createUri(uri, pathSegments, urlParameters));
    addHeader(requestBuilder, Headers.DAPR_USER_AGENT, Version.getSdkVersion());
    addHeader(requestBuilder, HEADER_DAPR_REQUEST_ID, UUID.randomUUID().toString());
    addHeader(requestBuilder, "Content-Type", getContentType(headers));
    addHeaders(requestBuilder, headers);

    if (daprApiToken != null) {
      addHeader(requestBuilder, Headers.DAPR_API_TOKEN, daprApiToken);
    }

    if (context != null) {
      context.stream()
          .filter(entry -> ALLOWED_CONTEXT_IN_HEADERS.contains(entry.getKey().toString().toLowerCase()))
          .forEach(entry -> addHeader(requestBuilder, entry.getKey().toString(), entry.getValue().toString()));
    }

    HttpRequest.BodyPublisher body = getBodyPublisher(content);

    if (HttpMethods.GET.name().equals(method)) {
      requestBuilder.GET();
    } else if (HttpMethods.DELETE.name().equals(method)) {
      requestBuilder.DELETE();
    } else if (HttpMethods.HEAD.name().equals(method)) {
      // HTTP HEAD is not exposed as a normal method
      requestBuilder.method(HttpMethods.HEAD.name(), EMPTY_BODY_PUBLISHER);
    } else {
      requestBuilder.method(method, body);
    }

    HttpRequest request = requestBuilder.timeout(readTimeout).build();

    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
        .thenApply(this::createResponse);
  }

  private static String getContentType(Map<String, String> headers) {
    String result = headers != null ? headers.get(Metadata.CONTENT_TYPE) : null;

    return result == null ? MEDIA_TYPE_APPLICATION_JSON : result;
  }

  private static URI createUri(URI uri, String[] pathSegments, Map<String, List<String>> urlParameters) {
    String path = createPath(uri, pathSegments);
    String query = createQuery(urlParameters);
    StringBuilder result = new StringBuilder();

    result.append(uri.getScheme()).append("://").append(uri.getAuthority()).append(path);

    if (query != null) {
      result.append("?").append(query);
    }

    try {
      return URI.create(result.toString());
    } catch (IllegalArgumentException exception) {
      throw new DaprException(exception);
    }
  }

  private static String createPath(URI uri, String[] pathSegments) {
    String basePath = uri.getPath();

    if (pathSegments == null || pathSegments.length == 0) {
      return basePath;
    }

    StringBuilder pathBuilder = new StringBuilder(basePath);

    if (!basePath.endsWith("/")) { // Add a "/" if needed
      pathBuilder.append("/");
    }

    for (String segment : pathSegments) {
      if (segment == null || segment.isEmpty()) {
        continue; // Skip empty segments
      }

      pathBuilder.append(encodePathSegment(segment)).append("/"); // Encode each segment
    }

    pathBuilder.deleteCharAt(pathBuilder.length() - 1); // Remove the trailing "/"

    return pathBuilder.toString();
  }

  private static String createQuery(Map<String, List<String>> urlParameters) {
    if (urlParameters == null || urlParameters.isEmpty()) {
      return null;
    }

    StringBuilder queryBuilder = new StringBuilder();

    for (Map.Entry<String, List<String>> entry : urlParameters.entrySet()) {
      String key = entry.getKey();

      if (key == null || key.isEmpty()) {
        continue; // Skip empty keys
      }

      List<String> values = entry.getValue();

      for (String value : values) {
        if (queryBuilder.length() > 0) {
          queryBuilder.append("&");
        }

        queryBuilder.append(encodeQueryParam(key, value)); // Encode key and value
      }
    }

    return queryBuilder.toString();
  }

  private static String encodePathSegment(String segment) {
    return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"); // Encode and handle spaces
  }

  private static String encodeQueryParam(String key, String value) {
    return URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static void addHeader(HttpRequest.Builder requestBuilder, String name, String value) {
    requestBuilder.header(name, value);
  }

  private static void addHeaders(HttpRequest.Builder requestBuilder, Map<String, String> headers) {
    if (headers == null || headers.isEmpty()) {
      return;
    }

    headers.forEach((k, v) -> addHeader(requestBuilder, k, v));
  }

  private static HttpRequest.BodyPublisher getBodyPublisher(byte[] content) {
    return HttpRequest.BodyPublishers.ofByteArray(Objects.requireNonNullElse(content, EMPTY_BYTES));
  }

  private Response createResponse(HttpResponse<byte[]> httpResponse) {
    Optional<String> headerValue = httpResponse.headers().firstValue("Metadata.statuscode");
    int httpStatusCode = parseHttpStatusCode(headerValue, httpResponse.statusCode());
    byte[] body = getBodyBytesOrEmptyArray(httpResponse.body());

    if (!DaprHttpException.isSuccessfulHttpStatusCode(httpStatusCode)) {
      DaprError error = parseDaprError(body);

      if (error != null) {
        throw new DaprException(error, body, httpStatusCode);
      } else {
        throw new DaprException("UNKNOWN", "", body, httpStatusCode);
      }
    }

    Map<String, String> responseHeaders = new HashMap<>();
    httpResponse.headers().map().forEach((k, v) -> responseHeaders.put(k, v.isEmpty() ? null : v.get(0)));

    return new Response(body, responseHeaders, httpStatusCode);
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
      return DAPR_ERROR_DETAILS_OBJECT_MAPPER.readValue(json, DaprError.class);
    } catch (IOException e) {
      // Could not parse DaprError. Return null.
      return null;
    }
  }

  private static byte[] getBodyBytesOrEmptyArray(byte[] body) {
    return body == null ? EMPTY_BYTES : body;
  }

  private static int parseHttpStatusCode(Optional<String> headerValue, int defaultStatusCode) {
    if (headerValue.isEmpty()) {
      return defaultStatusCode;
    }

    // Metadata used to override status code with code received from HTTP binding.
    try {
      int httpStatusCode = Integer.parseInt(headerValue.get());
      if (DaprHttpException.isValidHttpStatusCode(httpStatusCode)) {
        return httpStatusCode;
      }
      return defaultStatusCode;
    } catch (NumberFormatException nfe) {
      return defaultStatusCode;
    }
  }
}
