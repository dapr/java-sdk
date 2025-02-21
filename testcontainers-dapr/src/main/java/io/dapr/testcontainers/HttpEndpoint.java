package io.dapr.testcontainers;

public class HttpEndpoint {
  private String name;
  private String baseUrl;

  public HttpEndpoint(String name, String baseUrl) {
    this.name = name;
    this.baseUrl = baseUrl;
  }

  public String getName() {
    return name;
  }

  public String getBaseUrl() {
    return baseUrl;
  }
}
