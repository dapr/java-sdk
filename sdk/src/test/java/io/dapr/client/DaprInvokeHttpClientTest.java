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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DaprInvokeHttpClientTest {

  private static final URI BASE_URI = URI.create("http://localhost:3500/v1.0/invoke/orderprocessor/method/");
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

  private HttpClient httpClient;

  @BeforeEach
  public void setUp() {
    httpClient = mock(HttpClient.class);
  }

  @Test
  public void constructor_rejectsNullHttpClient() {
    assertThrows(NullPointerException.class,
        () -> new DaprInvokeHttpClient(null, BASE_URI, "token", READ_TIMEOUT));
  }

  @Test
  public void constructor_rejectsNullBaseUri() {
    assertThrows(NullPointerException.class,
        () -> new DaprInvokeHttpClient(httpClient, null, "token", READ_TIMEOUT));
  }

  @Test
  public void accessors_returnConfiguredValues() {
    DaprInvokeHttpClient invoker = new DaprInvokeHttpClient(httpClient, BASE_URI, "token", READ_TIMEOUT);

    assertSame(httpClient, invoker.httpClient());
    assertEquals(BASE_URI, invoker.baseUri());
  }

  @Test
  public void newRequestBuilder_resolvesRelativePathAgainstBaseUri() {
    DaprInvokeHttpClient invoker = new DaprInvokeHttpClient(httpClient, BASE_URI, null, null);

    HttpRequest request = invoker.newRequestBuilder("orders/42").GET().build();

    assertEquals("http://localhost:3500/v1.0/invoke/orderprocessor/method/orders/42",
        request.uri().toString());
  }

  @Test
  public void newRequestBuilder_attachesApiTokenHeaderWhenConfigured() {
    DaprInvokeHttpClient invoker = new DaprInvokeHttpClient(httpClient, BASE_URI, "xyz", null);

    HttpRequest request = invoker.newRequestBuilder("orders").GET().build();

    assertEquals("xyz", request.headers().firstValue(Headers.DAPR_API_TOKEN).orElse(null));
  }

  @Test
  public void newRequestBuilder_omitsApiTokenHeaderWhenTokenNull() {
    DaprInvokeHttpClient invoker = new DaprInvokeHttpClient(httpClient, BASE_URI, null, null);

    HttpRequest request = invoker.newRequestBuilder("orders").GET().build();

    assertFalse(request.headers().map().containsKey(Headers.DAPR_API_TOKEN));
  }

  @Test
  public void newRequestBuilder_omitsApiTokenHeaderWhenTokenEmpty() {
    DaprInvokeHttpClient invoker = new DaprInvokeHttpClient(httpClient, BASE_URI, "", null);

    HttpRequest request = invoker.newRequestBuilder("orders").GET().build();

    assertFalse(request.headers().map().containsKey(Headers.DAPR_API_TOKEN));
  }

  @Test
  public void newRequestBuilder_appliesReadTimeoutWhenConfigured() {
    DaprInvokeHttpClient invoker = new DaprInvokeHttpClient(httpClient, BASE_URI, null, READ_TIMEOUT);

    HttpRequest request = invoker.newRequestBuilder("orders").GET().build();

    assertEquals(Optional.of(READ_TIMEOUT), request.timeout());
  }

  @Test
  public void newRequestBuilder_omitsTimeoutWhenNullOrZeroOrNegative() {
    HttpRequest nullTimeoutRequest = new DaprInvokeHttpClient(httpClient, BASE_URI, null, null)
        .newRequestBuilder("a").GET().build();
    HttpRequest zeroTimeoutRequest = new DaprInvokeHttpClient(httpClient, BASE_URI, null, Duration.ZERO)
        .newRequestBuilder("a").GET().build();
    HttpRequest negativeTimeoutRequest = new DaprInvokeHttpClient(
        httpClient, BASE_URI, null, Duration.ofSeconds(-1))
        .newRequestBuilder("a").GET().build();

    assertTrue(nullTimeoutRequest.timeout().isEmpty());
    assertTrue(zeroTimeoutRequest.timeout().isEmpty());
    assertTrue(negativeTimeoutRequest.timeout().isEmpty());
  }

  @Test
  public void newRequestBuilder_rejectsNullRelativePath() {
    DaprInvokeHttpClient invoker = new DaprInvokeHttpClient(httpClient, BASE_URI, null, null);

    assertThrows(NullPointerException.class, () -> invoker.newRequestBuilder(null));
  }

  @Test
  public void send_delegatesToUnderlyingHttpClient() throws Exception {
    DaprInvokeHttpClient invoker = new DaprInvokeHttpClient(httpClient, BASE_URI, null, null);
    HttpRequest request = invoker.newRequestBuilder("orders").GET().build();
    @SuppressWarnings("unchecked")
    HttpResponse<String> stubbedResponse = mock(HttpResponse.class);
    BodyHandler<String> handler = BodyHandlers.ofString();
    doReturn(stubbedResponse).when(httpClient).send(same(request), same(handler));

    HttpResponse<String> response = invoker.send(request, handler);

    assertSame(stubbedResponse, response);
    verify(httpClient).send(same(request), same(handler));
  }

  @Test
  public void sendAsync_delegatesToUnderlyingHttpClient() {
    DaprInvokeHttpClient invoker = new DaprInvokeHttpClient(httpClient, BASE_URI, null, null);
    HttpRequest request = invoker.newRequestBuilder("orders").GET().build();
    @SuppressWarnings("unchecked")
    HttpResponse<String> stubbedResponse = mock(HttpResponse.class);
    BodyHandler<String> handler = BodyHandlers.ofString();
    CompletableFuture<HttpResponse<String>> future = CompletableFuture.completedFuture(stubbedResponse);
    when(httpClient.sendAsync(same(request), same(handler))).thenReturn(future);

    CompletableFuture<HttpResponse<String>> result = invoker.sendAsync(request, handler);

    assertSame(future, result);
    verify(httpClient).sendAsync(same(request), any());
  }
}
