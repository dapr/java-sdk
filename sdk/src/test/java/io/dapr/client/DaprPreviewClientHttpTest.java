/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import okhttp3.OkHttpClient;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static io.dapr.utils.TestUtils.findFreePort;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DaprPreviewClientHttpTest {
  private static final String CONFIG_STORE_NAME = "MyConfigurationStore";

  private DaprPreviewClient daprPreviewClientHttp;

  private DaprPreviewClient daprPreviewClientHttpXML;

  private DaprHttp daprHttp;

  private OkHttpClient okHttpClient;

  private MockInterceptor mockInterceptor;

  @Before
  public void setUp() {
    mockInterceptor = new MockInterceptor(Behavior.UNORDERED);
    okHttpClient = new OkHttpClient.Builder().addInterceptor(mockInterceptor).build();
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprPreviewClientHttp = new DaprClientHttp(daprHttp);
    daprPreviewClientHttpXML = new DaprClientHttp(
        daprHttp,
        new DaprPreviewClientHttpTest.XmlSerializer(),
        new DaprPreviewClientHttpTest.XmlSerializer()
    );
  }

  @Test
  public void waitForSidecarTimeout() throws Exception {
    int port = findFreePort();
    System.setProperty(Properties.HTTP_PORT.getName(), Integer.toString(port));
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), port, okHttpClient);
    DaprClientHttp daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrows(RuntimeException.class, () -> daprClientHttp.waitForSidecar(1).block());
  }

  @Test
  public void waitForSidecarTimeoutOK() throws Exception {
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
      daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), port, okHttpClient);
      DaprClientHttp daprClientHttp = new DaprClientHttp(daprHttp);
      daprClientHttp.waitForSidecar(10000).block();
    }
  }

  @Test
  public void getConfigurationWithSingleKey() {
    assertThrows(DaprException.class, () -> {
      daprPreviewClientHttp.getConfiguration(CONFIG_STORE_NAME, "key").block();
    });
  }

  @Test
  public void getConfigurations() {
    assertThrows(DaprException.class, () -> {
      daprPreviewClientHttp.getConfigurations(CONFIG_STORE_NAME, "key1", "key2").block();
    });
  }

  @Test
  public void getAllConfigurations() {
    assertThrows(DaprException.class, () -> {
      daprPreviewClientHttp.getAllConfigurations(CONFIG_STORE_NAME).block();
    });
  }

  @Test
  public void subscribeConfigurations() {
    assertThrows(DaprException.class, () -> {
      daprPreviewClientHttp.subscribeToConfigurations(CONFIG_STORE_NAME, "key1", "key2");
    });
  }

  @Test
  public void closeException() {
    DaprHttp daprHttp = Mockito.mock(DaprHttp.class);
    Mockito.doThrow(new RuntimeException()).when(daprHttp).close();

    // This method does not throw DaprException because it already throws RuntimeException and does not call Dapr.
    daprPreviewClientHttp = new DaprClientHttp(daprHttp);
    assertThrows(RuntimeException.class, () -> daprPreviewClientHttp.close());
  }

  @Test
  public void close() throws Exception {
    DaprHttp daprHttp = Mockito.mock(DaprHttp.class);
    Mockito.doNothing().when(daprHttp).close();

    // This method does not throw DaprException because IOException is expected by the Closeable interface.
    daprPreviewClientHttp = new DaprClientHttp(daprHttp);
    daprPreviewClientHttp.close();
  }

  @Test
  public void shutdown() throws Exception {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/shutdown")
        .respond(204);

    final Mono<Void> mono = daprPreviewClientHttp.shutdown();
    assertNull(mono.block());
  }

  private static class XmlSerializer implements DaprObjectSerializer {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    @Override
    public byte[] serialize(Object o) throws IOException {
      return XML_MAPPER.writeValueAsBytes(o);
    }

    @Override
    public <T> T deserialize(byte[] data, TypeRef<T> type) throws IOException {
      return XML_MAPPER.readValue(data, new TypeReference<T>() {
      });
    }

    @Override
    public String getContentType() {
      return "application/xml";
    }
  }
}
