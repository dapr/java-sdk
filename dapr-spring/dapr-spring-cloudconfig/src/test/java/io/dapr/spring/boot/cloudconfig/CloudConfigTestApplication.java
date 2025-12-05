package io.dapr.spring.boot.cloudconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "io.dapr.spring.boot.cloudconfig.config")
public class CloudConfigTestApplication {
  public static void main(String[] args) {
    SpringApplication.run(CloudConfigTestApplication.class, args);
  }

}
