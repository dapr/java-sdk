package io.dapr.spring.boot.autoconfigure.client;

class PropertiesDaprConnectionDetails implements DaprConnectionDetails {

  private final DaprClientProperties daprClientProperties;

  public PropertiesDaprConnectionDetails(DaprClientProperties daprClientProperties) {
    this.daprClientProperties = daprClientProperties;
  }

  @Override
  public String httpEndpoint() {
    return this.daprClientProperties.httpEndpoint();
  }

  @Override
  public String grpcEndpoint() {
    return this.daprClientProperties.grpcEndpoint();
  }

  @Override
  public Integer httpPort() {
    return this.daprClientProperties.httpPort();
  }

  @Override
  public Integer grcpPort() {
    return this.daprClientProperties.grpcPort();
  }
}
