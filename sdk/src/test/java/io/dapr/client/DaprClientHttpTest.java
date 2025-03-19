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

import com.fasterxml.jackson.core.JsonParseException;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprGrpc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static io.dapr.utils.TestUtils.findFreePort;
import static io.dapr.utils.TestUtils.formatIpAddress;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DaprClientHttpTest {

  private final String EXPECTED_RESULT =
      "{\"data\":\"ewoJCSJwcm9wZXJ0eUEiOiAidmFsdWVBIiwKCQkicHJvcGVydHlCIjogInZhbHVlQiIKCX0=\"}";

  private static final int HTTP_NO_CONTENT = 204;
  private static final int HTTP_NOT_FOUND = 404;
  private static final int HTTP_SERVER_ERROR = 500;
  private static final int HTTP_OK = 200;
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
  
  private String sidecarIp;

  private String daprApiToken;

  private DaprClient daprClientHttp;

  private DaprHttp daprHttp;

  private HttpClient httpClient;

  @BeforeEach
  public void setUp() {
    sidecarIp = formatIpAddress(Properties.SIDECAR_IP.get());
    daprApiToken = Properties.API_TOKEN.get();
    httpClient = mock(HttpClient.class);
    daprHttp = new DaprHttp(sidecarIp, 3000, daprApiToken, READ_TIMEOUT, httpClient);
    daprClientHttp = buildDaprClient(daprHttp);
  }

  private static DaprClient buildDaprClient(DaprHttp daprHttp) {
    GrpcChannelFacade channel = mock(GrpcChannelFacade.class);
    DaprGrpc.DaprStub daprStub = mock(DaprGrpc.DaprStub.class);
    when(daprStub.withInterceptors(any())).thenReturn(daprStub);
    try {
      doNothing().when(channel).close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new DaprClientImpl(channel, daprStub, daprHttp, new DefaultObjectSerializer(), new DefaultObjectSerializer());
  }

  @Test
  public void waitForSidecarTimeOutHealthCheck() throws Exception {
    MockHttpResponse mockHttpResponse = new MockHttpResponse(HTTP_NO_CONTENT);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, daprApiToken, READ_TIMEOUT, httpClient);
    DaprClient daprClientHttp = buildDaprClient(daprHttp);

    when(httpClient.sendAsync(any(), any())).thenAnswer(invocation -> {
      Thread.sleep(200);

      return mockResponse;
    });

    StepVerifier.create(daprClientHttp.waitForSidecar(100))
            .expectSubscription()
            .expectErrorMatches(throwable -> {
              if (throwable instanceof TimeoutException) {
                System.out.println("TimeoutException occurred on sidecar health check.");
                return true;
              }
              return false;
            })
            .verify(Duration.ofSeconds(20));
  }

  @Test
  public void waitForSidecarBadHealthCheck() throws Exception {
    MockHttpResponse mockHttpResponse = new MockHttpResponse(HTTP_NOT_FOUND);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    int port = findFreePort();
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), port, daprApiToken, READ_TIMEOUT, httpClient);
    DaprClient daprClientHttp = buildDaprClient(daprHttp);
    AtomicInteger count = new AtomicInteger(0);

    when(httpClient.sendAsync(any(), any())).thenAnswer(invocation -> {
      if (count.getAndIncrement() < 6) {
        return mockResponse;
      }

      return CompletableFuture.failedFuture(new TimeoutException());
    });

    // it will timeout.
    StepVerifier.create(daprClientHttp.waitForSidecar(5000))
            .expectSubscription()
            .expectError()
            .verify(Duration.ofMillis(6000));
  }

  @Test
  public void waitForSidecarSlowSuccessfulHealthCheck() throws Exception {
    int port = findFreePort();
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), port, daprApiToken, READ_TIMEOUT, httpClient);
    DaprClient daprClientHttp = buildDaprClient(daprHttp);
    AtomicInteger count = new AtomicInteger(0);

    when(httpClient.sendAsync(any(), any())).thenAnswer(invocation -> {
      if (count.getAndIncrement() < 2) {
        Thread.sleep(1000);

        MockHttpResponse mockHttpResponse = new MockHttpResponse(HTTP_SERVER_ERROR);
        return CompletableFuture.<HttpResponse<Object>>completedFuture(mockHttpResponse);
      }

      Thread.sleep(1000);

      MockHttpResponse mockHttpResponse = new MockHttpResponse(HTTP_NO_CONTENT);
      return CompletableFuture.<HttpResponse<Object>>completedFuture(mockHttpResponse);
    });

    // Simulate a slow response
    StepVerifier.create(daprClientHttp.waitForSidecar(5000))
            .expectSubscription()
            .expectNext()
            .expectComplete()
            .verify(Duration.ofSeconds(20));
  }

  @Test
  public void waitForSidecarOK() throws Exception {
    MockHttpResponse mockHttpResponse = new MockHttpResponse(HTTP_NO_CONTENT);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    int port = findFreePort();
    daprHttp = new DaprHttp(sidecarIp, port, daprApiToken, READ_TIMEOUT, httpClient);
    DaprClient daprClientHttp = buildDaprClient(daprHttp);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    StepVerifier.create(daprClientHttp.waitForSidecar(10000))
            .expectSubscription()
            .expectComplete()
            .verify();
  }

  @Test
  public void waitForSidecarTimeoutOK() throws Exception {
    MockHttpResponse mockHttpResponse = new MockHttpResponse(HTTP_NO_CONTENT);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    try (ServerSocket serverSocket = new ServerSocket(0)) {
      int port = serverSocket.getLocalPort();

      Thread t = new Thread(() -> {
        try {
            try (Socket socket = serverSocket.accept()) {
            }
        } catch (IOException e) {
        }
      });
      t.start();

      daprHttp = new DaprHttp(sidecarIp, port, daprApiToken, READ_TIMEOUT, httpClient);
      DaprClient daprClientHttp = buildDaprClient(daprHttp);
      daprClientHttp.waitForSidecar(10000).block();
    }
  }

  @Test
  public void invokeServiceVerbNull() {
    MockHttpResponse mockHttpResponse = new MockHttpResponse(EXPECTED_RESULT.getBytes(), HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    assertThrows(IllegalArgumentException.class, () ->
        daprClientHttp.invokeMethod(
            null,
            "",
            "",
            null,
            null,
            (Class)null
        ).block());
  }

  @Test
  public void invokeServiceIllegalArgumentException() {
    byte[] content = "INVALID JSON".getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    assertThrows(IllegalArgumentException.class, () -> {
      // null HttpMethod
      daprClientHttp.invokeMethod(
          "1",
          "2",
          "3",
          new HttpExtension(null),
          null,
          (Class)null
      ).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null HttpExtension
      daprClientHttp.invokeMethod(
          "1",
          "2",
          "3",
          null,
          null,
          (Class)null
      ).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty appId
      daprClientHttp.invokeMethod(
          "",
          "1",
          null,
          HttpExtension.GET,
          null,
          (Class)null
      ).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null appId, empty method
      daprClientHttp.invokeMethod(
          null,
          "",
          null,
          HttpExtension.POST,
          null,
          (Class)null
      ).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty method
      daprClientHttp.invokeMethod(
          "1",
          "",
          null,
          HttpExtension.PUT,
          null,
          (Class)null
      ).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null method
      daprClientHttp.invokeMethod(
          "1",
          null,
          null,
          HttpExtension.DELETE,
          null,
          (Class)null
      ).block();
    });
    assertThrowsDaprException(JsonParseException.class, () -> {
      // invalid JSON response
      daprClientHttp.invokeMethod(
          "41",
          "badorder",
          null,
          HttpExtension.GET,
          null,
          String.class
      ).block();
    });
  }

  @Test
  public void invokeServiceDaprError() {
    byte[] content = "{ \"errorCode\": \"MYCODE\", \"message\": \"My Message\"}".getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_SERVER_ERROR);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprException exception = assertThrows(DaprException.class, () -> {
      daprClientHttp.invokeMethod(
          "myapp",
          "mymethod",
          "anything",
          HttpExtension.POST
      ).block();
    });

    assertEquals("MYCODE", exception.getErrorCode());
    assertEquals("MYCODE: My Message (HTTP status code: 500)", exception.getMessage());
    assertEquals(HTTP_SERVER_ERROR, exception.getHttpStatusCode());
  }

  @Test
  public void invokeServiceDaprErrorFromGRPC() {
    byte[] content = "{ \"code\": 7 }".getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_SERVER_ERROR);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprException exception = assertThrows(DaprException.class, () -> {
      daprClientHttp.invokeMethod(
          "myapp",
          "mymethod",
          "anything",
          HttpExtension.POST
      ).block();
    });

    assertEquals("PERMISSION_DENIED", exception.getErrorCode());
    assertEquals("PERMISSION_DENIED: HTTP status code: 500", exception.getMessage());
  }

  @Test
  public void invokeServiceDaprErrorUnknownJSON() {
    byte[] content = "{ \"anything\": 7 }".getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_SERVER_ERROR);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprException exception = assertThrows(DaprException.class, () -> {
      daprClientHttp.invokeMethod("myapp", "mymethod", "anything", HttpExtension.POST).block();
    });

    assertEquals("UNKNOWN", exception.getErrorCode());
    assertEquals("UNKNOWN: HTTP status code: 500", exception.getMessage());
    assertEquals("{ \"anything\": 7 }", new String(exception.getPayload()));
  }

  @Test
  public void invokeServiceDaprErrorEmptyString() {
    byte[] content = "".getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_SERVER_ERROR);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    DaprException exception = assertThrows(DaprException.class, () -> {
      daprClientHttp.invokeMethod(
          "myapp",
          "mymethod",
          "anything",
          HttpExtension.POST
      ).block();
    });

    assertEquals("UNKNOWN", exception.getErrorCode());
    assertEquals("UNKNOWN: HTTP status code: 500", exception.getMessage());
  }

  @Test
  public void invokeServiceMethodNull() {
    byte[] content = EXPECTED_RESULT.getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    assertThrows(IllegalArgumentException.class, () ->
        daprClientHttp.invokeMethod(
            "1",
            "",
            null,
            HttpExtension.POST,
            null,
            (Class)null
        ).block());
  }

  @Test
  public void invokeService() {
    byte[] content = "\"hello world\"".getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    Mono<String> mono = daprClientHttp.invokeMethod(
       "41",
       "neworder",
       null,
       HttpExtension.GET,
       null,
       String.class
   );

    assertEquals("hello world", mono.block());
  }

  @Test
  public void invokeServiceNullResponse() {
    byte[] content = new byte[0];
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    Mono<String> mono = daprClientHttp.invokeMethod(
        "41",
        "neworder",
        null,
        HttpExtension.GET,
        null,
        String.class
    );

    assertNull(mono.block());
  }

  @Test
  public void simpleInvokeService() {
    byte[] content = EXPECTED_RESULT.getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    Mono<byte[]> mono = daprClientHttp.invokeMethod(
        "41",
        "neworder",
        null,
        HttpExtension.GET,
        byte[].class
    );

    assertEquals(new String(mono.block()), EXPECTED_RESULT);
  }

  @Test
  public void invokeServiceWithMetadataMap() {
    Map<String, String> map = Map.of();
    byte[] content = EXPECTED_RESULT.getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    Mono<byte[]> mono = daprClientHttp.invokeMethod(
        "41",
        "neworder",
        (byte[]) null,
        HttpExtension.GET,
        map
    );
    String monoString = new String(mono.block());

    assertEquals(monoString, EXPECTED_RESULT);
  }

  @Test
  public void invokeServiceWithOutRequest() {
    Map<String, String> map = Map.of();
    byte[] content = EXPECTED_RESULT.getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    Mono<Void> mono = daprClientHttp.invokeMethod(
        "41",
        "neworder",
        HttpExtension.GET,
        map
    );

    assertNull(mono.block());
  }

  @Test
  public void invokeServiceWithRequest() {
    Map<String, String> map = Map.of();
    byte[] content = EXPECTED_RESULT.getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    Mono<Void> mono = daprClientHttp.invokeMethod(
        "41",
        "neworder",
        "",
        HttpExtension.GET,
        map
    );

    assertNull(mono.block());
  }

  @Test
  public void invokeServiceWithRequestAndQueryString() {
    Map<String, String> map = Map.of();
    Map<String, List<String>> queryString = Map.of(
        "param1", List.of("1"),
        "param2", List.of("a", "b/c")
    );
    byte[] content = EXPECTED_RESULT.getBytes();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    HttpExtension httpExtension = new HttpExtension(DaprHttp.HttpMethods.GET, queryString, null);
    Mono<Void> mono = daprClientHttp.invokeMethod(
        "41",
        "neworder",
        "",
        httpExtension,
        map
    );

    assertNull(mono.block());
  }

  @Test
  public void invokeServiceNoHotMono() {
    Map<String, String> map = Map.of();
    MockHttpResponse mockHttpResponse = new MockHttpResponse(HTTP_SERVER_ERROR);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    daprClientHttp.invokeMethod(
        "41",
        "neworder",
        "",
        HttpExtension.GET,
        map
    );
    // No exception should be thrown because did not call block() on mono above.
  }

  @Test
  public void invokeServiceWithContext() {
    String traceparent = "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01";
    String tracestate = "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7";
    Context context = Context.empty()
        .put("traceparent", traceparent)
        .put("tracestate", tracestate)
        .put("not_added", "xyz");
    byte[] content = new byte[0];
    MockHttpResponse mockHttpResponse = new MockHttpResponse(content, HTTP_OK);
    CompletableFuture<HttpResponse<Object>> mockResponse = CompletableFuture.completedFuture(mockHttpResponse);
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    when(httpClient.sendAsync(any(), any())).thenReturn(mockResponse);

    InvokeMethodRequest req = new InvokeMethodRequest("41", "neworder")
        .setBody("request")
        .setHttpExtension(HttpExtension.POST);
    Mono<Void> result = daprClientHttp.invokeMethod(req, TypeRef.get(Void.class))
        .contextWrite(it -> it.putAll((ContextView) context));

    result.block();

    verify(httpClient).sendAsync(requestCaptor.capture(), any());

    HttpRequest request = requestCaptor.getValue();

    assertEquals(traceparent, request.headers().firstValue("traceparent").get());
    assertEquals(tracestate, request.headers().firstValue("tracestate").get());
  }

  @Test
  public void closeException() {
    DaprHttp daprHttp = Mockito.mock(DaprHttp.class);
    Mockito.doThrow(new RuntimeException()).when(daprHttp).close();

    // This method does not throw DaprException because it already throws RuntimeException and does not call Dapr.
    daprClientHttp = buildDaprClient(daprHttp);
    assertThrows(RuntimeException.class, () -> daprClientHttp.close());
  }

  @Test
  public void close() throws Exception {
    DaprHttp daprHttp = Mockito.mock(DaprHttp.class);
    Mockito.doNothing().when(daprHttp).close();

    // This method does not throw DaprException because IOException is expected by the Closeable interface.
    daprClientHttp = buildDaprClient(daprHttp);
    daprClientHttp.close();
  }

}
