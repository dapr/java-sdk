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

package io.dapr.actors.client;

import com.google.protobuf.ByteString;
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
import java.util.concurrent.ExecutionException;

import static io.dapr.actors.TestUtils.assertThrowsDaprException;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

public class DaprGrpcClientTest {

  private static final String ACTOR_TYPE = "MyActorType";

  private static final String ACTOR_ID_OK = "123-Ok";

  private static final String ACTOR_ID_NULL_INPUT = "123-Null";

  private static final String ACTOR_ID_EXCEPTION = "123-Exception";

  private static final String METHOD_NAME = "myMethod";

  private static final byte[] REQUEST_PAYLOAD = "{ \"id\": 123 }".getBytes();

  private static final byte[] RESPONSE_PAYLOAD = "\"OK\"".getBytes();

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private final DaprGrpc.DaprImplBase serviceImpl =
      mock(DaprGrpc.DaprImplBase.class, delegatesTo(
          new DaprGrpc.DaprImplBase() {
            @Override
            public void invokeActor(DaprProtos.InvokeActorRequest request,
                StreamObserver<DaprProtos.InvokeActorResponse> responseObserver) {
              assertEquals(ACTOR_TYPE, request.getActorType());
              assertEquals(METHOD_NAME, request.getMethod());
              switch (request.getActorId()) {
                case ACTOR_ID_OK:
                  assertArrayEquals(REQUEST_PAYLOAD, request.getData().toByteArray());
                  responseObserver.onNext(
                      DaprProtos.InvokeActorResponse.newBuilder().setData(ByteString.copyFrom(RESPONSE_PAYLOAD))
                          .build());
                  responseObserver.onCompleted();
                  return;
                case ACTOR_ID_NULL_INPUT:
                  assertArrayEquals(new byte[0], request.getData().toByteArray());
                  responseObserver.onNext(
                      DaprProtos.InvokeActorResponse.newBuilder().setData(ByteString.copyFrom(RESPONSE_PAYLOAD))
                          .build());
                  responseObserver.onCompleted();
                  return;

                case ACTOR_ID_EXCEPTION:
                  Throwable e = new ArithmeticException();
                  StatusException se = new StatusException(Status.UNKNOWN.withCause(e));
                  responseObserver.onError(se);
                  return;
              }
              super.invokeActor(request, responseObserver);
            }
          }));

  private DaprClientImpl client;

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
    client = new DaprClientImpl(DaprGrpc.newStub(channel), null);
  }

  @Test
  public void invoke() {
    Mono<byte[]> result = client.invoke(ACTOR_TYPE, ACTOR_ID_OK, METHOD_NAME, REQUEST_PAYLOAD);
    assertArrayEquals(RESPONSE_PAYLOAD, result.block());
  }

  @Test
  public void invokeNullPayload() {
    Mono<byte[]> result = client.invoke(ACTOR_TYPE, ACTOR_ID_NULL_INPUT, METHOD_NAME, null);
    assertArrayEquals(RESPONSE_PAYLOAD, result.block());
  }

  @Test
  public void invokeException() {
    Mono<byte[]> result = client.invoke(ACTOR_TYPE, ACTOR_ID_EXCEPTION, METHOD_NAME, null);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeNotHotMono() {
    client.invoke(ACTOR_TYPE, ACTOR_ID_EXCEPTION, METHOD_NAME, null);
    // No exception thrown because Mono is ignored here.
  }

}
