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

package io.dapr.it.actors.app;

import io.dapr.actors.runtime.ActorEventSubscription;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.actors.runtime.ActorReentrancyConfig;
import io.dapr.actors.runtime.ActorTypeConfig;

import java.time.Duration;

/**
 * Hosts the test actors over the Dapr actor event stream, without listening on any port.
 */
public class MyActorGrpcService {

  /**
   * Starts the service.
   * @param args Unused.
   * @throws Exception If the stream stops on an error.
   */
  public static void main(String[] args) throws Exception {
    System.out.println("Hello from main() MyActorGrpcService");

    ActorRuntime.getInstance().getConfig().setActorIdleTimeout(Duration.ofSeconds(5));
    ActorRuntime.getInstance().registerActor(MyActorStringImpl.class);
    ActorRuntime.getInstance().registerActor(MyActorBinaryImpl.class);
    ActorRuntime.getInstance().registerActor(MyActorObjectImpl.class);
    ActorRuntime.getInstance().getConfig().addActorTypeConfig(new ActorTypeConfig()
        .setActorTypeName(ReentrantActorImpl.TYPE)
        .setReentrancy(new ActorReentrancyConfig().setEnabled(true).setMaxStackDepth(5)));
    ActorRuntime.getInstance().registerActor(ReentrantActorImpl.class);

    try (ActorEventSubscription subscription = ActorRuntime.getInstance().subscribeActorEvents()) {
      subscription.awaitTermination();
    }
  }
}
