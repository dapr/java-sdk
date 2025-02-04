package io.dapr.springboot.examples.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class TestConsumerApplication {

  public static void main(String[] args) {
    org.testcontainers.Testcontainers.exposeHostPorts(8081);
    SpringApplication
            .from(ConsumerApplication::main)
            .with(DaprTestContainersConfig.class)
            .run(args);
  }


}
