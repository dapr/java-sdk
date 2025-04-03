package io.dapr.spring.openfeign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class DaprOpenFeignClientTestApplication {
  public static void main(String[] args) {
    SpringApplication.run(DaprOpenFeignClientTestApplication.class, args);
  }
}
