/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.testcontainers;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.Testcontainers;
import org.testcontainers.junit.jupiter.Container;

import java.util.Collections;

public interface DaprModule {

  @Container
  DaprContainer dapr = new DaprContainer("daprio/daprd:1.13.2")
      .withAppName("local-dapr-app")
      //Enable Workflows
      .withComponent(new Component("kvstore", "state.in-memory", "v1",
          Collections.singletonMap("actorStateStore", "true")))
      .withComponent(new Component("pubsub", "pubsub.in-memory", "v1", Collections.emptyMap()))
      .withAppPort(8080)
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withAppChannelAddress("host.testcontainers.internal");

  /**
   * Expose the Dapr ports to the host.
   *
   * @param registry the dynamic property registry
   */
  @DynamicPropertySource
  static void daprProperties(DynamicPropertyRegistry registry) {
    Testcontainers.exposeHostPorts(8080);
    dapr.start();
    System.setProperty("dapr.grpc.port", Integer.toString(dapr.getGrpcPort()));
    System.setProperty("dapr.http.port", Integer.toString(dapr.getHttpPort()));
  }

}
