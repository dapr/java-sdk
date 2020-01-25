/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.binding.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"io.dapr.it.binding.http"})
public class InputBindingExample {

  public static void main(String[] args) throws Exception {
    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(args[0]);
    // Start Dapr's callback endpoint.
    InputBindingExample.start(port);
  }

  /**
   * Starts Dapr's callback in a given port.
   *
   * @param port Port to listen to.
   */
  public static void start(int port) {
    SpringApplication app = new SpringApplication(InputBindingExample.class);
    app.run(String.format("--server.port=%d", port));
  }

}
