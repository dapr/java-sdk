/*
 * Copyright 2026 The Dapr Authors
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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * An HTTP client pre-configured to invoke a specific Dapr application via the
 * service invocation API.
 *
 * <p>Obtained via {@link DaprClient#invokeHttpClient(String)}. Relative paths
 * passed to {@link #newRequestBuilder(String)} resolve against
 * {@code {daprHttpEndpoint}/v1.0/invoke/{appId}/method/} and the configured
 * {@code dapr-api-token} header is attached automatically when present.
 *
 * <p>Example:
 * <pre>{@code
 * DaprInvokeHttpClient invoker = daprClient.invokeHttpClient("orderprocessor");
 *
 * HttpRequest request = invoker.newRequestBuilder("orders")
 *     .header("Content-Type", "application/json")
 *     .POST(HttpRequest.BodyPublishers.ofString(json))
 *     .build();
 *
 * HttpResponse<String> response = invoker.send(request, HttpResponse.BodyHandlers.ofString());
 * }</pre>
 *
 * <p><b>Migrating from {@code DaprClient.invokeMethod}:</b> the deprecated
 * {@code invokeMethod} APIs serialized request bodies through the configured
 * {@link io.dapr.serializer.DaprObjectSerializer} (JSON by default), so a {@code String}
 * payload was sent as a JSON string literal (e.g. {@code "hello"}). This client does
 * <em>not</em> serialize bodies — callers supply raw
 * {@link HttpRequest.BodyPublisher BodyPublisher}s. To preserve the previous JSON
 * encoding, use {@link DaprBodyPublishers#json(Object)}:
 * <pre>{@code
 * HttpRequest request = invoker.newRequestBuilder("orders")
 *     .header("Content-Type", "application/json")
 *     .POST(DaprBodyPublishers.json(order))
 *     .build();
 * }</pre>
 *
 * <p>This class is not {@link AutoCloseable}: the underlying {@link HttpClient} is
 * managed by the SDK and shared across all clients created from a single
 * {@link DaprClientBuilder}; closing the owning {@link DaprClient} releases it.
 */
public class DaprInvokeHttpClient {

  private final HttpClient httpClient;
  private final URI baseUri;
  private final String daprApiToken;
  private final Duration readTimeout;

  DaprInvokeHttpClient(HttpClient httpClient, URI baseUri, String daprApiToken, Duration readTimeout) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
    this.daprApiToken = daprApiToken;
    this.readTimeout = readTimeout;
  }

  /**
   * Returns the underlying JDK {@link HttpClient}. Useful as an escape hatch when
   * callers need full control over a request (for example to bypass the configured
   * base URI for a one-off call).
   *
   * @return the shared underlying HTTP client.
   */
  public HttpClient httpClient() {
    return httpClient;
  }

  /**
   * Returns the base URI against which {@link #newRequestBuilder(String)} resolves
   * relative paths. Always ends with a trailing slash, e.g.
   * {@code http://localhost:3500/v1.0/invoke/orderprocessor/method/}.
   *
   * @return the resolved invoke base URI.
   */
  public URI baseUri() {
    return baseUri;
  }

  /**
   * Creates an {@link HttpRequest.Builder} pre-bound to the Dapr invoke URL for the
   * configured app id, with the {@code dapr-api-token} header attached (when one is
   * configured) and the SDK's HTTP read timeout applied.
   *
   * <p>The {@code relativePath} is resolved against {@link #baseUri()} via
   * {@link URI#resolve(String)}. Per {@link URI#resolve(String)} semantics, a leading
   * slash replaces the entire path, so callers should typically pass a path
   * <em>without</em> a leading slash (e.g. {@code "orders/42"}).
   *
   * @param relativePath path appended to the invoke prefix.
   * @return a request builder ready to be customized and built.
   */
  public HttpRequest.Builder newRequestBuilder(String relativePath) {
    Objects.requireNonNull(relativePath, "relativePath");
    HttpRequest.Builder builder = HttpRequest.newBuilder().uri(baseUri.resolve(relativePath));
    if (daprApiToken != null && !daprApiToken.isEmpty()) {
      builder.header(Headers.DAPR_API_TOKEN, daprApiToken);
    }
    if (readTimeout != null && !readTimeout.isZero() && !readTimeout.isNegative()) {
      builder.timeout(readTimeout);
    }
    return builder;
  }

  /**
   * Sends a request synchronously using the underlying HTTP client.
   * Equivalent to {@code httpClient().send(request, bodyHandler)}.
   *
   * @param request     the request to send.
   * @param bodyHandler handler for the response body.
   * @param <T>         the response body type.
   * @return the HTTP response.
   * @throws IOException          if an I/O error occurs.
   * @throws InterruptedException if the operation is interrupted.
   */
  public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> bodyHandler)
      throws IOException, InterruptedException {
    return httpClient.send(request, bodyHandler);
  }

  /**
   * Sends a request asynchronously using the underlying HTTP client.
   * Equivalent to {@code httpClient().sendAsync(request, bodyHandler)}.
   *
   * @param request     the request to send.
   * @param bodyHandler handler for the response body.
   * @param <T>         the response body type.
   * @return a future completing with the HTTP response.
   */
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> bodyHandler) {
    return httpClient.sendAsync(request, bodyHandler);
  }
}
