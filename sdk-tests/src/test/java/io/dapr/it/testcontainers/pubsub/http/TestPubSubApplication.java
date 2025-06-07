package io.dapr.it.testcontainers.pubsub.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestPubSubApplication {
  public static void main(String[] args) {
    SpringApplication.run(TestPubSubApplication.class, args);
  }
}
