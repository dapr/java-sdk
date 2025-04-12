package io.dapr.examples.jobs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application to demonstrate Dapr Jobs callback API.
 * <p>
 * This application demonstrates how to use Dapr Jobs API with Spring Boot.
 * </p>
 */
@SpringBootApplication
public class DemoJobsSpringApplication {

  public static void main(String[] args) throws Exception {
    SpringApplication.run(DemoJobsSpringApplication.class, args);
  }
}
