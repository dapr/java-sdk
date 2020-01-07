/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Properties;

/**
 * Dapr's callback implementation via SpringBoot.
 */
@SpringBootApplication
public class DaprApplication {

  /**
   * Starts Dapr's callback in a given port.
   * @param port Port to listen to.
   */
  public static void start(int port) {
    SpringApplication app = new SpringApplication(DaprApplication.class);
    Properties properties = new Properties();
    properties.setProperty("server.port", Integer.toString(port));
    app.setDefaultProperties(properties);
    app.run();
  }

  /**
   * Main for SpringBoot requirements.
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    DaprApplication.start(3000);
  }

}
