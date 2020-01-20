package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.client.DefaultObjectSerializer;
import io.dapr.client.DaprHttpBuilder;
import io.dapr.client.DaprObjectSerializer;

/**
 * Builder to generate an ActorProxy instance. Builder can be reused for multiple instances.
 */
public class ActorProxyBuilder {

    /**
     * Default serializer for content to be sent back and forth between actors.
     */
    private static final DaprObjectSerializer DEFAULT_SERIALIZER = new DefaultObjectSerializer();

    /**
     * Builder for Dapr's raw http client.
     */
    private final DaprHttpBuilder daprHttpBuilder = new DaprHttpBuilder();

    /**
     * Actor's type.
     */
    private final String actorType;

    /**
     * Dapr's object serializer.
     */
    private final DaprObjectSerializer serializer;

    /**
     * Instantiates a new builder for a given Actor type.
     *
     * @param actorType Actor's type.
     * @param serializer Serializer for objects sent/received. Use null for default (not recommended).
     */
    public ActorProxyBuilder(String actorType, DaprObjectSerializer serializer) {
        this.actorType = actorType;
        this.serializer = serializer == null ? DEFAULT_SERIALIZER : serializer;
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
                this.serializer,
                new DaprHttpClient(this.daprHttpBuilder.build()));
    }

}
