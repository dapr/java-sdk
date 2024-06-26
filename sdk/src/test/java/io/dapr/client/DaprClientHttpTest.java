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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprGrpc;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.mock.Behavior;
import okhttp3.mock.MediaTypes;
import okhttp3.mock.MockInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static io.dapr.utils.TestUtils.findFreePort;
import static io.dapr.utils.TestUtils.formatIpAddress;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DaprClientHttpTest {

  private final String EXPECTED_RESULT =
      "{\"data\":\"ewoJCSJwcm9wZXJ0eUEiOiAidmFsdWVBIiwKCQkicHJvcGVydHlCIjogInZhbHVlQiIKCX0=\"}";
  
  private String sidecarIp;

  private DaprClient daprClientHttp;

  private DaprHttp daprHttp;

  private OkHttpClient okHttpClient;

  private MockInterceptor mockInterceptor;

  @BeforeEach
  public void setUp() {
    sidecarIp = formatIpAddress(Properties.SIDECAR_IP.get());
    mockInterceptor = new MockInterceptor(Behavior.UNORDERED);
    okHttpClient = new OkHttpClient.Builder().addInterceptor(mockInterceptor).build();
    daprHttp = new DaprHttp(sidecarIp, 3000, okHttpClient);
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
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    DaprClient daprClientHttp = buildDaprClient(daprHttp);

    mockInterceptor.addRule()
            .get()
            .path("/v1.0/healthz/outbound")
            .delay(200)
            .respond(204, ResponseBody.create("No Content", MediaType.get("application/json")));

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
    int port = findFreePort();
    System.setProperty(Properties.HTTP_PORT.getName(), Integer.toString(port));
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), port, okHttpClient);
    DaprClient daprClientHttp = buildDaprClient(daprHttp);

      mockInterceptor.addRule()
              .get()
              .path("/v1.0/healthz/outbound")
              .times(6)
              .respond(404, ResponseBody.create("Not Found", MediaType.get("application/json")));

    // retry the max allowed retries (5 times)
    StepVerifier.create(daprClientHttp.waitForSidecar(5000))
            .expectSubscription()
            .expectErrorMatches(throwable -> {
              if (throwable instanceof RuntimeException) {
                return "Retries exhausted: 5/5".equals(throwable.getMessage());
              }
              return false;
            })
            .verify(Duration.ofSeconds(20));
  }

  @Test
  public void waitForSidecarSlowSuccessfulHealthCheck() throws Exception {
    int port = findFreePort();
    System.setProperty(Properties.HTTP_PORT.getName(), Integer.toString(port));
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), port, okHttpClient);
    DaprClient daprClientHttp = buildDaprClient(daprHttp);

    // Simulate a slow response
    mockInterceptor.addRule()
            .get()
            .path("/v1.0/healthz/outbound")
            .delay(1000)
            .times(2)
            .respond(500, ResponseBody.create("Internal Server Error", MediaType.get("application/json")));

    mockInterceptor.addRule()
            .get()
            .path("/v1.0/healthz/outbound")
            .delay(1000)
            .times(1)
            .respond(204, ResponseBody.create("No Content", MediaType.get("application/json")));

    StepVerifier.create(daprClientHttp.waitForSidecar(5000))
            .expectSubscription()
            .expectNext()
            .expectComplete()
            .verify(Duration.ofSeconds(20));
  }

  @Test
  public void waitForSidecarOK() throws Exception {
    int port = findFreePort();
    System.setProperty(Properties.HTTP_PORT.getName(), Integer.toString(port));
    daprHttp = new DaprHttp(sidecarIp, port, okHttpClient);
    DaprClient daprClientHttp = buildDaprClient(daprHttp);

    mockInterceptor.addRule()
            .get()
            .path("/v1.0/healthz/outbound")
            .respond(204);

    StepVerifier.create(daprClientHttp.waitForSidecar(10000))
            .expectSubscription()
            .expectComplete()
            .verify();
  }

  @Test
  public void waitForSidecarTimeoutOK() throws Exception {
    mockInterceptor.addRule()
            .get()
            .path("/v1.0/healthz/outbound")
            .respond(204);
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      final int port = serverSocket.getLocalPort();
      System.setProperty(Properties.HTTP_PORT.getName(), Integer.toString(port));
      Thread t = new Thread(() -> {
        try {
            try (Socket socket = serverSocket.accept()) {
            }
        } catch (IOException e) {
        }
      });
      t.start();
      daprHttp = new DaprHttp(sidecarIp, port, okHttpClient);
      DaprClient daprClientHttp = buildDaprClient(daprHttp);
      daprClientHttp.waitForSidecar(10000).block();
    }
  }

  @Test
  public void invokeServiceVerbNull() {
    mockInterceptor.addRule()
          .post("http://" + sidecarIp + ":3000/v1.0/publish/A")
          .respond(EXPECTED_RESULT);
    String event = "{ \"message\": \"This is a test\" }";

    assertThrows(IllegalArgumentException.class, () ->
        daprClientHttp.invokeMethod(null, "", "", null, null, (Class)null).block());
  }

  @Test
  public void invokeServiceIllegalArgumentException() {
    mockInterceptor.addRule()
        .get("http://" + sidecarIp + ":3000/v1.0/invoke/41/method/badorder")
        .respond("INVALID JSON");

    assertThrows(IllegalArgumentException.class, () -> {
      // null HttpMethod
      daprClientHttp.invokeMethod("1", "2", "3", new HttpExtension(null), null, (Class)null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null HttpExtension
      daprClientHttp.invokeMethod("1", "2", "3", null, null, (Class)null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty appId
      daprClientHttp.invokeMethod("", "1", null, HttpExtension.GET, null, (Class)null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null appId, empty method
      daprClientHttp.invokeMethod(null, "", null, HttpExtension.POST, null, (Class)null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty method
      daprClientHttp.invokeMethod("1", "", null, HttpExtension.PUT, null, (Class)null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null method
      daprClientHttp.invokeMethod("1", null, null, HttpExtension.DELETE, null, (Class)null).block();
    });
    assertThrowsDaprException(JsonParseException.class, () -> {
      // invalid JSON response
      daprClientHttp.invokeMethod("41", "badorder", null, HttpExtension.GET, null, String.class).block();
    });
  }

  @Test
  public void invokeServiceDaprError() {
    mockInterceptor.addRule()
        .post("http://" + sidecarIp + ":3000/v1.0/invoke/myapp/method/mymethod")
        .respond(500,
            ResponseBody.create(
                "{ \"errorCode\": \"MYCODE\", \"message\": \"My Message\"}",
                MediaTypes.MEDIATYPE_JSON));

    DaprException exception = assertThrows(DaprException.class, () -> {
      daprClientHttp.invokeMethod("myapp", "mymethod", "anything", HttpExtension.POST).block();
    });

    assertEquals("MYCODE", exception.getErrorCode());
    assertEquals("MYCODE: My Message (HTTP status code: 500)", exception.getMessage());
    assertEquals(500, exception.getHttpStatusCode());
  }

  @Test
  public void invokeServiceDaprErrorFromGRPC() {
    mockInterceptor.addRule()
        .post("http://" + sidecarIp + ":3000/v1.0/invoke/myapp/method/mymethod")
        .respond(500,
            ResponseBody.create(
                "{ \"code\": 7 }",
                MediaTypes.MEDIATYPE_JSON));

    DaprException exception = assertThrows(DaprException.class, () -> {
      daprClientHttp.invokeMethod("myapp", "mymethod", "anything", HttpExtension.POST).block();
    });

    assertEquals("PERMISSION_DENIED", exception.getErrorCode());
    assertEquals("PERMISSION_DENIED: HTTP status code: 500", exception.getMessage());
  }

  @Test
  public void invokeServiceDaprErrorUnknownJSON() {
    mockInterceptor.addRule()
        .post("http://" + sidecarIp + ":3000/v1.0/invoke/myapp/method/mymethod")
        .respond(500,
            ResponseBody.create(
                "{ \"anything\": 7 }",
                MediaTypes.MEDIATYPE_JSON));

    DaprException exception = assertThrows(DaprException.class, () -> {
      daprClientHttp.invokeMethod("myapp", "mymethod", "anything", HttpExtension.POST).block();
    });

    assertEquals("UNKNOWN", exception.getErrorCode());
    assertEquals("UNKNOWN: HTTP status code: 500", exception.getMessage());
    assertEquals("{ \"anything\": 7 }", new String(exception.getPayload()));
  }

  @Test
  public void invokeServiceDaprErrorEmptyString() {
    mockInterceptor.addRule()
        .post("http://" + sidecarIp + ":3000/v1.0/invoke/myapp/method/mymethod")
        .respond(500,
            ResponseBody.create(
                "",
                MediaTypes.MEDIATYPE_JSON));

    DaprException exception = assertThrows(DaprException.class, () -> {
      daprClientHttp.invokeMethod("myapp", "mymethod", "anything", HttpExtension.POST).block();
    });

    assertEquals("UNKNOWN", exception.getErrorCode());
    assertEquals("UNKNOWN: HTTP status code: 500", exception.getMessage());
  }


  @Test
  public void invokeServiceMethodNull() {
    mockInterceptor.addRule()
          .post("http://" + sidecarIp + ":3000/v1.0/publish/A")
          .respond(EXPECTED_RESULT);

    assertThrows(IllegalArgumentException.class, () ->
        daprClientHttp.invokeMethod("1", "", null, HttpExtension.POST, null, (Class)null).block());
  }

  @Test
  public void invokeService() {
    mockInterceptor.addRule()
        .get("http://" + sidecarIp + ":3000/v1.0/invoke/41/method/neworder")
        .respond("\"hello world\"");

    Mono<String> mono = daprClientHttp.invokeMethod("41", "neworder", null, HttpExtension.GET, null, String.class);
    assertEquals("hello world", mono.block());
  }

  @Test
  public void invokeServiceNullResponse() {
    mockInterceptor.addRule()
        .get("http://" + sidecarIp + ":3000/v1.0/invoke/41/method/neworder")
        .respond(new byte[0]);

    Mono<String> mono = daprClientHttp.invokeMethod("41", "neworder", null, HttpExtension.GET, null, String.class);
    assertNull(mono.block());
  }

  @Test
  public void simpleInvokeService() {
    mockInterceptor.addRule()
          .get("http://" + sidecarIp + ":3000/v1.0/invoke/41/method/neworder")
          .respond(EXPECTED_RESULT);

    Mono<byte[]> mono = daprClientHttp.invokeMethod("41", "neworder", null, HttpExtension.GET, byte[].class);
    assertEquals(new String(mono.block()), EXPECTED_RESULT);
  }

  @Test
  public void invokeServiceWithMetadataMap() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
          .get("http://" + sidecarIp + ":3000/v1.0/invoke/41/method/neworder")
          .respond(EXPECTED_RESULT);

    Mono<byte[]> mono = daprClientHttp.invokeMethod("41", "neworder", (byte[]) null, HttpExtension.GET, map);
    String monoString = new String(mono.block());
    assertEquals(monoString, EXPECTED_RESULT);
  }

  @Test
  public void invokeServiceWithOutRequest() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
          .get("http://" + sidecarIp + ":3000/v1.0/invoke/41/method/neworder")
          .respond(EXPECTED_RESULT);

    Mono<Void> mono = daprClientHttp.invokeMethod("41", "neworder", HttpExtension.GET, map);
    assertNull(mono.block());
  }

  @Test
  public void invokeServiceWithRequest() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
          .get("http://" + sidecarIp + ":3000/v1.0/invoke/41/method/neworder")
          .respond(EXPECTED_RESULT);

    Mono<Void> mono = daprClientHttp.invokeMethod("41", "neworder", "", HttpExtension.GET, map);
    assertNull(mono.block());
  }

  @Test
  public void invokeServiceWithRequestAndQueryString() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
        .get("http://" + sidecarIp + ":3000/v1.0/invoke/41/method/neworder?param1=1&param2=a&param2=b%2Fc")
        .respond(EXPECTED_RESULT);

    Map<String, List<String>> queryString = new HashMap<>();
    queryString.put("param1", Collections.singletonList("1"));
    queryString.put("param2", Arrays.asList("a", "b/c"));
    HttpExtension httpExtension = new HttpExtension(DaprHttp.HttpMethods.GET, queryString, null);
    Mono<Void> mono = daprClientHttp.invokeMethod("41", "neworder", "", httpExtension, map);
    assertNull(mono.block());
  }

  @Test
  public void invokeServiceNoHotMono() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
        .get("http://" + sidecarIp + ":3000/v1.0/invoke/41/method/neworder")
        .respond(500);

    daprClientHttp.invokeMethod("41", "neworder", "", HttpExtension.GET, map);
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
    mockInterceptor.addRule()
        .post("http://" + sidecarIp + ":3000/v1.0/invoke/41/method/neworder")
        .header("traceparent", traceparent)
        .header("tracestate", tracestate)
        .respond(new byte[0]);

    InvokeMethodRequest req = new InvokeMethodRequest("41", "neworder")
        .setBody("request")
        .setHttpExtension(HttpExtension.POST);
    Mono<Void> result = daprClientHttp.invokeMethod(req, TypeRef.get(Void.class))
        .contextWrite(it -> it.putAll((ContextView) context));
    result.block();
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

  private static class XmlSerializer implements DaprObjectSerializer {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    @Override
    public byte[] serialize(Object o) throws IOException {
      return XML_MAPPER.writeValueAsBytes(o);
    }

    @Override
    public <T> T deserialize(byte[] data, TypeRef<T> type) throws IOException {
      return XML_MAPPER.readValue(data, new TypeReference<T>() {});
    }

    @Override
    public String getContentType() {
      return "application/xml";
    }
  }
}