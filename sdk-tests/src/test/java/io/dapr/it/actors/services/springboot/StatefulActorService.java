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

package io.dapr.it.actors.services.springboot;

import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.it.DaprRunConfig;
import io.dapr.serializer.DefaultObjectSerializer;

import java.time.Duration;

@DaprRunConfig(enableDaprApiToken = false)
public class StatefulActorService {

  public static final String SUCCESS_MESSAGE = "dapr initialized. Status: Running";

  /**
   * Starts the service.
   *
   * @param args Expects the port as only argument.
   * @throws Exception If cannot start service.
   */
  public static void main(String[] args) throws Exception {

    // If port string is not valid, it will throw an exception.
    long port = Long.parseLong(args[0]);
    ActorRuntime.getInstance().getConfig().setActorIdleTimeout(Duration.ofSeconds(5));
    ActorRuntime.getInstance().getConfig().setActorScanInterval(Duration.ofSeconds(2));
    ActorRuntime.getInstance().getConfig().setDrainOngoingCallTimeout(Duration.ofSeconds(10));
    ActorRuntime.getInstance().getConfig().setDrainBalancedActors(true);
    ActorRuntime.getInstance().registerActor(
      StatefulActorImpl.class, new DefaultObjectSerializer(), new DefaultObjectSerializer());

    DaprApplication.start(port);
  }
}
