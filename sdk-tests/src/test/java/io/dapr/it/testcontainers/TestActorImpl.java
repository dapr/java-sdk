package io.dapr.it.testcontainers;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.AbstractActor;
import io.dapr.actors.runtime.ActorRuntimeContext;

public class TestActorImpl extends AbstractActor implements TestActor {
  public TestActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
    super(runtimeContext, id);
  }

  @Override
  public String echo(String message) {
    return message;
  }
}
