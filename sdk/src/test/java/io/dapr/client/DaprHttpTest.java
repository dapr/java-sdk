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

import io.dapr.config.Properties;
import io.dapr.exceptions.DaprErrorDetails;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.dapr.utils.TestUtils.formatIpAddress;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SystemStubsExtension.class)
public class DaprHttpTest {

  private static final int HTTP_OK = 200;
  private static final int HTTP_SERVER_ERROR = 500;
  private static final int HTTP_NO_CONTENT = 204;
  private static final int HTTP_NOT_FOUND = 404;
  private static final String EXPECTED_RESULT =
      "{\"data\":\"ewoJCSJwcm9wZXJ0eUEiOiAidmFsdWVBIiwKCQkicHJvcGVydHlCIjogInZhbHVlQiIKCX0=\"}";
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

  @SystemStub
  private final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  private String sidecarIp;

  private String daprTokenApi;

  private HttpClient httpClient;

  private final ObjectSerializer serializer = new ObjectSerializer();

  @BeforeEach
  public void setUp() {
    sidecarIp = formatIpAddress(Properties.SIDECAR_IP.get());
    daprTokenApi = Properties.API_TOKEN.get();
    httpClient = mock(HttpClient.class);
  }

  @Test
  public void invokeApi_daprApiToken_present() throws IOException {
    byte[] content = serializer.serialize(EXPECTED_RESULT);
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    environmentVariables.set(Properties.API_TOKEN.getEnvName(), "xyz");
    assertEquals("xyz", Properties.API_TOKEN.get());
    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, Properties.API_TOKEN.get(), READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "POST",
        "v1.0/state".split("/"),
        null,
        (byte[]) null,
        null,
        Context.empty()
    );
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);

    verify(httpClient).sendAsync(requestCaptor.capture(), any());

    HttpRequest request = requestCaptor.getValue();

    assertEquals(EXPECTED_RESULT, body);
    assertEquals("POST", request.method());
    assertEquals("http://" + sidecarIp + ":3500/v1.0/state", request.uri().toString());
    assertEquals("xyz", request.headers().firstValue(Headers.DAPR_API_TOKEN).get());
  }

  @Test
  public void invokeApi_daprApiToken_absent() throws IOException {
    byte[] content = serializer.serialize(EXPECTED_RESULT);
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    assertNull(Properties.API_TOKEN.get());
    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "POST",
        "v1.0/state".split("/"),
        null,
        (byte[]) null,
        null,
        Context.empty()
    );
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);

    verify(httpClient).sendAsync(requestCaptor.capture(), any());

    HttpRequest request = requestCaptor.getValue();

    assertEquals(EXPECTED_RESULT, body);
    assertEquals("POST", request.method());
    assertEquals("http://" + sidecarIp + ":3500/v1.0/state", request.uri().toString());
    assertFalse(request.headers().map().containsKey(Headers.DAPR_API_TOKEN));
  }

  @Test
  public void invokeMethod() throws IOException {
    Map<String, String> headers = Map.of(
        "content-type", "text/html",
        "header1", "value1"
    );
    byte[] content = serializer.serialize(EXPECTED_RESULT);
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "POST",
        "v1.0/state".split("/"),
        null,
        (byte[]) null,
        headers,
        Context.empty()
    );
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);

    verify(httpClient).sendAsync(requestCaptor.capture(), any());

    HttpRequest request = requestCaptor.getValue();

    assertEquals(EXPECTED_RESULT, body);
    assertEquals("POST", request.method());
    assertEquals("http://" + sidecarIp + ":3500/v1.0/state", request.uri().toString());
    assertEquals("text/html", request.headers().firstValue("content-type").get());
    assertEquals("value1", request.headers().firstValue("header1").get());
  }

  @Test
  public void invokeMethodIPv6() throws IOException {
    sidecarIp = formatIpAddress("2001:db8:3333:4444:5555:6666:7777:8888");
    Map<String, String> headers = Map.of(
        "content-type", "text/html",
        "header1", "value1"
    );
    byte[] content = serializer.serialize(EXPECTED_RESULT);
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "POST",
        "v1.0/state".split("/"),
        null,
        (byte[]) null,
        headers,
        Context.empty()
    );
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);

    verify(httpClient).sendAsync(requestCaptor.capture(), any());

    HttpRequest request = requestCaptor.getValue();

    assertEquals(EXPECTED_RESULT, body);
    assertEquals("POST", request.method());
    assertEquals("http://" + sidecarIp + ":3500/v1.0/state", request.uri().toString());
    assertEquals("text/html", request.headers().firstValue("content-type").get());
    assertEquals("value1", request.headers().firstValue("header1").get());
  }

  @Test
  public void invokePostMethod() throws IOException {
    byte[] content = serializer.serialize(EXPECTED_RESULT);
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "POST",
        "v1.0/state".split("/"),
        null,
        "",
        null,
        Context.empty()
    );
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);

    verify(httpClient).sendAsync(requestCaptor.capture(), any());

    HttpRequest request = requestCaptor.getValue();

    assertEquals(EXPECTED_RESULT, body);
    assertEquals("POST", request.method());
    assertEquals("http://" + sidecarIp + ":3500/v1.0/state", request.uri().toString());
  }

  @Test
  public void invokeDeleteMethod() throws IOException {
    byte[] content = serializer.serialize(EXPECTED_RESULT);
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "DELETE",
        "v1.0/state".split("/"),
        null,
        (String) null,
        null,
        Context.empty()
    );
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);

    verify(httpClient).sendAsync(requestCaptor.capture(), any());

    HttpRequest request = requestCaptor.getValue();

    assertEquals(EXPECTED_RESULT, body);
    assertEquals("DELETE", request.method());
    assertEquals("http://" + sidecarIp + ":3500/v1.0/state", request.uri().toString());
  }

  @Test
  public void invokePatchMethod() throws IOException {
    byte[] content = serializer.serialize(EXPECTED_RESULT);
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "PATCH",
        "v1.0/state".split("/"),
        null,
        "",
        null,
        Context.empty()
    );
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);

    verify(httpClient).sendAsync(requestCaptor.capture(), any());

    HttpRequest request = requestCaptor.getValue();

    assertEquals(EXPECTED_RESULT, body);
    assertEquals("PATCH", request.method());
    assertEquals("http://" + sidecarIp + ":3500/v1.0/state", request.uri().toString());
  }

  @Test
  public void invokeHeadMethod() {
    MockHttpResponse mockHttpResponse = new MockHttpResponse(HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "HEAD",
        "v1.0/state".split("/"),
        null,
        (String) null,
        null,
        Context.empty()
    );
    DaprHttp.Response response = mono.block();

    verify(httpClient).sendAsync(requestCaptor.capture(), any());

    HttpRequest request = requestCaptor.getValue();

    assertEquals("HEAD", request.method());
    assertEquals("http://" + sidecarIp + ":3500/v1.0/state", request.uri().toString());
    assertEquals(HTTP_OK, response.getStatusCode());
  }

  @Test
  public void invokeGetMethod() throws IOException {
    byte[] content = serializer.serialize(EXPECTED_RESULT);
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "GET",
        "v1.0/state".split("/"),
        null,
        null,
        Context.empty()
    );
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);

    verify(httpClient).sendAsync(requestCaptor.capture(), any());

    HttpRequest request = requestCaptor.getValue();

    assertEquals(EXPECTED_RESULT, body);
    assertEquals("GET", request.method());
    assertEquals("http://" + sidecarIp + ":3500/v1.0/state", request.uri().toString());
  }

  @Test
  public void invokeMethodWithHeaders() throws IOException {
    Map<String, String> headers = Map.of(
        "header", "value",
        "header1", "value1"
    );
    Map<String, List<String>> urlParameters = Map.of(
        "orderId", List.of("41")
    );
    byte[] content = serializer.serialize(EXPECTED_RESULT);
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "GET",
        "v1.0/state/order".split("/"),
        urlParameters,
        headers,
        Context.empty()
    );
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);

    verify(httpClient).sendAsync(requestCaptor.capture(), any());

    HttpRequest request = requestCaptor.getValue();

    assertEquals(EXPECTED_RESULT, body);
    assertEquals("GET", request.method());
    assertEquals("http://" + sidecarIp + ":3500/v1.0/state/order?orderId=41", request.uri().toString());
    assertEquals("value", request.headers().firstValue("header").get());
    assertEquals("value1", request.headers().firstValue("header1").get());
  }

  @Test
  public void invokePostMethodRuntime() {
    MockHttpResponse mockHttpResponse = new MockHttpResponse(HTTP_SERVER_ERROR);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "POST",
        "v1.0/state".split("/"),
        null,
        null,
        Context.empty());

    StepVerifier.create(mono).expectError(RuntimeException.class).verify();
  }

  @Test
  public void invokePostDaprError() {
    byte[] content = "{\"errorCode\":null,\"message\":null}".getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_SERVER_ERROR);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "POST",
        "v1.0/state".split("/"),
        null,
        null,
        Context.empty()
    );

    StepVerifier.create(mono).expectError(RuntimeException.class).verify();
  }

  @Test
  public void invokePostMethodUnknownError() {
    byte[] content = "{\"errorCode\":null,\"message\":null}".getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_SERVER_ERROR);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "POST",
        "v1.0/state".split("/"),
        null,
        null,
        Context.empty()
    );

    StepVerifier.create(mono).expectError(RuntimeException.class).verify();
  }

  @Test
  public void validateExceptionParsing() {
    String payload = "{" +
        "\"errorCode\":\"ERR_PUBSUB_NOT_FOUND\"," +
        "\"message\":\"pubsub abc is not found\"," +
        "\"details\":[" +
        "{" +
        "\"@type\":\"type.googleapis.com/google.rpc.ErrorInfo\"," +
        "\"domain\":\"dapr.io\"," +
        "\"metadata\":{}," +
        "\"reason\":\"DAPR_PUBSUB_NOT_FOUND\"" +
        "}]}";
    byte[] content = payload.getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_SERVER_ERROR);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi(
        "POST",
        "v1.0/pubsub/publish".split("/"),
        null,
        null,
        Context.empty()
    );

    StepVerifier.create(mono).expectErrorMatches(e -> {
      assertEquals(DaprException.class, e.getClass());

      DaprException daprException = (DaprException)e;
      assertEquals("ERR_PUBSUB_NOT_FOUND", daprException.getErrorCode());
      assertEquals("DAPR_PUBSUB_NOT_FOUND",
          daprException.getErrorDetails()
              .get(DaprErrorDetails.ErrorDetailType.ERROR_INFO, "reason", TypeRef.STRING));
      return true;
    }).verify();
  }

  /**
   * The purpose of this test is to show that it doesn't matter when the client is called, the actual call to DAPR
   * will be done when the output Mono response call the Mono.block method.
   * Like for instance if you call getState, without blocking for the response, and then call delete for the same state
   * you just retrieved but block for the delete response, when later you block for the response of the getState, you will
   * not find the state.
   * <p>This test will execute the following flow:</p>
   * <ol>
   *   <li>Execute client getState for Key=key1</li>
   *   <li>Block for result to the state</li>
   *   <li>Assert the Returned State is the expected to key1</li>
   *   <li>Execute client getState for Key=key2</li>
   *   <li>Execute client deleteState for Key=key2</li>
   *   <li>Block for deleteState call.</li>
   *   <li>Block for getState for Key=key2 and Assert they 2 was not found.</li>
   * </ol>
   *
   * @throws IOException - Test will fail if any unexpected exception is being thrown
   */
  @Test
  public void testCallbackCalledAtTheExpectedTimeTest() throws IOException {
    String existingState = "existingState";
    String urlExistingState = "v1.0/state/" + existingState;
    String deletedStateKey = "deletedKey";
    String urlDeleteState = "v1.0/state/" + deletedStateKey;

    when(httpClient.sendAsync(any(), any())).thenAnswer(invocation -> {
      HttpRequest request = invocation.getArgument(0);
      String url = request.uri().toString();

      if (request.method().equals("GET") && url.contains(urlExistingState)) {
        MockHttpResponse mockHttpResponse = new MockHttpResponse(serializer.serialize(existingState), HTTP_OK);

        return CompletableFuture.completedFuture(mockHttpResponse);
      }

      if (request.method().equals("DELETE")) {
        return CompletableFuture.completedFuture(new MockHttpResponse(HTTP_NO_CONTENT));
      }

      if (request.method().equals("GET")) {
        byte [] content = "{\"errorCode\":\"404\",\"message\":\"State Not Found\"}".getBytes();

        return CompletableFuture.completedFuture(new MockHttpResponse(content, HTTP_NOT_FOUND));
      }

      return CompletableFuture.failedFuture(new RuntimeException("Unexpected call"));
    });

    DaprHttp daprHttp = new DaprHttp(sidecarIp, 3500, daprTokenApi, READ_TIMEOUT, httpClient);
    Mono<DaprHttp.Response> response = daprHttp.invokeApi(
        "GET",
        urlExistingState.split("/"),
        null,
        null,
        Context.empty()
    );

    assertEquals(existingState, serializer.deserialize(response.block().getBody(), String.class));

    Mono<DaprHttp.Response> responseDeleted = daprHttp.invokeApi(
        "GET",
        urlDeleteState.split("/"),
        null,
        null,
        Context.empty()
    );
    Mono<DaprHttp.Response> responseDeleteKey = daprHttp.invokeApi(
        "DELETE",
        urlDeleteState.split("/"),
        null,
        null,
        Context.empty()
    );

    assertNull(serializer.deserialize(responseDeleteKey.block().getBody(), String.class));

    try {
      responseDeleted.block();
      fail("Expected DaprException");
    } catch (Exception ex) {
      assertEquals(DaprException.class, ex.getClass());
    }
  }
}
