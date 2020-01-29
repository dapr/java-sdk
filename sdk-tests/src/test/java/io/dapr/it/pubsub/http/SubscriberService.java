/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.pubsub.http;

import io.dapr.it.binding.http.InputBindingService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Service for subscriber.
 */
@SpringBootApplication(scanBasePackages = {"io.dapr.it.pubsub.http"})
public class SubscriberService {

  public static final String SUCCESS_MESSAGE = "dapr initialized. Status: Running. Init Elapsed";

  public static void main(String[] args) throws Exception {
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
    SpringApplication app = new SpringApplication(SubscriberService.class);
    app.run(String.format("--server.port=%d", port));
  }

}