/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.it.actors;

import io.dapr.it.actors.app.MyActorGrpcService;
import io.dapr.testcontainers.DaprProtocol;
import org.junit.jupiter.api.BeforeAll;

/**
 * Cancelled actor calls on an app that hosts actors over the Dapr actor event stream.
 */
public class ActorCancelledCallGrpcHostIT extends AbstractActorCancelledCallIT {

  private static final String APP_NAME = "actor-cancelled-call-grpc-host-it";

  @BeforeAll
  public static void start() throws Exception {
    dapr = startActorHostAndAttach(
        APP_NAME,
        MyActorGrpcService.class,
        () -> daprBuilder(APP_NAME)
            .withAppProtocol(DaprProtocol.GRPC)
            .withComponent(redisStateStore(STATE_STORE_NAME))).dapr();
    waitForActorsReady(dapr);
  }
}
