package io.dapr.spring.boot.autoconfigure.client;

import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDaprWorkflows
public class WorkflowTestApplication {
  public static void main(String[] args) {
    SpringApplication.run(WorkflowTestApplication.class, args);

  }
}
