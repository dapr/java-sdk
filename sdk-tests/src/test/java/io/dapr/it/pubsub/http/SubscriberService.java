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

package io.dapr.it.pubsub.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Service for subscriber.
 */
@SpringBootApplication
public class SubscriberService {

  public static final String SUCCESS_MESSAGE = "Completed initialization in";

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