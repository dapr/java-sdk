package io.dapr.testcontainers;

import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.spring.core.client.DaprClientCustomizer;

public class TestcontainersDaprClientCustomizer implements DaprClientCustomizer {

  private final String httpEndpoint;
  private final String grpcEndpoint;
  private final String daprHttpPort;
  private final String daprGrpcPort;

  /**
    * Constructor for TestcontainersDaprClientCustomizer.
    * @param httpEndpoint HTTP endpoint.
    * @param grpcEndpoint GRPC endpoint.
    * @param daprHttpPort Dapr HTTP port.
    * @param daprGrpcPort Dapr GRPC port.
    */
  public TestcontainersDaprClientCustomizer(
      String httpEndpoint,
      String grpcEndpoint,
      String daprHttpPort,
      String daprGrpcPort
  ) {
    this.httpEndpoint = httpEndpoint;
    this.grpcEndpoint = grpcEndpoint;
    this.daprHttpPort = daprHttpPort;
    this.daprGrpcPort = daprGrpcPort;
  }

  @Override
  public void customize(DaprClientBuilder daprClientBuilder) {
    daprClientBuilder.withPropertyOverride(Properties.HTTP_ENDPOINT, httpEndpoint);
    daprClientBuilder.withPropertyOverride(Properties.GRPC_ENDPOINT, grpcEndpoint);
    daprClientBuilder.withPropertyOverride(Properties.HTTP_PORT, daprHttpPort);
    daprClientBuilder.withPropertyOverride(Properties.GRPC_PORT, daprGrpcPort);
  }
}
