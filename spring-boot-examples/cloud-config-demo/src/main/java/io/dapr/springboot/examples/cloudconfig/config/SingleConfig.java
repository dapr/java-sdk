package io.dapr.springboot.examples.cloudconfig.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SingleConfig {

  // should be testvalue
  @Value("${dapr.spring.demo-config-secret.singlevalue}")
  private String singleValueSecret;

  public String getSingleValueSecret() {
    return singleValueSecret;
  }

  public void setSingleValueSecret(String singleValueSecret) {
    this.singleValueSecret = singleValueSecret;
  }
}
