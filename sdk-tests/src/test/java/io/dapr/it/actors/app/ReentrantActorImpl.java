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
 * limitations under the License.
*/

package io.dapr.it.actors.app;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.actors.runtime.AbstractActor;
import io.dapr.actors.runtime.ActorRuntimeContext;
import reactor.core.publisher.Mono;

/** Actor whose call method performs an A -&gt; B -&gt; A actor call chain. */
@ActorType(name = ReentrantActorImpl.TYPE)
public class ReentrantActorImpl extends AbstractActor implements ReentrantActor {

  public static final String TYPE = "ReentrantActorTest";

  private static final ActorClient ACTOR_CLIENT = new ActorClient();

  private static final ActorProxyBuilder<ReentrantActor> PROXY_BUILDER =
      new ActorProxyBuilder<>(TYPE, ReentrantActor.class, ACTOR_CLIENT);

  public ReentrantActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
    super(runtimeContext, id);
  }

  @Override
  public Mono<String> call(String targetActorId) {
    return proxy(targetActorId).returnTo(getId().toString());
  }

  @Override
  public Mono<String> returnTo(String actorId) {
    return proxy(actorId).finish();
  }

  @Override
  public Mono<String> finish() {
    return Mono.just("reentrancy-ok");
  }

  private static ReentrantActor proxy(String actorId) {
    return PROXY_BUILDER.build(new ActorId(actorId));
  }
}
