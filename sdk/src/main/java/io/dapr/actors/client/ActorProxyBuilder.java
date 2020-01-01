package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.ActorStateSerializer;
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
    private final ActorProxyClientBuilder clientBuilder = new ActorProxyClientBuilder();

    /**
     * Actor's type.
     */
    private String actorType;

    /**
     * Actor's identifier.
     */
    private ActorId actorId;

    /**
     * Changes build config to use specific port.
     *
     * @param port Port to be used.
     * @return Same builder object.
     */
    public ActorProxyBuilder withPort(int port) {
        this.clientBuilder.withPort(port);
        return this;
    }

    public ActorProxyBuilder withHost(String host) {
        this.clientBuilder.withHost(host);
        return this;
    }

    public ActorProxyBuilder withThreadPoolSize(int threadPoolSize) {
        this.clientBuilder.withThreadPoolSize(threadPoolSize);
        return this;
    }

    public ActorProxyBuilder withOkHttpClientBuilder(OkHttpClient.Builder okHttpClientBuilder) {
        this.clientBuilder.withOkHttpClientBuilder(okHttpClientBuilder);
        return this;
    }

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
                this.clientBuilder.buildAsyncClient());
    }

}
