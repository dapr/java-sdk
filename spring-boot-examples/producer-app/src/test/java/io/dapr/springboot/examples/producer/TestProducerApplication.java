package io.dapr.springboot.examples.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class TestProducerApplication {

  public static void main(String[] args) {

    SpringApplication.from(ProducerApplication::main)
            .with(DaprTestContainersConfig.class)
            .run(args);
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
  }

}
