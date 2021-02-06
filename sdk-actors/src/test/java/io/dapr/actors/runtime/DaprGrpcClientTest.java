/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.dapr.utils.DurationUtils;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DaprGrpcClientTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
  public void getActorStateException() {
    SettableFuture<DaprProtos.GetActorStateResponse> settableFuture = SettableFuture.create();
    settableFuture.setException(new ArithmeticException());

    when(grpcStub.getActorState(argThat(new GetActorStateRequestMatcher(
        ACTOR_TYPE,
        ACTOR_ID,
        "MyKey"
    )))).thenReturn(settableFuture);
    Mono<byte[]> result = client.getState(ACTOR_TYPE, ACTOR_ID, "MyKey");
    Exception exception = assertThrows(Exception.class, () -> result.block());
    assertTrue(exception.getCause().getCause() instanceof ArithmeticException);
  }

  @Test
  public void getActorState() {
    byte[] data = "hello world".getBytes();
    SettableFuture<DaprProtos.GetActorStateResponse> settableFuture = SettableFuture.create();
    settableFuture.set(DaprProtos.GetActorStateResponse.newBuilder().setData(ByteString.copyFrom(data)).build());

    when(grpcStub.getActorState(argThat(new GetActorStateRequestMatcher(
        ACTOR_TYPE,
        ACTOR_ID,
        "MyKey"
    )))).thenReturn(settableFuture);
    Mono<byte[]> result = client.getState(ACTOR_TYPE, ACTOR_ID, "MyKey");
    assertArrayEquals(data, result.block());
  }

  @Test
  public void saveActorStateTransactionallyException() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    settableFuture.setException(new ArithmeticException());

    when(grpcStub.executeActorStateTransaction(argThat(new ExecuteActorStateTransactionRequestMatcher(
        ACTOR_TYPE,
        ACTOR_ID,
        new ArrayList<>()
    )))).thenReturn(settableFuture);
    Mono<Void> result = client.saveStateTransactionally(ACTOR_TYPE, ACTOR_ID, new ArrayList<>());
    Exception exception = assertThrows(Exception.class, () -> result.block());
    assertTrue(exception.getCause().getCause() instanceof ArithmeticException);
  }

  @Test
  public void saveActorStateTransactionally() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    settableFuture.set(Empty.newBuilder().build());

    ActorStateOperation[] operations = new ActorStateOperation[] {
        new ActorStateOperation("upsert", "mykey", "hello world"),
        new ActorStateOperation("delete", "mykey", null),
    };

    when(grpcStub.executeActorStateTransaction(argThat(new ExecuteActorStateTransactionRequestMatcher(
        ACTOR_TYPE,
        ACTOR_ID,
        Arrays.asList(operations)
    )))).thenReturn(settableFuture);
    Mono<Void> result = client.saveStateTransactionally(ACTOR_TYPE, ACTOR_ID, Arrays.asList(operations));
    result.block();
  }

  @Test
  public void saveActorStateTransactionallyByteArray() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    settableFuture.set(Empty.newBuilder().build());

    ActorStateOperation[] operations = new ActorStateOperation[] {
        new ActorStateOperation("upsert", "mykey", "hello world".getBytes()),
        new ActorStateOperation("delete", "mykey", null),
    };

    when(grpcStub.executeActorStateTransaction(argThat(new ExecuteActorStateTransactionRequestMatcher(
        ACTOR_TYPE,
        ACTOR_ID,
        Arrays.asList(operations)
    )))).thenReturn(settableFuture);
    Mono<Void> result = client.saveStateTransactionally(ACTOR_TYPE, ACTOR_ID, Arrays.asList(operations));
    result.block();
  }

  @Test
  public void saveActorStateTransactionallyInvalidValueType() {
    ActorStateOperation[] operations = new ActorStateOperation[] {
        new ActorStateOperation("upsert", "mykey", 123),
        new ActorStateOperation("delete", "mykey", null),
    };

    Mono<Void> result = client.saveStateTransactionally(ACTOR_TYPE, ACTOR_ID, Arrays.asList(operations));
    assertThrows(IllegalArgumentException.class, () -> result.block());
  }


  @Test
  public void registerActorReminder() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    settableFuture.set(Empty.newBuilder().build());

    String reminderName = "myreminder";
    ActorReminderParams params = new ActorReminderParams(
        "hello world".getBytes(),
        Duration.ofSeconds(1),
        Duration.ofSeconds(2)
    );

    when(grpcStub.registerActorReminder(argThat(argument -> {
      assertEquals(ACTOR_TYPE, argument.getActorType());
      assertEquals(ACTOR_ID, argument.getActorId());
      assertEquals(reminderName, argument.getName());
      assertEquals(DurationUtils.convertDurationToDaprFormat(params.getDueTime()), argument.getDueTime());
      assertEquals(DurationUtils.convertDurationToDaprFormat(params.getPeriod()), argument.getPeriod());
      return true;
    }))).thenReturn(settableFuture);
    Mono<Void> result = client.registerReminder(ACTOR_TYPE, ACTOR_ID, reminderName, params);
    result.block();
  }

  @Test
  public void unregisterActorReminder() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    settableFuture.set(Empty.newBuilder().build());

    String reminderName = "myreminder";

    when(grpcStub.unregisterActorReminder(argThat(argument -> {
      assertEquals(ACTOR_TYPE, argument.getActorType());
      assertEquals(ACTOR_ID, argument.getActorId());
      assertEquals(reminderName, argument.getName());
      return true;
    }))).thenReturn(settableFuture);
    Mono<Void> result = client.unregisterReminder(ACTOR_TYPE, ACTOR_ID, reminderName);
    result.block();
  }

  @Test
  public void registerActorTimer() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    settableFuture.set(Empty.newBuilder().build());

    String timerName = "mytimer";
    String callback = "mymethod";
    ActorTimerParams params = new ActorTimerParams(
        callback,
        "hello world".getBytes(),
        Duration.ofSeconds(1),
        Duration.ofSeconds(2)
    );

    when(grpcStub.registerActorTimer(argThat(argument -> {
      assertEquals(ACTOR_TYPE, argument.getActorType());
      assertEquals(ACTOR_ID, argument.getActorId());
      assertEquals(timerName, argument.getName());
      assertEquals(callback, argument.getCallback());
      assertEquals(DurationUtils.convertDurationToDaprFormat(params.getDueTime()), argument.getDueTime());
      assertEquals(DurationUtils.convertDurationToDaprFormat(params.getPeriod()), argument.getPeriod());
      return true;
    }))).thenReturn(settableFuture);
    Mono<Void> result = client.registerTimer(ACTOR_TYPE, ACTOR_ID, timerName, params);
    result.block();
  }

  @Test
  public void unregisterActorTimer() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    settableFuture.set(Empty.newBuilder().build());

    String timerName = "mytimer";

    when(grpcStub.unregisterActorTimer(argThat(argument -> {
      assertEquals(ACTOR_TYPE, argument.getActorType());
      assertEquals(ACTOR_ID, argument.getActorId());
      assertEquals(timerName, argument.getName());
      return true;
    }))).thenReturn(settableFuture);
    Mono<Void> result = client.unregisterTimer(ACTOR_TYPE, ACTOR_ID, timerName);
    result.block();
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

  private static class GetActorStateRequestMatcher implements ArgumentMatcher<DaprProtos.GetActorStateRequest> {

    private final String actorType;

    private final String actorId;

    private final String key;

    GetActorStateRequestMatcher(String actorType, String actorId, String key) {
      this.actorType = actorType;
      this.actorId = actorId;
      this.key = key;
    }

    @Override
    public boolean matches(DaprProtos.GetActorStateRequest argument) {
      if (argument == null) {
        return false;
      }

      return actorType.equals(argument.getActorType())
          && actorId.equals(argument.getActorId())
          && key.equals(argument.getKey());
    }
  }

  private static class ExecuteActorStateTransactionRequestMatcher
      implements ArgumentMatcher<DaprProtos.ExecuteActorStateTransactionRequest> {

    private final String actorType;

    private final String actorId;

    private final List<ActorStateOperation> operations;

    ExecuteActorStateTransactionRequestMatcher(String actorType, String actorId, List<ActorStateOperation> operations) {
      this.actorType = actorType;
      this.actorId = actorId;
      this.operations = operations;
    }

    @Override
    public boolean matches(DaprProtos.ExecuteActorStateTransactionRequest argument) {
      if (argument == null) {
        return false;
      }

      if (operations.size() != argument.getOperationsCount()) {
        return false;
      }

      if (!actorType.equals(argument.getActorType())
          || !actorId.equals(argument.getActorId())) {
        return false;
      }

      for(ActorStateOperation operation : operations) {
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
  }
}
