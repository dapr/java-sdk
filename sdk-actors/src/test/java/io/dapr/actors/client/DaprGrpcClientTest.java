/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;

import static io.dapr.actors.TestUtils.assertThrowsDaprException;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DaprGrpcClientTest {

  private static final String ACTOR_TYPE = "MyActorType";

  private static final String ACTOR_ID = "1234567890";

  private DaprGrpc.DaprFutureStub grpcStub;

  private DaprGrpcClient client;

  @Before
  public void setup() {
    grpcStub = mock(DaprGrpc.DaprFutureStub.class);
    client = new DaprGrpcClient(grpcStub);
  }

  @Test
  public void invoke() {
    String methodName = "mymethod";
    byte[] payload = "{ \"id\": 123 }".getBytes();
    byte[] response = "\"OK\"".getBytes();

    SettableFuture<DaprProtos.InvokeActorResponse> settableFuture = SettableFuture.create();
    settableFuture.set(DaprProtos.InvokeActorResponse.newBuilder().setData(ByteString.copyFrom(response)).build());

    when(grpcStub.invokeActor(argThat(argument -> {
      assertEquals(ACTOR_TYPE, argument.getActorType());
      assertEquals(ACTOR_ID, argument.getActorId());
      assertEquals(methodName, argument.getMethod());
      assertArrayEquals(payload, argument.getData().toByteArray());
      return true;
    }))).thenReturn(settableFuture);
    Mono<byte[]> result = client.invoke(ACTOR_TYPE, ACTOR_ID, methodName, payload);
    assertArrayEquals(response, result.block());
  }

  @Test
  public void invokeNullPayload() {
    String methodName = "mymethod";
    byte[] response = "\"OK\"".getBytes();

    SettableFuture<DaprProtos.InvokeActorResponse> settableFuture = SettableFuture.create();
    settableFuture.set(DaprProtos.InvokeActorResponse.newBuilder().setData(ByteString.copyFrom(response)).build());

    when(grpcStub.invokeActor(argThat(argument -> {
      assertEquals(ACTOR_TYPE, argument.getActorType());
      assertEquals(ACTOR_ID, argument.getActorId());
      assertEquals(methodName, argument.getMethod());
      assertArrayEquals(new byte[0], argument.getData().toByteArray());
      return true;
    }))).thenReturn(settableFuture);
    Mono<byte[]> result = client.invoke(ACTOR_TYPE, ACTOR_ID, methodName, null);
    assertArrayEquals(response, result.block());
  }

  @Test
  public void invokeException() {
    String methodName = "mymethod";

    SettableFuture<DaprProtos.InvokeActorResponse> settableFuture = SettableFuture.create();
    settableFuture.setException(new ArithmeticException());

    when(grpcStub.invokeActor(argThat(argument -> {
      assertEquals(ACTOR_TYPE, argument.getActorType());
      assertEquals(ACTOR_ID, argument.getActorId());
      assertEquals(methodName, argument.getMethod());
      assertArrayEquals(new byte[0], argument.getData().toByteArray());
      return true;
    }))).thenReturn(settableFuture);
    Mono<byte[]> result = client.invoke(ACTOR_TYPE, ACTOR_ID, methodName, null);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.ArithmeticException",
        () -> result.block());
  }

  @Test
  public void invokeNotHotMono() {
    String methodName = "mymethod";

    SettableFuture<DaprProtos.InvokeActorResponse> settableFuture = SettableFuture.create();
    settableFuture.setException(new ArithmeticException());

    when(grpcStub.invokeActor(argThat(argument -> {
      assertEquals(ACTOR_TYPE, argument.getActorType());
      assertEquals(ACTOR_ID, argument.getActorId());
      assertEquals(methodName, argument.getMethod());
      assertArrayEquals(new byte[0], argument.getData().toByteArray());
      return true;
    }))).thenReturn(settableFuture);
    client.invoke(ACTOR_TYPE, ACTOR_ID, methodName, null);
    // No exception thrown because Mono is ignored here.
  }

}
