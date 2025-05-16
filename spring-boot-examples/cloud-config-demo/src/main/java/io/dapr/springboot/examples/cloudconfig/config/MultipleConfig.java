package io.dapr.springboot.examples.cloudconfig.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MultipleConfig {

  //should be spring
  @Value("${dapr.spring.demo-config-secret.multivalue.v1}")
  private String multipleSecretConfigV1;
  //should be dapr
  @Value("${dapr.spring.demo-config-secret.multivalue.v2}")
  private String multipleSecretConfigV2;


  public String getMultipleSecretConfigV1() {
    return multipleSecretConfigV1;
  }

  public void setMultipleSecretConfigV1(String multipleSecretConfigV1) {
    this.multipleSecretConfigV1 = multipleSecretConfigV1;
  }

  public String getMultipleSecretConfigV2() {
    return multipleSecretConfigV2;
  }

  public void setMultipleSecretConfigV2(String multipleSecretConfigV2) {
    this.multipleSecretConfigV2 = multipleSecretConfigV2;
  }
}
