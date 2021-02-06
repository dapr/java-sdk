/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Dapr's HTTP callback implementation via SpringBoot.
 */
@SpringBootApplication
public class TestApplication {

  /**
   * Starts Dapr's callback in a given port.
   * @param port Port to listen to.
   */
  public static void start(long port) {
    SpringApplication app = new SpringApplication(TestApplication.class);
    app.run(String.format("--server.port=%d", port));
  }

}
