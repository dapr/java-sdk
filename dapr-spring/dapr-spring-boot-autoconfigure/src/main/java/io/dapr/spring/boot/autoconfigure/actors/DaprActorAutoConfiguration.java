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
import io.dapr.spring.boot.autoconfigure.client.DaprClientAutoConfiguration;
import io.dapr.spring.boot.properties.actors.DaprActorProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Dapr actor hosting.
 */
@AutoConfiguration(after = DaprClientAutoConfiguration.class)
@ConditionalOnClass(ActorRuntime.class)
@EnableConfigurationProperties(DaprActorProperties.class)
public class DaprActorAutoConfiguration {

  /**
   * Starts the gRPC actor event stream after all application runners have registered their actor types.
   *
   * @param actorRuntime Dapr actor runtime.
   * @param applicationContext Spring application context.
   * @return The lifecycle that owns the event subscription.
   */
  @Bean
  @ConditionalOnProperty(prefix = "dapr.actors", name = "transport", havingValue = "grpc")
  @ConditionalOnMissingBean(ActorEventSubscription.class)
  DaprActorEventSubscriptionLifecycle daprActorEventSubscriptionLifecycle(
      ActorRuntime actorRuntime, ConfigurableApplicationContext applicationContext) {
    return new DaprActorEventSubscriptionLifecycle(actorRuntime, applicationContext);
  }
}
