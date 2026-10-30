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
import io.dapr.actors.runtime.ActorEventSubscriptionTestFactory;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.actors.runtime.ActorRuntimeConfig;
import io.dapr.spring.boot.properties.actors.DaprActorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DaprActorAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(DaprActorAutoConfiguration.class));

  @Test
  void doesNotStartSubscriptionForDefaultHttpTransport() {
    ActorRuntime actorRuntime = mock(ActorRuntime.class);

    contextRunner.withBean(ActorRuntime.class, () -> actorRuntime).run(context -> {
      assertThat(context.getBean(DaprActorProperties.class).getTransport())
          .isEqualTo(DaprActorProperties.Transport.HTTP);
      assertThat(context).doesNotHaveBean(DaprActorEventSubscriptionLifecycle.class);
      assertThat(context).doesNotHaveBean(ActorEventSubscription.class);
    });

    verify(actorRuntime, never()).subscribeActorEvents();
  }

  @Test
  void startsAndExposesSubscriptionWhenApplicationIsReady() {
    ActorRuntime actorRuntime = mock(ActorRuntime.class);
    ActorRuntimeConfig actorRuntimeConfig = mock(ActorRuntimeConfig.class);
    when(actorRuntime.getConfig()).thenReturn(actorRuntimeConfig);
    ActorEventSubscription subscription = ActorEventSubscriptionTestFactory.newUnstartedSubscription(actorRuntime);
    when(actorRuntime.subscribeActorEvents()).thenReturn(subscription);

    contextRunner.withPropertyValues("dapr.actors.transport=grpc")
        .withBean(ActorRuntime.class, () -> actorRuntime)
        .run(context -> {
          assertThat(context).hasSingleBean(DaprActorEventSubscriptionLifecycle.class);
          assertThat(context).doesNotHaveBean(ActorEventSubscription.class);
          assertThat(context.getBean(DaprActorProperties.class).getTransport())
              .isEqualTo(DaprActorProperties.Transport.GRPC);

          ConfigurableApplicationContext applicationContext = context.getSourceApplicationContext();
          context.publishEvent(new ApplicationReadyEvent(
              new SpringApplication(), new String[0], applicationContext, Duration.ZERO));

          assertThat(context).hasSingleBean(ActorEventSubscription.class);
          assertThat(context.getBean(ActorEventSubscription.class)).isSameAs(subscription);
        });

    verify(actorRuntime).subscribeActorEvents();
    assertThat(subscription.isRunning()).isFalse();
  }
}
