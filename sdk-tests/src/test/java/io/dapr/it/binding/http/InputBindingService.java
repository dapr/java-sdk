/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.binding.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InputBindingService {

  public static final String SUCCESS_MESSAGE = "Completed initialization in";

  public static void main(String[] args) throws Exception {
    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(args[0]);

    System.out.printf("Service starting on port %d ...\n", port);
    // Start Dapr's callback endpoint.
    start(port);
  }

  /**
   * Starts Dapr's callback in a given port.
   *
   * @param port Port to listen to.
   */
  private static void start(int port) {
    SpringApplication app = new SpringApplication(InputBindingService.class);
    app.run(String.format("--server.port=%d", port));
  }

}
