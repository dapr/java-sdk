/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.services.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Dapr's HTTP callback implementation via SpringBoot.
 */
@SpringBootApplication
public class DaprApplication {

  /**
   * Starts Dapr's callback in a given port.
   *
   * @param port Port to listen to.
   */
  public static void start(long port) {
    SpringApplication app = new SpringApplication(DaprApplication.class);
    app.run(String.format("--server.port=%d", port));
  }

}
