package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.ActorStateSerializer;
import io.dapr.client.DaprClientBuilder;
import okhttp3.OkHttpClient;

/**
 * Builder to generate an ActorProxy instance.
 */
public class ActorProxyBuilder {

    /**
     * Serializer for content to be sent back and forth between actors.
     */
    private static final ActorStateSerializer SERIALIZER = new ActorStateSerializer();

    /**
     * Builder for the Dapr client.
     */
    private final DaprClientBuilder clientBuilder = new DaprClientBuilder();

    /**
     * Actor's type.
     */
    private String actorType;

    /**
     * Actor's identifier.
     */
    private ActorId actorId;

    /**
     * Changes build config to use given Actor's type.
     *
     * @param actorType Actor's type.
     * @return Same builder object.
     */
    public ActorProxyBuilder withActorType(String actorType) {
        this.actorType = actorType;
        return this;
    }

    /**
     * Changes build config to use given Actor's identifier.
     *
     * @param actorId Actor's identifier.
     * @return Same builder object.
     */
    public ActorProxyBuilder withActorId(ActorId actorId) {
        this.actorId = actorId;
        return this;
    }

    /**
     * Instantiates a new ActorProxy.
     *
     * @return New instance of ActorProxy.
     */
    public ActorProxy build() {
        if ((this.actorType == null) || this.actorType.isEmpty()) {
            throw new IllegalArgumentException("Cannot instantiate an Actor without type.");
        }

        if (this.actorId == null) {
            throw new IllegalArgumentException("Cannot instantiate an Actor without Id.");
        }

        // TODO: Share client between actor proxy instances.
        return new ActorProxyImpl(
                this.actorType,
                this.actorId,
                SERIALIZER,
                this.clientBuilder.build());
    }

}
