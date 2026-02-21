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

import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.it.actors.app.MyActorBinaryImpl;
import io.dapr.it.actors.app.MyActorObjectImpl;
import io.dapr.it.actors.app.MyActorStringImpl;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyActorRuntimeRegistrationConfiguration {

  @Bean
  public MyActorRuntimeRegistrar myActorRuntimeRegistrar(ActorRuntime actorRuntime) {
    return new MyActorRuntimeRegistrar(actorRuntime);
  }

  static final class MyActorRuntimeRegistrar {
    private final ActorRuntime actorRuntime;

    private MyActorRuntimeRegistrar(ActorRuntime actorRuntime) {
      this.actorRuntime = actorRuntime;
    }

    @PostConstruct
    void registerActors() {
      actorRuntime.registerActor(MyActorStringImpl.class);
      actorRuntime.registerActor(MyActorBinaryImpl.class);
      actorRuntime.registerActor(MyActorObjectImpl.class);
    }
  }
}
