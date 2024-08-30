package io.dapr.testcontainers;

import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TestcontainersDaprClientCustomizerTest {
  private static final int HTTP_PORT = 3500;
    private static final int GRPC_PORT = 50001;
  private static final String HTTP_ENDPOINT = "http://localhost:" + HTTP_PORT;
  private static final String GRPC_ENDPOINT = "localhost:" + GRPC_PORT;

  private DaprClientBuilder daprClientBuilder;

  @BeforeEach
  void setUp() {
    daprClientBuilder = mock(DaprClientBuilder.class);
  }

  @Test
  void testCustomizeWithEndpointOverrides() {
    TestcontainersDaprClientCustomizer customizer = new TestcontainersDaprClientCustomizer(HTTP_ENDPOINT, GRPC_ENDPOINT);
    customizer.customize(daprClientBuilder);

    verify(daprClientBuilder).withPropertyOverride(Properties.HTTP_ENDPOINT, HTTP_ENDPOINT);
    verify(daprClientBuilder).withPropertyOverride(Properties.GRPC_ENDPOINT, GRPC_ENDPOINT);
  }

  @Test
  void testCustomizeWithPortOverrides() {
    TestcontainersDaprClientCustomizer customizer = new TestcontainersDaprClientCustomizer(HTTP_PORT, GRPC_PORT);
    customizer.customize(daprClientBuilder);

    verify(daprClientBuilder).withPropertyOverride(Properties.HTTP_PORT, String.valueOf(HTTP_PORT));
    verify(daprClientBuilder).withPropertyOverride(Properties.GRPC_PORT, String.valueOf(GRPC_PORT));
  }
}
