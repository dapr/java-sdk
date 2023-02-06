/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.examples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Dapr's callback implementation via SpringBoot.
 */
@SpringBootApplication
public class DaprApplication {

  /**
   * Starts Dapr's callback in a given port and specified protocal.
   * 
   * @param port Port to listen to.
   * @param protocal select Http or gRPC to run.
   */
  public static void start(String protocal, int port) {
    SpringApplication app = new SpringApplication(DaprApplication.class);

    String args;
    if (protocal.equals("grpc")) {
      args = String.format("--grpc.server.port=%d", port);
    } else if (protocal.equals("http")) {
      args = String.format("--server.port=%d", port);
    } else {
      System.out.println("please select protocal in grpc or http.");
      return; 
    }

    app.run(args);
  }

  /**
   * Starts Dapr's callback in a given port. HTTP is used by default.
   * 
   * @param port Port to listen to.
   */
  public static void start(int port) {

    SpringApplication app = new SpringApplication(DaprApplication.class);

    app.run(String.format("--server.port=%d", port));
  }

}
