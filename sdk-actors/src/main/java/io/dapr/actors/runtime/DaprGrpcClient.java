/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.dapr.config.Properties;
import io.dapr.utils.DurationUtils;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.ManagedChannel;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * A DaprClient over HTTP for Actor's runtime.
 */
class DaprGrpcClient implements DaprClient {

  /**
   * Use to handle internal serialization.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Dapr's charset.
   */
  private static final Charset CHARSET = Properties.STRING_CHARSET.get();

  /**
   * The GRPC client to be used.
   *
   * @see io.dapr.v1.DaprGrpc.DaprFutureStub
   */
  private DaprGrpc.DaprFutureStub client;

  /**
   * Internal constructor.
   *
   * @param channel channel (client needs to close channel after use).
   */
  DaprGrpcClient(ManagedChannel channel) {
    this(DaprGrpc.newFutureStub(channel));
  }

  /**
   * Internal constructor.
   *
   * @param grpcClient Dapr's GRPC client.
   */
  DaprGrpcClient(DaprGrpc.DaprFutureStub grpcClient) {
    this.client = grpcClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> getState(String actorType, String actorId, String keyName) {
    return Mono.fromCallable(() -> {
      DaprProtos.GetActorStateRequest req =
          DaprProtos.GetActorStateRequest.newBuilder()
              .setActorType(actorType)
              .setActorId(actorId)
              .setKey(keyName)
              .build();

      ListenableFuture<DaprProtos.GetActorStateResponse> futureResponse = client.getActorState(req);
      return futureResponse.get();
    }).map(r -> r.getData().toByteArray());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveStateTransactionally(
      String actorType,
      String actorId,
      List<ActorStateOperation> operations) {
    List<DaprProtos.TransactionalActorStateOperation> grpcOps = new ArrayList<>();
    for (ActorStateOperation op : operations) {
      String operationType = op.getOperationType();
      String key = op.getKey();
      Object value = op.getValue();
      DaprProtos.TransactionalActorStateOperation.Builder opBuilder =
          DaprProtos.TransactionalActorStateOperation.newBuilder()
              .setOperationType(operationType)
              .setKey(key);
      if (value != null) {
        if (value instanceof String) {
          opBuilder.setValue(Any.newBuilder().setValue(ByteString.copyFrom((String) value, CHARSET)));
        } else if (value instanceof byte[]) {
          try {
            String base64 = OBJECT_MAPPER.writeValueAsString(value);
            opBuilder.setValue(Any.newBuilder().setValue(ByteString.copyFrom(base64, CHARSET)));
          } catch (IOException e) {
            return Mono.error(e);
          }
        } else {
          return Mono.error(() -> {
            throw new IllegalArgumentException("Actor state value must be String or byte[]");
          });
        }
      }

      grpcOps.add(opBuilder.build());
    }

    DaprProtos.ExecuteActorStateTransactionRequest req =
        DaprProtos.ExecuteActorStateTransactionRequest.newBuilder()
            .setActorType(actorType)
            .setActorId(actorId)
            .addAllOperations(grpcOps)
            .build();

    return Mono.fromCallable(() -> {
      ListenableFuture<Empty> futureResponse = client.executeActorStateTransaction(req);
      return futureResponse.get();
    }).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerReminder(
      String actorType,
      String actorId,
      String reminderName,
      ActorReminderParams reminderParams) {
    return Mono.fromCallable(() -> {
      DaprProtos.RegisterActorReminderRequest req =
          DaprProtos.RegisterActorReminderRequest.newBuilder()
              .setActorType(actorType)
              .setActorId(actorId)
              .setName(reminderName)
              .setData(ByteString.copyFrom(reminderParams.getData()))
              .setDueTime(DurationUtils.convertDurationToDaprFormat(reminderParams.getDueTime()))
              .setPeriod(DurationUtils.convertDurationToDaprFormat(reminderParams.getPeriod()))
              .build();

      ListenableFuture<Empty> futureResponse = client.registerActorReminder(req);
      futureResponse.get();
      return null;
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName) {
    return Mono.fromCallable(() -> {
      DaprProtos.UnregisterActorReminderRequest req =
          DaprProtos.UnregisterActorReminderRequest.newBuilder()
              .setActorType(actorType)
              .setActorId(actorId)
              .setName(reminderName)
              .build();

      ListenableFuture<Empty> futureResponse = client.unregisterActorReminder(req);
      futureResponse.get();
      return null;
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerTimer(
      String actorType,
      String actorId,
      String timerName,
      ActorTimerParams timerParams) {
    return Mono.fromCallable(() -> {
      DaprProtos.RegisterActorTimerRequest req =
          DaprProtos.RegisterActorTimerRequest.newBuilder()
              .setActorType(actorType)
              .setActorId(actorId)
              .setName(timerName)
              .setCallback(timerParams.getCallback())
              .setData(ByteString.copyFrom(timerParams.getData()))
              .setDueTime(DurationUtils.convertDurationToDaprFormat(timerParams.getDueTime()))
              .setPeriod(DurationUtils.convertDurationToDaprFormat(timerParams.getPeriod()))
              .build();

      ListenableFuture<Empty> futureResponse = client.registerActorTimer(req);
      futureResponse.get();
      return null;
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterTimer(String actorType, String actorId, String timerName) {
    return Mono.fromCallable(() -> {
      DaprProtos.UnregisterActorTimerRequest req =
          DaprProtos.UnregisterActorTimerRequest.newBuilder()
              .setActorType(actorType)
              .setActorId(actorId)
              .setName(timerName)
              .build();

      ListenableFuture<Empty> futureResponse = client.unregisterActorTimer(req);
      futureResponse.get();
      return null;
    });
  }

}
