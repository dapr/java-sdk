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

package io.dapr.spring.boot.autoconfigure.actors;

import io.dapr.actors.runtime.ActorEventSubscription;
import io.dapr.actors.runtime.ActorRuntime;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Owns the gRPC actor event subscription for the lifetime of a Spring Boot application.
 */
final class DaprActorEventSubscriptionLifecycle
    implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

  static final String BEAN_NAME = "daprActorEventSubscription";

  private final ActorRuntime actorRuntime;

  private final ConfigurableApplicationContext applicationContext;

  private ActorEventSubscription subscription;

  DaprActorEventSubscriptionLifecycle(
      ActorRuntime actorRuntime, ConfigurableApplicationContext applicationContext) {
    this.actorRuntime = actorRuntime;
    this.applicationContext = applicationContext;
  }

  @Override
  public synchronized void onApplicationEvent(ApplicationReadyEvent event) {
    if (event.getApplicationContext() != applicationContext || subscription != null) {
      return;
    }

    ActorEventSubscription started = actorRuntime.subscribeActorEvents();
    try {
      // Register only after ApplicationReadyEvent so actor types registered by application runners are included.
      applicationContext.getBeanFactory().registerSingleton(BEAN_NAME, started);
      subscription = started;
    } catch (RuntimeException e) {
      started.close();
      throw e;
    }
  }

  @Override
  public synchronized void destroy() {
    if (subscription != null) {
      subscription.close();
      subscription = null;
    }
  }
}
