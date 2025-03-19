package io.dapr.testcontainers;

/**
 * Configuration settings for Otel tracing.
 */
public class OtelTracingConfigurationSettings implements ConfigurationSettings {
  private final String endpointAddress;
  private final Boolean isSecure;
  private final String protocol;

  /**
   * Creates a new configuration.
   * @param endpointAddress tracing endpoint address
   * @param isSecure if the endpoint is secure
   * @param protocol tracing protocol
   */
  public OtelTracingConfigurationSettings(String endpointAddress, Boolean isSecure, String protocol) {
    this.endpointAddress = endpointAddress;
    this.isSecure = isSecure;
    this.protocol = protocol;
  }

  public String getEndpointAddress() {
    return endpointAddress;
  }

  public Boolean getSecure() {
    return isSecure;
  }

  public String getProtocol() {
    return protocol;
  }
}
