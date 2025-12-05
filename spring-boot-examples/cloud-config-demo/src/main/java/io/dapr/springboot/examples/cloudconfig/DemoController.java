package io.dapr.springboot.examples.cloudconfig;

import io.dapr.springboot.examples.cloudconfig.config.SingleConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

  private final SingleConfig singleConfig;

  public DemoController(SingleConfig singleConfig) {
    this.singleConfig = singleConfig;
  }

  @GetMapping("/config")
  public String getConfig() {
    return singleConfig.getSingleValueSecret();
  }

}
