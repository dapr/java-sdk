package io.dapr.spring.boot.autoconfigure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = DaprClientProperties.CONFIG_PREFIX)
public class DaprClientProperties {

  public static final String CONFIG_PREFIX = "dapr.client";

  private int grpcPort;
  private int httpPort;


  public int getGrpcPort() {
    return grpcPort;
  }

  public void setGrpcPort(int grpcPort) {
    this.grpcPort = grpcPort;
  }

  public int getHttpPort() {
    return httpPort;
  }

  public void setHttpPort(int httpPort) {
    this.httpPort = httpPort;
  }
}
