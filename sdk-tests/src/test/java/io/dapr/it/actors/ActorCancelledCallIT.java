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

import io.dapr.it.AppRun;
import io.dapr.it.actors.app.MyActorService;
import org.junit.jupiter.api.BeforeAll;

/**
 * Cancelled actor calls on an app that hosts actors over HTTP.
 */
public class ActorCancelledCallIT extends AbstractActorCancelledCallIT {

  private static final String APP_NAME = "actor-cancelled-call-it";

  @BeforeAll
  public static void start() throws Exception {
    dapr = startAppAndAttach(
        APP_NAME,
        MyActorService.class,
        AppRun.AppProtocol.HTTP,
        appPort -> daprBuilder(APP_NAME)
            .withAppPort(appPort)
            .withAppChannelAddress("host.testcontainers.internal")
            .withComponent(redisStateStore(STATE_STORE_NAME))).dapr();
    waitForActorsReady(dapr);
  }
}
