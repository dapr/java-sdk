package io.dapr.spring.openfeign.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(DaprFeignClientProperties.PROPERTY_PREFIX)
public class DaprFeignClientProperties {

  public static final String PROPERTY_PREFIX = "dapr.feign";

  private boolean enabled;
  private Integer timeout;
  private Integer retries;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public Integer getRetries() {
    return retries;
  }

  public void setRetries(Integer retries) {
    this.retries = retries;
  }
}
