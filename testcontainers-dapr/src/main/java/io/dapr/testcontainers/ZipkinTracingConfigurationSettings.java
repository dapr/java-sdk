package io.dapr.testcontainers;

/**
 * Configuration settings for Zipkin tracing.
 */
public class ZipkinTracingConfigurationSettings implements ConfigurationSettings {
  private final String endpointAddress;

  public ZipkinTracingConfigurationSettings(String endpointAddress) {
    this.endpointAddress = endpointAddress;
  }

  public String getEndpointAddress() {
    return endpointAddress;
  }
}
