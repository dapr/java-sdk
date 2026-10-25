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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.Map;

/** Spring Boot actor host used to verify the gRPC actor transport auto-configuration. */
@SpringBootApplication
public class SpringBootGrpcActorService {

  public static void main(String[] args) throws Exception {
    SpringApplication application = new SpringApplication(SpringBootGrpcActorService.class);
    application.setDefaultProperties(Map.of(
        "dapr.actors.transport", "grpc",
        "spring.main.web-application-type", "none",
        "spring.main.banner-mode", "off"));

    try (ConfigurableApplicationContext context = application.run(args)) {
      context.getBean(ActorEventSubscription.class).awaitTermination();
    }
  }

  @Bean
  ApplicationRunner registerActors(ActorRuntime actorRuntime) {
    return new ApplicationRunner() {
      @Override
      public void run(ApplicationArguments args) {
        actorRuntime.getConfig().setActorIdleTimeout(Duration.ofSeconds(5));
        actorRuntime.registerActor(MyActorStringImpl.class);
      }
    };
  }
}
