package io.dapr.spring.boot4.autoconfigure.client;

import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDaprWorkflows
public class WorkflowTestApplication {
  public static void main(String[] args) {
    SpringApplication.run(WorkflowTestApplication.class, args);
  }

  @Configuration
  static class Config {
    @Bean
    RestTemplate restTemplate(){
      return new RestTemplate();
    }
  }
}
