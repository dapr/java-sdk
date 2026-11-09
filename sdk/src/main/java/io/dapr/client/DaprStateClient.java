/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.client;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.GetBulkStateRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprStateProtos;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs the state management calls of {@link DaprClientImpl} and maps their protos.
 */
final class DaprStateClient {

  private final DaprGrpcCalls grpcCalls;

  private final DaprObjectSerializer stateSerializer;

  DaprStateClient(DaprGrpcCalls grpcCalls, DaprObjectSerializer stateSerializer) {
    this.grpcCalls = grpcCalls;
    this.stateSerializer = stateSerializer;
  }

  private CommonProtos.StateOptions.StateConsistency getGrpcStateConsistency(StateOptions options) {
    switch (options.getConsistency()) {
      case EVENTUAL:
        return CommonProtos.StateOptions.StateConsistency.CONSISTENCY_EVENTUAL;
      case STRONG:
        return CommonProtos.StateOptions.StateConsistency.CONSISTENCY_STRONG;
      default:
        throw new IllegalArgumentException("Missing Consistency mapping to gRPC Consistency enum");
    }
  }

  private CommonProtos.StateOptions.StateConcurrency getGrpcStateConcurrency(StateOptions options) {
    switch (options.getConcurrency()) {
      case FIRST_WRITE:
        return CommonProtos.StateOptions.StateConcurrency.CONCURRENCY_FIRST_WRITE;
      case LAST_WRITE:
        return CommonProtos.StateOptions.StateConcurrency.CONCURRENCY_LAST_WRITE;
      default:
        throw new IllegalArgumentException("Missing StateConcurrency mapping to gRPC Concurrency enum");
    }
  }

  <T> Mono<State<T>> getState(GetStateRequest request, TypeRef<T> type) {
    try {
      final String stateStoreName = request.getStoreName();
      final String key = request.getKey();
      final StateOptions options = request.getStateOptions();
      final Map<String, String> metadata = request.getMetadata();

      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }
      DaprStateProtos.GetStateRequest.Builder builder = DaprStateProtos.GetStateRequest.newBuilder()
          .setStoreName(stateStoreName)
          .setKey(key);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      if (options != null && options.getConsistency() != null) {
        builder.setConsistency(getGrpcStateConsistency(options));
      }

      DaprStateProtos.GetStateRequest envelope = builder.build();

      return Mono.deferContextual(
          context ->
              grpcCalls.<DaprStateProtos.GetStateResponse>createMono(
                  it -> grpcCalls.intercept(context).getState(envelope, it)
              )
      ).map(
          it -> {
            try {
              return buildStateKeyValue(it, key, options, type);
            } catch (IOException ex) {
              throw DaprException.propagate(ex);
            }
          }
      );
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  <T> Mono<List<State<T>>> getBulkState(GetBulkStateRequest request, TypeRef<T> type) {
    try {
      final String stateStoreName = request.getStoreName();
      final List<String> keys = request.getKeys();
      final int parallelism = request.getParallelism();
      final Map<String, String> metadata = request.getMetadata();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if (keys == null || keys.isEmpty()) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }

      if (parallelism < 0) {
        throw new IllegalArgumentException("Parallelism cannot be negative.");
      }
      DaprStateProtos.GetBulkStateRequest.Builder builder = DaprStateProtos.GetBulkStateRequest.newBuilder()
          .setStoreName(stateStoreName)
          .addAllKeys(keys)
          .setParallelism(parallelism);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprStateProtos.GetBulkStateRequest envelope = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprStateProtos.GetBulkStateResponse>createMono(it -> grpcCalls.intercept(context)
              .getBulkState(envelope, it)
          )
      ).map(
          it ->
              it
                  .getItemsList()
                  .stream()
                  .map(b -> {
                    try {
                      return buildStateKeyValue(b, type);
                    } catch (Exception e) {
                      throw DaprException.propagate(e);
                    }
                  })
                  .collect(Collectors.toList())
      );
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private <T> State<T> buildStateKeyValue(
      DaprStateProtos.BulkStateItem item,
      TypeRef<T> type) throws IOException {
    String key = item.getKey();
    String error = item.getError();
    if (!Strings.isNullOrEmpty(error)) {
      return new State<>(key, error);
    }

    String etag = item.getEtag();
    if (etag.equals("")) {
      etag = null;
    }

    T value = null;
    if (type != null) {
      ByteString payload = item.getData();
      byte[] data = payload == null ? null : payload.toByteArray();
      value = stateSerializer.deserialize(data, type);
    }

    return new State<>(key, value, etag, item.getMetadataMap(), null);
  }

  private <T> State<T> buildStateKeyValue(
      DaprStateProtos.GetStateResponse response,
      String requestedKey,
      StateOptions stateOptions,
      TypeRef<T> type) throws IOException {
    ByteString payload = response.getData();
    byte[] data = payload == null ? null : payload.toByteArray();
    T value = null;
    if (type != null) {
      value = stateSerializer.deserialize(data, type);
    }

    String etag = response.getEtag();
    if (etag.equals("")) {
      etag = null;
    }
    return new State<>(requestedKey, value, etag, response.getMetadataMap(), stateOptions);
  }

  Mono<Void> executeStateTransaction(ExecuteStateTransactionRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final List<TransactionalStateOperation<?>> operations = request.getOperations();
      final Map<String, String> metadata = request.getMetadata();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      DaprStateProtos.ExecuteStateTransactionRequest.Builder builder = DaprStateProtos.ExecuteStateTransactionRequest
          .newBuilder();
      builder.setStoreName(stateStoreName);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      for (TransactionalStateOperation<?> operation : operations) {
        DaprStateProtos.TransactionalStateOperation.Builder operationBuilder = DaprStateProtos
            .TransactionalStateOperation
            .newBuilder();
        operationBuilder.setOperationType(operation.getOperation().toString().toLowerCase());
        operationBuilder.setRequest(buildStateRequest(operation.getRequest()).build());
        builder.addOperations(operationBuilder.build());
      }
      DaprStateProtos.ExecuteStateTransactionRequest req = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<Empty>createMono(it -> grpcCalls.intercept(context)
              .executeStateTransaction(req, it))
      ).then();
    } catch (Exception e) {
      return DaprException.wrapMono(e);
    }
  }

  Mono<Void> saveBulkState(SaveStateRequest request) {
    try {
      final String stateStoreName = request.getStoreName();
      final List<State<?>> states = request.getStates();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      DaprStateProtos.SaveStateRequest.Builder builder = DaprStateProtos.SaveStateRequest.newBuilder();
      builder.setStoreName(stateStoreName);
      for (State<?> state : states) {
        builder.addStates(buildStateRequest(state).build());
      }
      DaprStateProtos.SaveStateRequest req = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<Empty>createMono(it -> grpcCalls.intercept(context).saveState(req, it))
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private <T> CommonProtos.StateItem.Builder buildStateRequest(State<T> state) throws IOException {
    byte[] bytes = stateSerializer.serialize(state.getValue());

    CommonProtos.StateItem.Builder stateBuilder = CommonProtos.StateItem.newBuilder();
    if (state.getEtag() != null) {
      stateBuilder.setEtag(CommonProtos.Etag.newBuilder().setValue(state.getEtag()).build());
    }
    if (state.getMetadata() != null) {
      stateBuilder.putAllMetadata(state.getMetadata());
    }
    if (bytes != null) {
      stateBuilder.setValue(ByteString.copyFrom(bytes));
    }
    stateBuilder.setKey(state.getKey());
    CommonProtos.StateOptions.Builder optionBuilder = null;
    if (state.getOptions() != null) {
      StateOptions options = state.getOptions();
      optionBuilder = CommonProtos.StateOptions.newBuilder();
      if (options.getConcurrency() != null) {
        optionBuilder.setConcurrency(getGrpcStateConcurrency(options));
      }
      if (options.getConsistency() != null) {
        optionBuilder.setConsistency(getGrpcStateConsistency(options));
      }
    }
    if (optionBuilder != null) {
      stateBuilder.setOptions(optionBuilder.build());
    }
    return stateBuilder;
  }

  Mono<Void> deleteState(DeleteStateRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final String key = request.getKey();
      final StateOptions options = request.getStateOptions();
      final String etag = request.getEtag();
      final Map<String, String> metadata = request.getMetadata();

      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }

      CommonProtos.StateOptions.Builder optionBuilder = null;
      if (options != null) {
        optionBuilder = CommonProtos.StateOptions.newBuilder();
        if (options.getConcurrency() != null) {
          optionBuilder.setConcurrency(getGrpcStateConcurrency(options));
        }
        if (options.getConsistency() != null) {
          optionBuilder.setConsistency(getGrpcStateConsistency(options));
        }
      }
      DaprStateProtos.DeleteStateRequest.Builder builder = DaprStateProtos.DeleteStateRequest.newBuilder()
          .setStoreName(stateStoreName)
          .setKey(key);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      if (etag != null) {
        builder.setEtag(CommonProtos.Etag.newBuilder().setValue(etag).build());
      }

      if (optionBuilder != null) {
        builder.setOptions(optionBuilder.build());
      }

      DaprStateProtos.DeleteStateRequest req = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<Empty>createMono(it -> grpcCalls.intercept(context).deleteState(req, it))
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  <T> Mono<QueryStateResponse<T>> queryState(QueryStateRequest request, TypeRef<T> type) {
    try {
      if (request == null) {
        throw new IllegalArgumentException("Query state request cannot be null.");
      }
      final String storeName = request.getStoreName();
      final Map<String, String> metadata = request.getMetadata();
      if ((storeName == null) || (storeName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }

      String queryString;
      if (request.getQuery() != null) {
        queryString = AbstractDaprClient.JSON_REQUEST_MAPPER.writeValueAsString(request.getQuery());
      } else if (request.getQueryString() != null) {
        queryString = request.getQueryString();
      } else {
        throw new IllegalArgumentException("Both query and queryString fields are not set.");
      }

      DaprStateProtos.QueryStateRequest.Builder builder = DaprStateProtos.QueryStateRequest.newBuilder()
          .setStoreName(storeName)
          .setQuery(queryString);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprStateProtos.QueryStateRequest envelope = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprStateProtos.QueryStateResponse>createMono(
              it -> grpcCalls.intercept(context).queryStateAlpha1(envelope, it)
          )
      ).map(
          it -> {
            Map<String, String> resultMeta = it.getMetadataMap();
            String token = it.getToken();
            List<QueryStateItem<T>> res = it.getResultsList()
                .stream()
                .map(v -> {
                  try {
                    return buildQueryStateKeyValue(v, type);
                  } catch (Exception e) {
                    throw DaprException.propagate(e);
                  }
                })
                .collect(Collectors.toList());
            return new QueryStateResponse<>(res, token).setMetadata(metadata);
          });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private <T> QueryStateItem<T> buildQueryStateKeyValue(
      DaprStateProtos.QueryStateItem item,
      TypeRef<T> type) throws IOException {
    String key = item.getKey();
    String error = item.getError();
    if (!Strings.isNullOrEmpty(error)) {
      return new QueryStateItem<>(key, null, error);
    }
    ByteString payload = item.getData();
    byte[] data = payload == null ? null : payload.toByteArray();
    T value = null;
    if (type != null) {
      value = stateSerializer.deserialize(data, type);
    }

    String etag = item.getEtag();
    if (etag.equals("")) {
      etag = null;
    }
    return new QueryStateItem<>(key, value, etag);
  }
}
