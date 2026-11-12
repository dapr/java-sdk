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

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.rpc.Status;
import io.dapr.client.domain.BatchQueryVectorsRequestAlpha1;
import io.dapr.client.domain.BatchQueryVectorsResultAlpha1;
import io.dapr.client.domain.CreateVectorCollectionRequestAlpha1;
import io.dapr.client.domain.DeleteVectorsRequestAlpha1;
import io.dapr.client.domain.DistanceMetric;
import io.dapr.client.domain.FailedItem;
import io.dapr.client.domain.GetVectorCollectionResponseAlpha1;
import io.dapr.client.domain.GetVectorsRequestAlpha1;
import io.dapr.client.domain.IndexAck;
import io.dapr.client.domain.IndexingOptions;
import io.dapr.client.domain.IndexingWaitTimeoutAction;
import io.dapr.client.domain.QueryVectorsRequestAlpha1;
import io.dapr.client.domain.QueryVectorsResponseAlpha1;
import io.dapr.client.domain.UpsertVectorsRequestAlpha1;
import io.dapr.client.domain.UpsertVectorsResponseAlpha1;
import io.dapr.client.domain.VectorMatch;
import io.dapr.client.domain.VectorQuery;
import io.dapr.client.domain.VectorRecord;
import io.dapr.exceptions.DaprErrorDetails;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.DaprSearchProtos;
import io.dapr.v1.DaprVectorProtos;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Implements the vector building block (alpha) of {@link DaprPreviewClient} over gRPC.
 */
final class DaprVectorClient {

  private final DaprGrpcCalls grpcCalls;

  DaprVectorClient(DaprGrpcCalls grpcCalls) {
    this.grpcCalls = grpcCalls;
  }

  Mono<Void> createVectorCollection(CreateVectorCollectionRequestAlpha1 request) {
    try {
      requireRequest(request);
      validateCollection(request.getStoreName(), request.getCollection());
      if (request.getDimensions() <= 0) {
        throw new IllegalArgumentException("Dimensions must be greater than zero.");
      }

      DaprVectorProtos.CreateCollectionRequestAlpha1.Builder builder =
          DaprVectorProtos.CreateCollectionRequestAlpha1.newBuilder()
              .setStoreName(request.getStoreName())
              .setCollection(request.getCollection())
              .setDimensions(request.getDimensions())
              .setMetric(toProto(request.getMetric()));
      putMetadata(builder::putAllMetadata, request.getMetadata());
      DaprVectorProtos.CreateCollectionRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<Empty>createMono(
              it -> grpcCalls.intercept(context).createCollectionAlpha1(protoRequest, it)
          )
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<GetVectorCollectionResponseAlpha1> getVectorCollection(
      String storeName, String collection, Map<String, String> metadata) {
    try {
      validateCollection(storeName, collection);

      DaprVectorProtos.GetCollectionRequestAlpha1.Builder builder =
          DaprVectorProtos.GetCollectionRequestAlpha1.newBuilder()
              .setStoreName(storeName)
              .setCollection(collection);
      putMetadata(builder::putAllMetadata, metadata);
      DaprVectorProtos.GetCollectionRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprVectorProtos.GetCollectionResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).getCollectionAlpha1(protoRequest, it)
          )
      ).map(response -> new GetVectorCollectionResponseAlpha1(
          response.getCollection(),
          response.getRecordCount(),
          response.getPropertiesMap(),
          response.getDimensions(),
          fromProto(response.getMetric())));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<List<String>> listVectorCollections(String storeName, Map<String, String> metadata) {
    try {
      validateStoreName(storeName);

      DaprVectorProtos.ListCollectionsRequestAlpha1.Builder builder =
          DaprVectorProtos.ListCollectionsRequestAlpha1.newBuilder().setStoreName(storeName);
      putMetadata(builder::putAllMetadata, metadata);
      DaprVectorProtos.ListCollectionsRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprVectorProtos.ListCollectionsResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).listCollectionsAlpha1(protoRequest, it)
          )
      ).map(response -> List.copyOf(response.getCollectionsList()));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<Void> deleteVectorCollection(String storeName, String collection, Map<String, String> metadata) {
    try {
      validateCollection(storeName, collection);

      DaprVectorProtos.DeleteCollectionRequestAlpha1.Builder builder =
          DaprVectorProtos.DeleteCollectionRequestAlpha1.newBuilder()
              .setStoreName(storeName)
              .setCollection(collection);
      putMetadata(builder::putAllMetadata, metadata);
      DaprVectorProtos.DeleteCollectionRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<Empty>createMono(
              it -> grpcCalls.intercept(context).deleteCollectionAlpha1(protoRequest, it)
          )
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<UpsertVectorsResponseAlpha1> upsertVectors(UpsertVectorsRequestAlpha1 request) {
    try {
      requireRequest(request);
      validateCollection(request.getStoreName(), request.getCollection());
      if (request.getRecords() == null) {
        throw new IllegalArgumentException("Records cannot be null.");
      }

      DaprVectorProtos.UpsertVectorsRequestAlpha1.Builder builder =
          DaprVectorProtos.UpsertVectorsRequestAlpha1.newBuilder()
              .setStoreName(request.getStoreName())
              .setCollection(request.getCollection());
      for (VectorRecord record : request.getRecords()) {
        builder.addRecords(toProto(record));
      }
      putMetadata(builder::putAllMetadata, request.getMetadata());
      if (request.getOptions() != null) {
        builder.setOptions(toProto(request.getOptions()));
      }
      DaprVectorProtos.UpsertVectorsRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprVectorProtos.UpsertVectorsResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).upsertVectorsAlpha1(protoRequest, it)
          )
      ).map(response -> new UpsertVectorsResponseAlpha1(
          fromProto(response.getAck()), fromProto(response.getFailedItemsList())));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<List<VectorRecord>> getVectors(GetVectorsRequestAlpha1 request) {
    try {
      requireRequest(request);
      validateCollection(request.getStoreName(), request.getCollection());
      validateIds(request.getIds());

      DaprVectorProtos.GetVectorsRequestAlpha1.Builder builder =
          DaprVectorProtos.GetVectorsRequestAlpha1.newBuilder()
              .setStoreName(request.getStoreName())
              .setCollection(request.getCollection())
              .addAllIds(request.getIds());
      if (request.getIncludeValues() != null) {
        builder.setIncludeValues(request.getIncludeValues());
      }
      putMetadata(builder::putAllMetadata, request.getMetadata());
      DaprVectorProtos.GetVectorsRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprVectorProtos.GetVectorsResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).getVectorsAlpha1(protoRequest, it)
          )
      ).map(response -> {
        List<VectorRecord> records = new ArrayList<>();
        for (DaprVectorProtos.VectorRecord record : response.getRecordsList()) {
          records.add(fromProto(record));
        }
        return List.copyOf(records);
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<IndexAck> deleteVectors(DeleteVectorsRequestAlpha1 request) {
    try {
      requireRequest(request);
      validateCollection(request.getStoreName(), request.getCollection());
      validateIds(request.getIds());

      DaprVectorProtos.DeleteVectorsRequestAlpha1.Builder builder =
          DaprVectorProtos.DeleteVectorsRequestAlpha1.newBuilder()
              .setStoreName(request.getStoreName())
              .setCollection(request.getCollection())
              .addAllIds(request.getIds());
      putMetadata(builder::putAllMetadata, request.getMetadata());
      if (request.getOptions() != null) {
        builder.setOptions(toProto(request.getOptions()));
      }
      DaprVectorProtos.DeleteVectorsRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprVectorProtos.DeleteVectorsResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).deleteVectorsAlpha1(protoRequest, it)
          )
      ).map(response -> fromProto(response.getAck()));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<QueryVectorsResponseAlpha1> queryVectors(QueryVectorsRequestAlpha1 request) {
    try {
      requireRequest(request);
      validateCollection(request.getStoreName(), request.getCollection());

      DaprVectorProtos.QueryVectorsRequestAlpha1.Builder builder =
          DaprVectorProtos.QueryVectorsRequestAlpha1.newBuilder()
              .setStoreName(request.getStoreName())
              .setCollection(request.getCollection())
              .setQuery(toProto(request.getQuery()));
      putMetadata(builder::putAllMetadata, request.getMetadata());
      DaprVectorProtos.QueryVectorsRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprVectorProtos.QueryVectorsResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).queryVectorsAlpha1(protoRequest, it)
          )
      ).map(DaprVectorClient::fromProto);
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<List<BatchQueryVectorsResultAlpha1>> batchQueryVectors(BatchQueryVectorsRequestAlpha1 request) {
    try {
      requireRequest(request);
      validateCollection(request.getStoreName(), request.getCollection());
      if (request.getQueries() == null) {
        throw new IllegalArgumentException("Queries cannot be null.");
      }

      DaprVectorProtos.BatchQueryVectorsRequestAlpha1.Builder builder =
          DaprVectorProtos.BatchQueryVectorsRequestAlpha1.newBuilder()
              .setStoreName(request.getStoreName())
              .setCollection(request.getCollection());
      for (VectorQuery query : request.getQueries()) {
        builder.addQueries(toProto(query));
      }
      putMetadata(builder::putAllMetadata, request.getMetadata());
      DaprVectorProtos.BatchQueryVectorsRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprVectorProtos.BatchQueryVectorsResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).batchQueryVectorsAlpha1(protoRequest, it)
          )
      ).map(response -> fromProto(response, protoRequest.getQueriesCount()));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private static void requireRequest(Object request) {
    if (request == null) {
      throw new IllegalArgumentException("Request cannot be null.");
    }
  }

  private static void validateStoreName(String storeName) {
    if (isBlank(storeName)) {
      throw new IllegalArgumentException("Store name cannot be null or empty.");
    }
  }

  private static void validateCollection(String storeName, String collection) {
    validateStoreName(storeName);
    if (isBlank(collection)) {
      throw new IllegalArgumentException("Collection name cannot be null or empty.");
    }
  }

  private static void validateIds(List<String> ids) {
    if (ids == null) {
      throw new IllegalArgumentException("IDs cannot be null.");
    }
    for (String id : ids) {
      if (isBlank(id)) {
        throw new IllegalArgumentException("IDs cannot contain a null or empty ID.");
      }
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static void putMetadata(Consumer<Map<String, String>> target, Map<String, String> metadata) {
    if (metadata != null) {
      target.accept(metadata);
    }
  }

  private static DaprVectorProtos.VectorRecord toProto(VectorRecord record) {
    if (record == null) {
      throw new IllegalArgumentException("Records cannot contain a null record.");
    }
    if (isBlank(record.getId())) {
      throw new IllegalArgumentException("Record ID cannot be null or empty.");
    }
    if (record.getValues() == null) {
      throw new IllegalArgumentException("Record values cannot be null.");
    }

    DaprVectorProtos.VectorRecord.Builder builder = DaprVectorProtos.VectorRecord.newBuilder()
        .setId(record.getId());
    addValues(builder, record.getValues());
    if (record.getPayload() != null) {
      builder.setPayload(ByteString.copyFrom(record.getPayload()));
    }
    if (record.getMetadata() != null) {
      builder.setMetadata(toStruct(record.getMetadata()));
    }
    return builder.build();
  }

  private static DaprVectorProtos.VectorQuery toProto(VectorQuery query) {
    if (query == null) {
      throw new IllegalArgumentException("Query cannot be null.");
    }
    if ((query.getVector() == null) == (query.getById() == null)) {
      throw new IllegalArgumentException("A query needs exactly one of a vector or a record ID.");
    }
    if (query.getTopK() != null && query.getTopK() < 0) {
      throw new IllegalArgumentException("Top K cannot be negative.");
    }

    DaprVectorProtos.VectorQuery.Builder builder = DaprVectorProtos.VectorQuery.newBuilder();
    if (query.getVector() != null) {
      if (query.getVector().length == 0) {
        throw new IllegalArgumentException("The query vector cannot be empty.");
      }
      DaprVectorProtos.VectorRecord.Builder vector = DaprVectorProtos.VectorRecord.newBuilder();
      addValues(vector, query.getVector());
      builder.setVector(vector);
    } else {
      if (isBlank(query.getById())) {
        throw new IllegalArgumentException("The query record ID cannot be empty.");
      }
      builder.setById(query.getById());
    }
    if (query.getTopK() != null) {
      builder.setTopK(query.getTopK());
    }
    if (query.getFilter() != null) {
      builder.setFilter(toStruct(query.getFilter()));
    }
    if (query.getIncludeValues() != null) {
      builder.setIncludeValues(query.getIncludeValues());
    }
    if (query.getIncludePayload() != null) {
      builder.setIncludePayload(query.getIncludePayload());
    }
    builder.setMetric(toProto(query.getMetric()));
    if (query.getScoreThreshold() != null) {
      builder.setScoreThreshold(query.getScoreThreshold());
    }
    return builder.build();
  }

  private static DaprSearchProtos.IndexingOptionsAlpha1 toProto(IndexingOptions options) {
    if (!options.isWaitForCompletion()) {
      return DaprSearchProtos.IndexingOptionsAlpha1.newBuilder()
          .setMode(DaprSearchProtos.IndexingMode.INDEXING_MODE_RETURN_ON_ACCEPTANCE)
          .build();
    }

    Duration waitTimeout = options.getWaitTimeout();
    if (waitTimeout == null || waitTimeout.isZero() || waitTimeout.isNegative()) {
      throw new IllegalArgumentException("Waiting for completion needs a positive wait timeout.");
    }
    if (options.getOnWaitTimeout() == null) {
      throw new IllegalArgumentException("Waiting for completion needs an action for when the wait times out.");
    }
    return DaprSearchProtos.IndexingOptionsAlpha1.newBuilder()
        .setMode(DaprSearchProtos.IndexingMode.INDEXING_MODE_WAIT_FOR_COMPLETION)
        .setWaitTimeout(com.google.protobuf.Duration.newBuilder()
            .setSeconds(waitTimeout.getSeconds())
            .setNanos(waitTimeout.getNano()))
        .setOnWaitTimeout(toProto(options.getOnWaitTimeout()))
        .build();
  }

  private static DaprSearchProtos.IndexingWaitTimeoutAction toProto(IndexingWaitTimeoutAction action) {
    switch (action) {
      case CONTINUE_ASYNC:
        return DaprSearchProtos.IndexingWaitTimeoutAction.INDEXING_WAIT_TIMEOUT_ACTION_CONTINUE_ASYNC;
      case FAIL_REQUEST:
        return DaprSearchProtos.IndexingWaitTimeoutAction.INDEXING_WAIT_TIMEOUT_ACTION_FAIL_REQUEST;
      default:
        throw new IllegalArgumentException("Unknown wait timeout action: " + action);
    }
  }

  private static DaprVectorProtos.DistanceMetric toProto(DistanceMetric metric) {
    if (metric == null) {
      return DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_UNSPECIFIED;
    }
    switch (metric) {
      case COSINE:
        return DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_COSINE;
      case DOT_PRODUCT:
        return DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_DOT_PRODUCT;
      case EUCLIDEAN:
        return DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_EUCLIDEAN;
      default:
        return DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_UNSPECIFIED;
    }
  }

  private static DistanceMetric fromProto(DaprVectorProtos.DistanceMetric metric) {
    switch (metric) {
      case DISTANCE_METRIC_COSINE:
        return DistanceMetric.COSINE;
      case DISTANCE_METRIC_DOT_PRODUCT:
        return DistanceMetric.DOT_PRODUCT;
      case DISTANCE_METRIC_EUCLIDEAN:
        return DistanceMetric.EUCLIDEAN;
      default:
        throw invalidResponse("Vector store returned an invalid distance metric: " + metric + ".");
    }
  }

  private static IndexAck fromProto(DaprSearchProtos.IndexAck ack) {
    switch (ack) {
      case INDEX_ACK_QUEUED:
        return IndexAck.QUEUED;
      case INDEX_ACK_COMPLETED:
        return IndexAck.COMPLETED;
      default:
        throw invalidResponse("Vector store returned an invalid write acknowledgement: " + ack + ".");
    }
  }

  private static List<FailedItem> fromProto(List<DaprSearchProtos.FailedItem> failedItems) {
    List<FailedItem> items = new ArrayList<>();
    for (DaprSearchProtos.FailedItem failedItem : failedItems) {
      items.add(new FailedItem(failedItem.getId(), toDaprException(failedItem.getError())));
    }
    return items;
  }

  private static VectorRecord fromProto(DaprVectorProtos.VectorRecord record) {
    float[] values = new float[record.getValuesCount()];
    for (int i = 0; i < values.length; i++) {
      values[i] = record.getValues(i);
    }
    VectorRecord vectorRecord = new VectorRecord(record.getId(), values);
    if (record.hasPayload()) {
      vectorRecord.setPayload(record.getPayload().toByteArray());
    }
    if (record.hasMetadata()) {
      vectorRecord.setMetadata(ProtobufValueHelper.fromProtobufStruct(record.getMetadata()));
    }
    return vectorRecord;
  }

  private static QueryVectorsResponseAlpha1 fromProto(DaprVectorProtos.QueryVectorsResponseAlpha1 response) {
    List<VectorMatch> matches = new ArrayList<>();
    for (DaprVectorProtos.VectorMatch match : response.getMatchesList()) {
      if (!match.hasRecord()) {
        throw invalidResponse("Vector store returned a match without a record.");
      }
      matches.add(new VectorMatch(fromProto(match.getRecord()), match.getScore()));
    }
    return new QueryVectorsResponseAlpha1(matches, fromProto(response.getMetric()));
  }

  private static List<BatchQueryVectorsResultAlpha1> fromProto(
      DaprVectorProtos.BatchQueryVectorsResponseAlpha1 response, int queryCount) {
    // Results are matched to queries by position, so a reply of the wrong shape is rejected.
    if (response.getResultsCount() != queryCount) {
      throw invalidResponse(
          "Vector store returned " + response.getResultsCount() + " results for " + queryCount + " queries.");
    }

    List<BatchQueryVectorsResultAlpha1> results = new ArrayList<>();
    for (DaprVectorProtos.BatchQueryResultAlpha1 result : response.getResultsList()) {
      switch (result.getResultCase()) {
        case RESPONSE:
          results.add(BatchQueryVectorsResultAlpha1.ofResponse(fromProto(result.getResponse())));
          break;
        case ERROR:
          results.add(BatchQueryVectorsResultAlpha1.ofError(toDaprException(result.getError())));
          break;
        default:
          throw invalidResponse("Vector store returned a batch result with neither a response nor an error.");
      }
    }
    return List.copyOf(results);
  }

  private static DaprException toDaprException(Status status) {
    io.grpc.Status.Code code = io.grpc.Status.fromCodeValue(status.getCode()).getCode();
    if (code == io.grpc.Status.Code.OK) {
      throw invalidResponse("Vector store returned an error with an OK status code.");
    }
    return new DaprException(code.toString(), status.getMessage(), new DaprErrorDetails(status), status.toByteArray());
  }

  private static DaprException invalidResponse(String message) {
    return new DaprException(io.grpc.Status.Code.INTERNAL.toString(), message, (Throwable) null);
  }

  private static void addValues(DaprVectorProtos.VectorRecord.Builder builder, float[] values) {
    for (float value : values) {
      builder.addValues(value);
    }
  }

  // Unlike ProtobufValueHelper.toProtobufValue, this rejects unsupported types instead of sending their toString(),
  // which a filter would silently fail to match.
  private static Struct toStruct(Map<?, ?> map) {
    Struct.Builder builder = Struct.newBuilder();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String)) {
        throw new IllegalArgumentException("Filter and metadata keys must be strings.");
      }
      builder.putFields((String) entry.getKey(), toValue(entry.getValue()));
    }
    return builder.build();
  }

  private static Value toValue(Object value) {
    Value.Builder builder = Value.newBuilder();
    if (value == null) {
      return builder.setNullValue(NullValue.NULL_VALUE).build();
    }
    if (value instanceof Boolean) {
      return builder.setBoolValue((Boolean) value).build();
    }
    if (value instanceof Number) {
      return builder.setNumberValue(((Number) value).doubleValue()).build();
    }
    if (value instanceof String) {
      return builder.setStringValue((String) value).build();
    }
    if (value instanceof Collection) {
      ListValue.Builder list = ListValue.newBuilder();
      for (Object item : (Collection<?>) value) {
        list.addValues(toValue(item));
      }
      return builder.setListValue(list).build();
    }
    if (value instanceof Map) {
      return builder.setStructValue(toStruct((Map<?, ?>) value)).build();
    }
    throw new IllegalArgumentException("Filter and metadata values must be null, a Boolean, a Number, a String, "
        + "a Collection or a Map, not " + value.getClass().getName() + ".");
  }
}
