/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.actors.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static io.dapr.actors.TestUtils.assertThrowsDaprException;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DaprGrpcClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String ACTOR_TYPE = "MyActorType";

    private static final String ACTOR_ID = "1234567890";

    private static final String KEY = "MyKey";

    private static final String ACTOR_EXCEPTION = "1_exception";

    private static final String REMINDER_NAME = "myreminder";

    private static final String TIMER_NAME = "timerName";

    private static final byte[] RESPONSE_PAYLOAD = "\"hello world\"".getBytes();

    private static final List<ActorStateOperation> OPERATIONS =  Arrays.asList(
            new ActorStateOperation("upsert", "mykey", "hello world".getBytes()),
            new ActorStateOperation("delete", "mykey", null));

    private final DaprGrpc.DaprImplBase serviceImpl = new CustomDaprClient();

    private DaprClientImpl client;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @BeforeEach
    public void setup() throws IOException {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(serviceImpl).build().start());

        // Create a client channel and register for automatic graceful shutdown.
        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build());

        // Create a HelloWorldClient using the in-process channel;
        client = new DaprClientImpl(DaprGrpc.newStub(channel));
    }

    @Test
    public void getActorStateException() {
        Mono<byte[]> result = client.getState(ACTOR_TYPE, ACTOR_EXCEPTION, KEY);
        assertThrowsDaprException(
                ExecutionException.class,
                "UNKNOWN",
                "UNKNOWN: ",
                result::block);
    }

    @Test
    public void getActorState() {
        Mono<byte[]> result = client.getState(ACTOR_TYPE, ACTOR_ID, KEY);
        assertArrayEquals(RESPONSE_PAYLOAD, result.block());
    }

    @Test
    public void saveActorStateTransactionallyException() {
        Mono<Void> result = client.saveStateTransactionally(ACTOR_TYPE, ACTOR_EXCEPTION, OPERATIONS);
        assertThrowsDaprException(
                ExecutionException.class,
                "UNKNOWN",
                "UNKNOWN: ",
                result::block);
    }
    @Test
    public void saveActorStateTransactionally() {
        Mono<Void> result = client.saveStateTransactionally(ACTOR_TYPE, ACTOR_ID, OPERATIONS);
        result.block();
    }

    @Test
    public void saveActorStateTransactionallyByteArray() {
        Mono<Void> result = client.saveStateTransactionally(ACTOR_TYPE, ACTOR_ID, OPERATIONS);
        result.block();
    }

    @Test
    public void saveActorStateTransactionallyInvalidValueType() {
        ActorStateOperation[] operations = new ActorStateOperation[]{
                new ActorStateOperation("upsert", "mykey", 123),
                new ActorStateOperation("delete", "mykey", null),
        };

        Mono<Void> result = client.saveStateTransactionally(ACTOR_TYPE, ACTOR_ID, Arrays.asList(operations));
        assertThrows(IllegalArgumentException.class, result::block);
    }


    @Test
    public void registerActorReminder() {
        ActorReminderParams params = new ActorReminderParams(
                "hello world".getBytes(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)
        );
        Mono<Void> result = client.registerReminder(ACTOR_TYPE, ACTOR_ID, REMINDER_NAME, params);
        result.block();
    }

    @Test
    public void unregisterActorReminder() {

        Mono<Void> result = client.unregisterReminder(ACTOR_TYPE, ACTOR_ID, REMINDER_NAME);
        result.block();
    }

    @Test
    public void registerActorTimer() {
        String callback = "mymethod";
        ActorTimerParams params = new ActorTimerParams(
                callback,
                "hello world".getBytes(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)
        );

        Mono<Void> result = client.registerTimer(ACTOR_TYPE, ACTOR_ID, TIMER_NAME, params);
        result.block();
    }

    @Test
    public void unregisterActorTimer() {
        Mono<Void> result = client.unregisterTimer(ACTOR_TYPE, ACTOR_ID, TIMER_NAME);
        result.block();
    }


    private class CustomDaprClient extends DaprGrpc.DaprImplBase {

        @Override
        public void getActorState(DaprProtos.GetActorStateRequest request,
                                  StreamObserver<DaprProtos.GetActorStateResponse> responseObserver) {
            assertEquals(ACTOR_TYPE, request.getActorType());
            assertEquals(KEY, request.getKey());
            assertEquals(ACTOR_ID, request.getActorId());
            switch (request.getActorId()) {
                case ACTOR_ID:
                    populateObserver(responseObserver,  DaprProtos.GetActorStateResponse.newBuilder().setData(ByteString.copyFrom(RESPONSE_PAYLOAD))
                            .build());
                    return;

                case ACTOR_EXCEPTION:
                    throwException(responseObserver);
                    return;
            }
            super.getActorState(request, responseObserver);
        }

        public void executeActorStateTransaction(io.dapr.v1.DaprProtos.ExecuteActorStateTransactionRequest request,
                                                 io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
            assertEquals(ACTOR_TYPE, request.getActorType());
            assertEquals(ACTOR_ID, request.getActorId());
            assertTrue(new OperationsMatcher(OPERATIONS).matches(request));
            switch (request.getActorId()) {
                case ACTOR_ID:
                    populateObserver(responseObserver, Empty.newBuilder().build());
                    return;

                case ACTOR_EXCEPTION:
                    throwException(responseObserver);
                    return;
            }
            super.executeActorStateTransaction(request, responseObserver);
        }

        @Override
        public void registerActorReminder(io.dapr.v1.DaprProtos.RegisterActorReminderRequest request,
                                          io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
            assertEquals(REMINDER_NAME, request.getName());
            assertEquals("0h0m1s0ms", request.getDueTime());
            assertEquals("0h0m2s0ms", request.getPeriod());
            assertEquals(ACTOR_TYPE, request.getActorType());
            assertEquals(ACTOR_ID, request.getActorId());
            switch (request.getActorId()) {
                case ACTOR_ID:
                    populateObserver(responseObserver, Empty.newBuilder().build());
                    return;

                case ACTOR_EXCEPTION:
                    throwException(responseObserver);
                    return;
            }
            super.registerActorReminder(request, responseObserver);
        }

        public void registerActorTimer(io.dapr.v1.DaprProtos.RegisterActorTimerRequest request,
                                       io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
            assertEquals(ACTOR_TYPE, request.getActorType());
            assertEquals(ACTOR_ID, request.getActorId());
            assertEquals(TIMER_NAME, request.getName());
            assertEquals("mymethod", request.getCallback());
            assertEquals("0h0m1s0ms", request.getDueTime());
            assertEquals("0h0m2s0ms", request.getPeriod());
            switch (request.getActorId()) {
                case ACTOR_ID:
                    populateObserver(responseObserver, Empty.newBuilder().build());
                    return;

                case ACTOR_EXCEPTION:
                    throwException(responseObserver);
                    return;
            }
            super.registerActorTimer(request, responseObserver);
        }

        /**
         * <pre>
         * Unregister an actor timer.
         * </pre>
         */
        public void unregisterActorTimer(io.dapr.v1.DaprProtos.UnregisterActorTimerRequest request,
                                         io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
            assertEquals(ACTOR_TYPE, request.getActorType());
            assertEquals(ACTOR_ID, request.getActorId());
            assertEquals(TIMER_NAME, request.getName());
            switch (request.getActorId()) {
                case ACTOR_ID:
                    populateObserver(responseObserver, Empty.newBuilder().build());
                    return;

                case ACTOR_EXCEPTION:
                    throwException(responseObserver);
                    return;
            }
            super.unregisterActorTimer(request, responseObserver);
        }

        public void unregisterActorReminder(io.dapr.v1.DaprProtos.UnregisterActorReminderRequest request,
                                            io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
            assertEquals(ACTOR_TYPE, request.getActorType());
            assertEquals(ACTOR_ID, request.getActorId());
            assertEquals(REMINDER_NAME, request.getName());
            switch (request.getActorId()) {
                case ACTOR_ID:
                    populateObserver(responseObserver, Empty.newBuilder().build());
                    return;

                case ACTOR_EXCEPTION:
                    throwException(responseObserver);
                    return;
            }
            super.unregisterActorReminder(request, responseObserver);
        }

        private void throwException(StreamObserver<?> responseObserver) {
            Throwable e = new ArithmeticException();
            StatusException se = new StatusException(Status.UNKNOWN.withCause(e));
            responseObserver.onError(se);
        }

        private <T extends GeneratedMessageV3> void populateObserver(StreamObserver<T> responseObserver, GeneratedMessageV3 generatedMessageV3) {
            responseObserver.onNext((T) generatedMessageV3);
            responseObserver.onCompleted();
        }
    }

    private static class OperationsMatcher {

        private final List<ActorStateOperation> operations;

        OperationsMatcher(List<ActorStateOperation> operations) {
            this.operations = operations;
        }

        public boolean matches(DaprProtos.ExecuteActorStateTransactionRequest argument) {
            if (argument == null) {
                return false;
            }

            if (operations.size() != argument.getOperationsCount()) {
                return false;
            }

            for (ActorStateOperation operation : operations) {
                boolean found = false;
                for (DaprProtos.TransactionalActorStateOperation grpcOperation : argument.getOperationsList()) {
                    if (operation.getKey().equals(grpcOperation.getKey())
                            && operation.getOperationType().equals(grpcOperation.getOperationType())
                            && nullableEquals(operation.getValue(), grpcOperation.getValue())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    return false;
                }
            }

            return true;
        }

        private static boolean nullableEquals(Object one, Any another) {
            if (one == null) {
                return another.getValue().isEmpty();
            }

            if ((one == null) ^ (another == null)) {
                return false;
            }

            try {
                Any oneAny = getAny(one);
                return oneAny.getValue().equals(another.getValue());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        private static Any getAny(Object value) throws IOException {
            if (value instanceof byte[]) {
                String base64 = OBJECT_MAPPER.writeValueAsString(value);
                return Any.newBuilder().setValue(ByteString.copyFrom(base64.getBytes())).build();
            } else if (value instanceof String) {
                return Any.newBuilder().setValue(ByteString.copyFrom(((String)value).getBytes())).build();
            }

            throw new IllegalArgumentException("Must be byte[] or String");
        }
    }
}
