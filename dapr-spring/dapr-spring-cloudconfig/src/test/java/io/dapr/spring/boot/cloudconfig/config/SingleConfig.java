package io.dapr.spring.boot.cloudconfig.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SingleConfig {

  // should be testvalue
  @Value("dapr.spring.demo-config-secret.singlevalue")
  private String singleValueSecret;

  // should be config
  @Value("dapr.spring.demo-config-secret.multivalue.v4")
  private String multiValuedSecret;

  // should be testvalue
  @Value("dapr.spring.demo-config-config.singlevalue")
  private String singleValueConfig;

  public String getSingleValueSecret() {
    return singleValueSecret;
  }

  public void setSingleValueSecret(String singleValueSecret) {
    this.singleValueSecret = singleValueSecret;
  }

  public String getMultiValuedSecret() {
    return multiValuedSecret;
  }

  public void setMultiValuedSecret(String multiValuedSecret) {
    this.multiValuedSecret = multiValuedSecret;
  }

  public String getSingleValueConfig() {
    return singleValueConfig;
  }

  public void setSingleValueConfig(String singleValueConfig) {
    this.singleValueConfig = singleValueConfig;
  }
}
