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
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.DurationUtils;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * A DaprClient over HTTP for Actor's runtime.
 */
class DaprClientImpl implements DaprClient {

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
   * @see io.dapr.v1.DaprGrpc.DaprStub
   */
  private DaprGrpc.DaprStub client;

  /**
   * Internal constructor.
   *
   * @param channel channel (client needs to close channel after use).
   */
  DaprClientImpl(ManagedChannel channel) {
    this(DaprGrpc.newStub(channel));
  }

  /**
   * Internal constructor.
   *
   * @param daprStubClient Dapr's GRPC client.
   */
  DaprClientImpl(DaprGrpc.DaprStub daprStubClient) {
    this.client = daprStubClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> getState(String actorType, String actorId, String keyName) {
    DaprProtos.GetActorStateRequest req =
            DaprProtos.GetActorStateRequest.newBuilder()
                    .setActorType(actorType)
                    .setActorId(actorId)
                    .setKey(keyName)
                    .build();

    return Mono.<DaprProtos.GetActorStateResponse>create(it ->
            client.getActorState(req, createStreamObserver(it))).map(r -> r.getData().toByteArray());
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

    return Mono.<Empty>create(it -> client.executeActorStateTransaction(req, createStreamObserver(it))).then();
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
    DaprProtos.RegisterActorReminderRequest req =
            DaprProtos.RegisterActorReminderRequest.newBuilder()
                    .setActorType(actorType)
                    .setActorId(actorId)
                    .setName(reminderName)
                    .setData(ByteString.copyFrom(reminderParams.getData()))
                    .setDueTime(DurationUtils.convertDurationToDaprFormat(reminderParams.getDueTime()))
                    .setPeriod(DurationUtils.convertDurationToDaprFormat(reminderParams.getPeriod()))
                    .build();
    return Mono.<Empty>create(it -> client.registerActorReminder(req, createStreamObserver(it))).then().then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName) {
    DaprProtos.UnregisterActorReminderRequest req =
        DaprProtos.UnregisterActorReminderRequest.newBuilder()
            .setActorType(actorType)
            .setActorId(actorId)
            .setName(reminderName)
            .build();

    return Mono.<Empty>create(it -> client.unregisterActorReminder(req, createStreamObserver(it))).then().then();
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

    return Mono.<Empty>create(it -> client.registerActorTimer(req, createStreamObserver(it))).then().then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterTimer(String actorType, String actorId, String timerName) {
    DaprProtos.UnregisterActorTimerRequest req =
        DaprProtos.UnregisterActorTimerRequest.newBuilder()
            .setActorType(actorType)
            .setActorId(actorId)
            .setName(timerName)
            .build();

    return Mono.<Empty>create(it -> client.unregisterActorTimer(req, createStreamObserver(it))).then().then();
  }

  private <T> StreamObserver<T> createStreamObserver(MonoSink<T> sink) {
    return new StreamObserver<T>() {
      @Override
      public void onNext(T value) {
        sink.success(value);
      }

      @Override
      public void onError(Throwable t) {
        sink.error(DaprException.propagate(new ExecutionException(t)));
      }

      @Override
      public void onCompleted() {
        sink.success();
      }
    };
  }

}
