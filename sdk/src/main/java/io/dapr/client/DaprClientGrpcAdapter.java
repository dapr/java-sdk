/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import io.dapr.DaprGrpc;
import io.dapr.DaprProtos;
import io.dapr.utils.ObjectSerializer;
import reactor.core.publisher.Mono;

/**
 * An adapter for the GRPC Client.
 *
 * @see io.dapr.DaprGrpc
 * @see io.dapr.client.DaprClient
 */
class DaprClientGrpcAdapter implements DaprClient {

    /**
     * The GRPC client to be used
     *
     * @see io.dapr.DaprGrpc.DaprFutureStub
     */
    private DaprGrpc.DaprFutureStub client;
    /**
     * A utitlity class for serialize and deserialize the messages sent and retrived by the client.
     */
    private ObjectSerializer objectSerializer;

    /**
     * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
     *
     * @param futureClient
     * @see io.dapr.client.DaprClientBuilder
     */
    DaprClientGrpcAdapter(DaprGrpc.DaprFutureStub futureClient) {
        client = futureClient;
        objectSerializer = new ObjectSerializer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Mono<Void> publishEvent(T event) {
        try {
            String serializedEvent = objectSerializer.serialize(event);
            DaprProtos.PublishEventEnvelope envelope = DaprProtos.PublishEventEnvelope.parseFrom(serializedEvent.getBytes());
            ListenableFuture<Empty> futureEmpty = client.publishEvent(envelope);
            return Mono.just(futureEmpty).flatMap(f -> {
                try {
                    f.get();
                } catch (Exception ex) {
                    return Mono.error(ex);
                }
                return Mono.empty();
            });
        } catch (Exception ex) {
            return Mono.error(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, K> Mono<T> invokeService(K request, Class<T> clazz) {
        try {
            String serializedRequest = objectSerializer.serialize(request);
            DaprProtos.InvokeServiceEnvelope envelope =
                    DaprProtos.InvokeServiceEnvelope.parseFrom(serializedRequest.getBytes());
            ListenableFuture<DaprProtos.InvokeServiceResponseEnvelope> futureResponse =
                    client.invokeService(envelope);
            return Mono.just(futureResponse).flatMap(f -> {
                try {
                    return Mono.just(objectSerializer.deserialize(f.get().getData().getValue().toStringUtf8(), clazz));
                } catch (Exception ex) {
                    return Mono.error(ex);
                }
            });

        } catch (Exception ex) {
            return Mono.error(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Mono<Void> invokeBinding(T request) {
        try {
            String serializedRequest = objectSerializer.serialize(request);
            DaprProtos.InvokeBindingEnvelope envelope =
                    DaprProtos.InvokeBindingEnvelope.parseFrom(serializedRequest.getBytes());
            ListenableFuture<Empty> futureEmpty = client.invokeBinding(envelope);
            return Mono.just(futureEmpty).flatMap(f -> {
                try {
                    f.get();
                } catch (Exception ex) {
                    return Mono.error(ex);
                }
                return Mono.empty();
            });
        } catch (Exception ex) {
            return Mono.error(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, K> Mono<T> getState(K key, Class<T> clazz) {
        try {
            String serializedRequest = objectSerializer.serialize(key);
            DaprProtos.GetStateEnvelope envelope = DaprProtos.GetStateEnvelope.parseFrom(serializedRequest.getBytes());
            ListenableFuture<DaprProtos.GetStateResponseEnvelope> futureResponse = client.getState(envelope);
            return Mono.just(futureResponse).flatMap(f -> {
                try {
                    return Mono.just(objectSerializer.deserialize(f.get().getData().getValue().toStringUtf8(), clazz));
                } catch (Exception ex) {
                    return Mono.error(ex);
                }
            });
        } catch (Exception ex) {
            return Mono.error(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Mono<Void> saveState(T state) {
        try {
            String serializedRequest = objectSerializer.serialize(state);
            DaprProtos.SaveStateEnvelope envelope = DaprProtos.SaveStateEnvelope.parseFrom(serializedRequest.getBytes());
            ListenableFuture<Empty> futureEmpty = client.saveState(envelope);
            return Mono.just(futureEmpty).flatMap(f -> {
                try {
                    f.get();
                } catch (Exception ex) {
                    return Mono.error(ex);
                }
                return Mono.empty();
            });
        } catch (Exception ex) {
            return Mono.error(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Mono<Void> deleteState(T key) {
        try {
            String serializedRequest = objectSerializer.serialize(key);
            DaprProtos.DeleteStateEnvelope envelope = DaprProtos.DeleteStateEnvelope.parseFrom(serializedRequest.getBytes());
            ListenableFuture<Empty> futureEmpty = client.deleteState(envelope);
            return Mono.just(futureEmpty).flatMap(f -> {
                try {
                    f.get();
                } catch (Exception ex) {
                    return Mono.error(ex);
                }
                return Mono.empty();
            });
        } catch (Exception ex) {
            return Mono.error(ex);
        }
    }

}