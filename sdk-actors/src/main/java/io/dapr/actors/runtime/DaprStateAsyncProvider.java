/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.dapr.actors.ActorId;
import io.dapr.client.DaprObjectSerializer;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * State Provider to interact with Dapr runtime to handle state.
 */
class DaprStateAsyncProvider {

    /**
     * Shared Json Factory as per Jackson's documentation, used only for this class.
     */
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private final DaprClient daprClient;

    private final DaprObjectSerializer serializer;

    DaprStateAsyncProvider(DaprClient daprClient, DaprObjectSerializer serializer) {
        this.daprClient = daprClient;
        this.serializer = serializer;
    }

    <T> Mono<T> load(String actorType, ActorId actorId, String stateName, Class<T> clazz) {
        Mono<byte[]> result = this.daprClient.getActorState(actorType, actorId.toString(), stateName);

        return result.flatMap(s -> {
                    try {
                        T response = this.serializer.deserialize(s, clazz);
                        if (response == null) {
                            return Mono.empty();
                        }

                        return Mono.just(response);
                    } catch (IOException e) {
                        return Mono.error(new RuntimeException(e));
                    }
                });
    }

    Mono<Boolean> contains(String actorType, ActorId actorId, String stateName) {
        Mono<byte[]> result = this.daprClient.getActorState(actorType, actorId.toString(), stateName);
        return result.map(s -> true).defaultIfEmpty(false);
    }

    /**
     * Saves state changes transactionally.
     * [
     * {
     * "operation": "upsert",
     * "request": {
     * "key": "key1",
     * "value": "myData"
     * }
     * },
     * {
     * "operation": "delete",
     * "request": {
     * "key": "key2"
     * }
     * }
     * ]
     *
     * @param actorType    Name of the actor being changed.
     * @param actorId      Identifier of the actor being changed.
     * @param stateChanges Collection of changes to be performed transactionally.
     * @return Void.
     */
    Mono<Void> apply(String actorType, ActorId actorId, ActorStateChange... stateChanges) {
        if ((stateChanges == null) || stateChanges.length == 0) {
            return Mono.empty();
        }

        int count = 0;
        // Constructing the JSON via a stream API to avoid creating transient objects to be instantiated.
        byte[] payload = null;
        try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
            JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
            // Start array
            generator.writeStartArray();

            for (ActorStateChange stateChange : stateChanges) {
                if ((stateChange == null) || (stateChange.getChangeKind() == null)) {
                    continue;
                }

                String operationName = stateChange.getChangeKind().getDaprStateChangeOperation();
                if ((operationName == null) || (operationName.length() == 0)) {
                    continue;
                }

                count++;

                // Start operation object.
                generator.writeStartObject();
                generator.writeStringField("operation", operationName);

                // Start request object.
                generator.writeObjectFieldStart("request");
                generator.writeStringField("key", stateChange.getStateName());
                if ((stateChange.getChangeKind() == ActorStateChangeKind.UPDATE) || (stateChange.getChangeKind() == ActorStateChangeKind.ADD)) {
                    generator.writeBinaryField("value", this.serializer.serialize(stateChange.getValue()));
                }
                // End request object.
                generator.writeEndObject();

                // End operation object.
                generator.writeEndObject();
            }

            // End array
            generator.writeEndArray();

            generator.close();
            writer.flush();
            payload = writer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return Mono.error(e);
        }

        if (count == 0) {
            // No-op since there is no operation to be performed.
            Mono.empty();
        }

        return this.daprClient.saveActorStateTransactionally(actorType, actorId.toString(), payload);
    }
}
