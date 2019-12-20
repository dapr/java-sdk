package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;

/**
 * Interface exposed to Actor's implementations (application layer).
 */
public interface ActorService<T extends AbstractActor> {

  /**
   * Creates an actor.
   * @param actorId Identifier for the Actor to be created.
   * @return New Actor instance.
   */
  T createActor(ActorId actorId);
}
