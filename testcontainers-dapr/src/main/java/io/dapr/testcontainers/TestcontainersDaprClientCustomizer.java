package io.dapr.testcontainers;

import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.spring.core.client.DaprClientCustomizer;

public class TestcontainersDaprClientCustomizer implements DaprClientCustomizer {

  private String httpEndpoint;
  private String grpcEndpoint;
  private Integer daprHttpPort;
  private Integer daprGrpcPort;

  /**
    * Constructor for TestcontainersDaprClientCustomizer.
    * @param httpEndpoint HTTP endpoint.
    * @param grpcEndpoint GRPC endpoint.
    */
  public TestcontainersDaprClientCustomizer(String httpEndpoint, String grpcEndpoint) {
    this.httpEndpoint = httpEndpoint;
    this.grpcEndpoint = grpcEndpoint;
  }

  /**
   * Constructor for TestcontainersDaprClientCustomizer.
   * @param daprHttpPort Dapr HTTP port.
   * @param daprGrpcPort Dapr GRPC port.
   */
  public TestcontainersDaprClientCustomizer(int daprHttpPort, int daprGrpcPort) {
    this.daprHttpPort = daprHttpPort;
    this.daprGrpcPort = daprGrpcPort;
  }

  @Override
  public void customize(DaprClientBuilder daprClientBuilder) {
    if (httpEndpoint != null) {
      daprClientBuilder.withPropertyOverride(Properties.HTTP_ENDPOINT, httpEndpoint);
    }

    if (grpcEndpoint != null) {
      daprClientBuilder.withPropertyOverride(Properties.GRPC_ENDPOINT, grpcEndpoint);
    }

    if (daprHttpPort != null) {
      daprClientBuilder.withPropertyOverride(Properties.HTTP_PORT, String.valueOf(daprHttpPort));
    }

    if (daprGrpcPort != null) {
      daprClientBuilder.withPropertyOverride(Properties.GRPC_PORT, String.valueOf(daprGrpcPort));
    }
  }
}
