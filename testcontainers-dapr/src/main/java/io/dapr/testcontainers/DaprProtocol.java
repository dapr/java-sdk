package io.dapr.testcontainers;

public enum DaprProtocol {
  HTTP("http"),
  GRPC("grpc");

  private String name;

  DaprProtocol(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
