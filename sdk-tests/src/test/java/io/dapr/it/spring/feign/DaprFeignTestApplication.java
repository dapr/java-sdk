package io.dapr.it.spring.feign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DaprFeignTestApplication {
  public static void main(String[] args) {
    SpringApplication.run(DaprFeignTestApplication.class, args);
  }
}
