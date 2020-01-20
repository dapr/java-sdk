package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.ActorStateSerializer;
import io.dapr.client.DaprHttpBuilder;

/**
 * Builder to generate an ActorProxy instance. Builder can be reused for multiple instances.
 */
public class ActorProxyBuilder {

    /**
     * Serializer for content to be sent back and forth between actors.
     */
    private static final ActorStateSerializer SERIALIZER = new ActorStateSerializer();

    /**
     * Builder for Dapr's raw http client.
     */
    private final DaprHttpBuilder daprHttpBuilder = new DaprHttpBuilder();

    /**
     * Actor's type.
     */
    private final String actorType;

    /**
     * Instantiates a new builder for a given Actor type.
     *
     * @param actorType Actor's type.
     */
    public ActorProxyBuilder(String actorType) {
        this.actorType = actorType;
    }

    /**
     * Instantiates a new ActorProxy.
     *
     * @param actorId Actor's identifier.
     * @return New instance of ActorProxy.
     */
    public ActorProxy build(ActorId actorId) {
        if ((this.actorType == null) || this.actorType.isEmpty()) {
            throw new IllegalArgumentException("Cannot instantiate an Actor without type.");
        }

        if (actorId == null) {
            throw new IllegalArgumentException("Cannot instantiate an Actor without Id.");
        }

        return new ActorProxyImpl(
                this.actorType,
                actorId,
                SERIALIZER,
                new DaprHttpClient(this.daprHttpBuilder.build()));
    }

}
