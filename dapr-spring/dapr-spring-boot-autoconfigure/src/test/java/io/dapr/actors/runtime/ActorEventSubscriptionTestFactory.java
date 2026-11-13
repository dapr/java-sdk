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

package io.dapr.actors.runtime;

import java.time.Duration;

/** Test helper that creates an event subscription without opening a Dapr connection. */
public final class ActorEventSubscriptionTestFactory {

  private ActorEventSubscriptionTestFactory() {
  }

  /**
   * Creates a subscription that can be used to verify lifecycle management without starting its stream.
   *
   * @param actorRuntime Actor runtime used when the subscription is closed.
   * @return An unstarted event subscription.
   */
  public static ActorEventSubscription newUnstartedSubscription(ActorRuntime actorRuntime) {
    return new ActorEventSubscription(actorRuntime, null, null, Duration.ZERO);
  }
}
