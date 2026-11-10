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

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import io.dapr.client.domain.BatchQueryVectorsRequestAlpha1;
import io.dapr.client.domain.BatchQueryVectorsResultAlpha1;
import io.dapr.client.domain.CreateVectorCollectionRequestAlpha1;
import io.dapr.client.domain.DeleteVectorsRequestAlpha1;
import io.dapr.client.domain.DistanceMetric;
import io.dapr.client.domain.GetVectorCollectionResponseAlpha1;
import io.dapr.client.domain.GetVectorsRequestAlpha1;
import io.dapr.client.domain.IndexAck;
import io.dapr.client.domain.IndexingOptions;
import io.dapr.client.domain.IndexingWaitTimeoutAction;
import io.dapr.client.domain.QueryVectorsRequestAlpha1;
import io.dapr.client.domain.QueryVectorsResponseAlpha1;
import io.dapr.client.domain.UpsertVectorsRequestAlpha1;
import io.dapr.client.domain.UpsertVectorsResponseAlpha1;
import io.dapr.client.domain.VectorQuery;
import io.dapr.client.domain.VectorRecord;
import io.dapr.exceptions.DaprErrorDetails;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprSearchProtos;
import io.dapr.v1.DaprVectorProtos;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.dapr.client.DaprClientGrpcTest.newStatusRuntimeException;
import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DaprPreviewClientVectorTest {

  private static final String STORE = "vectorstore";

  private static final String COLLECTION = "books";

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private GrpcChannelFacade channel;
  private DaprGrpc.DaprStub daprStub;
  private DaprPreviewClient previewClient;

  @BeforeEach
  public void setup() throws IOException {
    channel = mock(GrpcChannelFacade.class);
    daprStub = mock(DaprGrpc.DaprStub.class);
    when(daprStub.withInterceptors(any())).thenReturn(daprStub);
    previewClient = new DaprClientImpl(
        channel, daprStub, mock(DaprHttp.class), new DefaultObjectSerializer(), new DefaultObjectSerializer());
    doNothing().when(channel).close();
  }

  @AfterEach
  public void tearDown() throws Exception {
    previewClient.close();
  }

  @SuppressWarnings("unchecked")
  private static <T> Answer<Void> respond(T response) {
    return invocation -> {
      StreamObserver<T> observer = (StreamObserver<T>) invocation.getArguments()[1];
      observer.onNext(response);
      observer.onCompleted();
      return null;
    };
  }

  private static void assertRejected(Supplier<Mono<?>> call) {
    assertThrows(IllegalArgumentException.class, () -> call.get().block(TIMEOUT));
  }

  @Test
  @DisplayName("createVectorCollection should send the names, dimensions, metric and metadata")
  public void createVectorCollectionTest() {
    ArgumentCaptor<DaprVectorProtos.CreateCollectionRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.CreateCollectionRequestAlpha1.class);
    doAnswer(respond(Empty.getDefaultInstance())).when(daprStub).createCollectionAlpha1(captor.capture(), any());

    previewClient.createVectorCollection(new CreateVectorCollectionRequestAlpha1(STORE, COLLECTION, 3)
        .setMetric(DistanceMetric.DOT_PRODUCT)
        .setMetadata(Map.of("shards", "2"))).block();

    DaprVectorProtos.CreateCollectionRequestAlpha1 request = captor.getValue();
    assertEquals(STORE, request.getStoreName());
    assertEquals(COLLECTION, request.getCollection());
    assertEquals(3, request.getDimensions());
    assertEquals(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_DOT_PRODUCT, request.getMetric());
    assertEquals(Map.of("shards", "2"), request.getMetadataMap());
  }

  @Test
  @DisplayName("createVectorCollection should default to the component's metric and no metadata")
  public void createVectorCollectionDefaultsTest() {
    ArgumentCaptor<DaprVectorProtos.CreateCollectionRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.CreateCollectionRequestAlpha1.class);
    doAnswer(respond(Empty.getDefaultInstance())).when(daprStub).createCollectionAlpha1(captor.capture(), any());

    previewClient.createVectorCollection(new CreateVectorCollectionRequestAlpha1(STORE, COLLECTION, 8)).block();

    assertEquals(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_UNSPECIFIED, captor.getValue().getMetric());
    assertTrue(captor.getValue().getMetadataMap().isEmpty());
  }

  @Test
  @DisplayName("createVectorCollection should reject a missing request, blank names and non-positive dimensions")
  public void createVectorCollectionInvalidArgumentsTest() {
    assertRejected(() -> previewClient.createVectorCollection(null));
    assertRejected(() -> previewClient.createVectorCollection(
        new CreateVectorCollectionRequestAlpha1(" ", COLLECTION, 3)));
    assertRejected(() -> previewClient.createVectorCollection(
        new CreateVectorCollectionRequestAlpha1(STORE, null, 3)));
    assertRejected(() -> previewClient.createVectorCollection(
        new CreateVectorCollectionRequestAlpha1(STORE, COLLECTION, 0)));
    assertRejected(() -> previewClient.createVectorCollection(
        new CreateVectorCollectionRequestAlpha1(STORE, COLLECTION, -1)));
    verify(daprStub, never()).createCollectionAlpha1(any(), any());
  }

  @Test
  @DisplayName("getVectorCollection should send the metadata and map the collection")
  public void getVectorCollectionTest() {
    ArgumentCaptor<DaprVectorProtos.GetCollectionRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.GetCollectionRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.GetCollectionResponseAlpha1.newBuilder()
        .setCollection(COLLECTION)
        .setRecordCount(42)
        .putProperties("engine", "hnsw")
        .setDimensions(3)
        .setMetric(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_EUCLIDEAN)
        .build())).when(daprStub).getCollectionAlpha1(captor.capture(), any());

    GetVectorCollectionResponseAlpha1 collection =
        previewClient.getVectorCollection(STORE, COLLECTION, Map.of("consistency", "strong")).block();

    assertEquals(STORE, captor.getValue().getStoreName());
    assertEquals(COLLECTION, captor.getValue().getCollection());
    assertEquals(Map.of("consistency", "strong"), captor.getValue().getMetadataMap());
    assertEquals(COLLECTION, collection.getCollection());
    assertEquals(42, collection.getRecordCount());
    assertEquals(Map.of("engine", "hnsw"), collection.getProperties());
    assertEquals(3, collection.getDimensions());
    assertEquals(DistanceMetric.EUCLIDEAN, collection.getMetric());
  }

  @Test
  @DisplayName("getVectorCollection without metadata should send none")
  public void getVectorCollectionWithoutMetadataTest() {
    ArgumentCaptor<DaprVectorProtos.GetCollectionRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.GetCollectionRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.GetCollectionResponseAlpha1.newBuilder()
        .setMetric(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_COSINE)
        .build())).when(daprStub).getCollectionAlpha1(captor.capture(), any());

    GetVectorCollectionResponseAlpha1 collection = previewClient.getVectorCollection(STORE, COLLECTION).block();

    assertTrue(captor.getValue().getMetadataMap().isEmpty());
    assertEquals(DistanceMetric.COSINE, collection.getMetric());
  }

  @Test
  @DisplayName("every vector call should surface the sidecar's error code")
  public void sidecarErrorTest() {
    Answer<Void> notFound = invocation -> {
      StreamObserver<?> observer = (StreamObserver<?>) invocation.getArguments()[1];
      observer.onError(newStatusRuntimeException("NOT_FOUND", "collection books not found"));
      return null;
    };
    doAnswer(notFound).when(daprStub).createCollectionAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).getCollectionAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).listCollectionsAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).deleteCollectionAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).upsertVectorsAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).getVectorsAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).deleteVectorsAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).queryVectorsAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).batchQueryVectorsAlpha1(any(), any());
    List<VectorRecord> records = List.of(new VectorRecord("a", new float[] {1f}));
    List<Supplier<Mono<?>>> calls = List.of(
        () -> previewClient.createVectorCollection(new CreateVectorCollectionRequestAlpha1(STORE, COLLECTION, 3)),
        () -> previewClient.getVectorCollection(STORE, COLLECTION),
        () -> previewClient.listVectorCollections(STORE),
        () -> previewClient.deleteVectorCollection(STORE, COLLECTION),
        () -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(STORE, COLLECTION, records)),
        () -> previewClient.getVectors(new GetVectorsRequestAlpha1(STORE, COLLECTION, List.of("a"))),
        () -> previewClient.deleteVectors(new DeleteVectorsRequestAlpha1(STORE, COLLECTION, List.of("a"))),
        () -> previewClient.queryVectors(new QueryVectorsRequestAlpha1(STORE, COLLECTION, VectorQuery.byId("a"))),
        () -> previewClient.batchQueryVectors(new BatchQueryVectorsRequestAlpha1(
            STORE, COLLECTION, List.of(VectorQuery.byId("a")))));

    for (Supplier<Mono<?>> call : calls) {
      DaprException exception = assertThrows(DaprException.class, () -> call.get().block(TIMEOUT));
      assertEquals("NOT_FOUND", exception.getErrorCode());
    }
  }

  @Test
  @DisplayName("responses with an unspecified or unknown distance metric should fail")
  public void invalidMetricResponseTest() {
    doAnswer(respond(DaprVectorProtos.GetCollectionResponseAlpha1.getDefaultInstance()))
        .when(daprStub).getCollectionAlpha1(any(), any());
    doAnswer(respond(DaprVectorProtos.QueryVectorsResponseAlpha1.newBuilder().setMetricValue(42).build()))
        .when(daprStub).queryVectorsAlpha1(any(), any());

    assertThrowsDaprException("INTERNAL", () -> previewClient.getVectorCollection(STORE, COLLECTION).block(TIMEOUT));
    assertThrowsDaprException("INTERNAL", () -> previewClient.queryVectors(
        new QueryVectorsRequestAlpha1(STORE, COLLECTION, VectorQuery.byId("a"))).block(TIMEOUT));
  }

  @Test
  @DisplayName("write responses with an unspecified or unknown acknowledgement should fail")
  public void invalidAckResponseTest() {
    doAnswer(respond(DaprVectorProtos.UpsertVectorsResponseAlpha1.getDefaultInstance()))
        .when(daprStub).upsertVectorsAlpha1(any(), any());
    doAnswer(respond(DaprVectorProtos.DeleteVectorsResponseAlpha1.newBuilder().setAckValue(9).build()))
        .when(daprStub).deleteVectorsAlpha1(any(), any());

    assertThrowsDaprException("INTERNAL", () -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(
        STORE, COLLECTION, List.of(new VectorRecord("a", new float[] {1f})))).block(TIMEOUT));
    assertThrowsDaprException("INTERNAL", () -> previewClient.deleteVectors(
        new DeleteVectorsRequestAlpha1(STORE, COLLECTION, List.of("a"))).block(TIMEOUT));
  }

  @Test
  @DisplayName("listVectorCollections should send the metadata and return the names")
  public void listVectorCollectionsTest() {
    ArgumentCaptor<DaprVectorProtos.ListCollectionsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.ListCollectionsRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.ListCollectionsResponseAlpha1.newBuilder()
        .addCollections("books")
        .addCollections("films")
        .build())).when(daprStub).listCollectionsAlpha1(captor.capture(), any());

    List<String> names = previewClient.listVectorCollections(STORE, Map.of("page", "1")).block();

    assertEquals(List.of("books", "films"), names);
    assertEquals(STORE, captor.getValue().getStoreName());
    assertEquals(Map.of("page", "1"), captor.getValue().getMetadataMap());
  }

  @Test
  @DisplayName("listVectorCollections without metadata should send none")
  public void listVectorCollectionsWithoutMetadataTest() {
    ArgumentCaptor<DaprVectorProtos.ListCollectionsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.ListCollectionsRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.ListCollectionsResponseAlpha1.getDefaultInstance()))
        .when(daprStub).listCollectionsAlpha1(captor.capture(), any());

    assertEquals(List.of(), previewClient.listVectorCollections(STORE).block());
    assertTrue(captor.getValue().getMetadataMap().isEmpty());
  }

  @Test
  @DisplayName("deleteVectorCollection should send the names and metadata")
  public void deleteVectorCollectionTest() {
    ArgumentCaptor<DaprVectorProtos.DeleteCollectionRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.DeleteCollectionRequestAlpha1.class);
    doAnswer(respond(Empty.getDefaultInstance())).when(daprStub).deleteCollectionAlpha1(captor.capture(), any());

    previewClient.deleteVectorCollection(STORE, COLLECTION, Map.of("force", "true")).block();
    previewClient.deleteVectorCollection(STORE, COLLECTION).block();

    List<DaprVectorProtos.DeleteCollectionRequestAlpha1> requests = captor.getAllValues();
    assertEquals(STORE, requests.get(0).getStoreName());
    assertEquals(COLLECTION, requests.get(0).getCollection());
    assertEquals(Map.of("force", "true"), requests.get(0).getMetadataMap());
    assertTrue(requests.get(1).getMetadataMap().isEmpty());
  }

  @Test
  @DisplayName("collection calls should reject blank store and collection names")
  public void collectionCallsInvalidNamesTest() {
    assertRejected(() -> previewClient.getVectorCollection(null, COLLECTION));
    assertRejected(() -> previewClient.getVectorCollection(STORE, ""));
    assertRejected(() -> previewClient.listVectorCollections(" "));
    assertRejected(() -> previewClient.listVectorCollections(null, Map.of()));
    assertRejected(() -> previewClient.deleteVectorCollection("", COLLECTION));
    assertRejected(() -> previewClient.deleteVectorCollection(STORE, " ", Map.of()));
    verify(daprStub, never()).getCollectionAlpha1(any(), any());
    verify(daprStub, never()).listCollectionsAlpha1(any(), any());
    verify(daprStub, never()).deleteCollectionAlpha1(any(), any());
  }

  @Test
  @DisplayName("upsertVectors should send the records with their payload, metadata and indexing options")
  public void upsertVectorsTest() {
    ArgumentCaptor<DaprVectorProtos.UpsertVectorsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.UpsertVectorsRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.UpsertVectorsResponseAlpha1.newBuilder()
        .setAck(DaprSearchProtos.IndexAck.INDEX_ACK_COMPLETED)
        .build())).when(daprStub).upsertVectorsAlpha1(captor.capture(), any());

    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("genre", "fantasy");
    metadata.put("year", 1954);
    metadata.put("tags", List.of("classic", true));
    metadata.put("missing", null);
    List<VectorRecord> records = List.of(
        new VectorRecord("lotr", new float[] {0.1f, 0.2f, 0.3f})
            .setPayload("payload".getBytes())
            .setMetadata(metadata),
        new VectorRecord("dune", new float[] {0.4f, 0.5f, 0.6f}));

    UpsertVectorsResponseAlpha1 response = previewClient.upsertVectors(
        new UpsertVectorsRequestAlpha1(STORE, COLLECTION, records)
            .setMetadata(Map.of("ttl", "60"))
            .setOptions(IndexingOptions.waitForCompletion(
                Duration.ofMillis(1500), IndexingWaitTimeoutAction.FAIL_REQUEST))).block();

    DaprVectorProtos.UpsertVectorsRequestAlpha1 request = captor.getValue();
    assertEquals(STORE, request.getStoreName());
    assertEquals(COLLECTION, request.getCollection());
    assertEquals(Map.of("ttl", "60"), request.getMetadataMap());
    assertEquals(2, request.getRecordsCount());

    DaprVectorProtos.VectorRecord first = request.getRecords(0);
    assertEquals("lotr", first.getId());
    assertEquals(List.of(0.1f, 0.2f, 0.3f), first.getValuesList());
    assertEquals(ByteString.copyFromUtf8("payload"), first.getPayload());
    Struct struct = first.getMetadata();
    assertEquals("fantasy", struct.getFieldsOrThrow("genre").getStringValue());
    assertEquals(1954, struct.getFieldsOrThrow("year").getNumberValue());
    assertEquals(2, struct.getFieldsOrThrow("tags").getListValue().getValuesCount());
    assertEquals(NullValue.NULL_VALUE, struct.getFieldsOrThrow("missing").getNullValue());

    DaprVectorProtos.VectorRecord second = request.getRecords(1);
    assertFalse(second.hasPayload());
    assertFalse(second.hasMetadata());

    DaprSearchProtos.IndexingOptionsAlpha1 options = request.getOptions();
    assertEquals(DaprSearchProtos.IndexingMode.INDEXING_MODE_WAIT_FOR_COMPLETION, options.getMode());
    assertEquals(1, options.getWaitTimeout().getSeconds());
    assertEquals(500_000_000, options.getWaitTimeout().getNanos());
    assertEquals(DaprSearchProtos.IndexingWaitTimeoutAction.INDEXING_WAIT_TIMEOUT_ACTION_FAIL_REQUEST,
        options.getOnWaitTimeout());

    assertEquals(IndexAck.COMPLETED, response.getAck());
    assertTrue(response.getFailedItems().isEmpty());
  }

  @Test
  @DisplayName("upsertVectors should send return-on-acceptance options and none when they are not set")
  public void upsertVectorsIndexingModesTest() {
    ArgumentCaptor<DaprVectorProtos.UpsertVectorsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.UpsertVectorsRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.UpsertVectorsResponseAlpha1.newBuilder()
        .setAck(DaprSearchProtos.IndexAck.INDEX_ACK_QUEUED)
        .build())).when(daprStub).upsertVectorsAlpha1(captor.capture(), any());
    List<VectorRecord> records = List.of(new VectorRecord("a", new float[] {1f}));

    previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(STORE, COLLECTION, records)
        .setOptions(IndexingOptions.returnOnAcceptance())).block();
    previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(STORE, COLLECTION, records)).block();

    assertEquals(DaprSearchProtos.IndexingMode.INDEXING_MODE_RETURN_ON_ACCEPTANCE,
        captor.getAllValues().get(0).getOptions().getMode());
    assertFalse(captor.getAllValues().get(0).getOptions().hasWaitTimeout());
    assertFalse(captor.getAllValues().get(1).hasOptions());
  }

  @Test
  @DisplayName("upsertVectors should report records the store rejected as failed items with their error")
  public void upsertVectorsFailedItemsTest() {
    doAnswer(respond(DaprVectorProtos.UpsertVectorsResponseAlpha1.newBuilder()
        .setAck(DaprSearchProtos.IndexAck.INDEX_ACK_COMPLETED)
        .addFailedItems(DaprSearchProtos.FailedItem.newBuilder()
            .setId("dune")
            .setError(Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage("expected 3 dimensions, got 2")
                .addDetails(Any.pack(ErrorInfo.newBuilder().setReason("DIMENSION_MISMATCH").build()))))
        .build())).when(daprStub).upsertVectorsAlpha1(any(), any());

    UpsertVectorsResponseAlpha1 response = previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(
        STORE, COLLECTION, List.of(new VectorRecord("dune", new float[] {1f, 2f})))).block();

    assertEquals(1, response.getFailedItems().size());
    assertEquals("dune", response.getFailedItems().get(0).getId());
    DaprException error = response.getFailedItems().get(0).getError();
    assertEquals("INVALID_ARGUMENT", error.getErrorCode());
    assertEquals("INVALID_ARGUMENT: expected 3 dimensions, got 2", error.getMessage());
    assertEquals("DIMENSION_MISMATCH",
        error.getErrorDetails().get(DaprErrorDetails.ErrorDetailType.ERROR_INFO, "reason", TypeRef.STRING));
  }

  @Test
  @DisplayName("upsertVectors should fail on a failed item with an OK status code")
  public void upsertVectorsOkFailedItemTest() {
    doAnswer(respond(DaprVectorProtos.UpsertVectorsResponseAlpha1.newBuilder()
        .setAck(DaprSearchProtos.IndexAck.INDEX_ACK_COMPLETED)
        .addFailedItems(DaprSearchProtos.FailedItem.newBuilder().setId("dune"))
        .build())).when(daprStub).upsertVectorsAlpha1(any(), any());

    assertThrowsDaprException("INTERNAL", () -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(
        STORE, COLLECTION, List.of(new VectorRecord("dune", new float[] {1f})))).block(TIMEOUT));
  }

  @Test
  @DisplayName("upsertVectors and queryVectors should reject metadata and filter values of unsupported types")
  public void unsupportedStructValuesTest() {
    Map<String, Object> nonStringKey = new LinkedHashMap<>();
    nonStringKey.put("genre", Map.of(1, "fantasy"));
    assertRejected(() -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(STORE, COLLECTION, List.of(
        new VectorRecord("a", new float[] {1f}).setMetadata(Map.of("tags", new String[] {"classic"}))))));
    assertRejected(() -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(STORE, COLLECTION, List.of(
        new VectorRecord("a", new float[] {1f}).setMetadata(nonStringKey)))));
    assertRejected(() -> previewClient.queryVectors(new QueryVectorsRequestAlpha1(STORE, COLLECTION,
        VectorQuery.byId("a").setFilter(Map.of("published", Duration.ofDays(1))))));
    verify(daprStub, never()).upsertVectorsAlpha1(any(), any());
    verify(daprStub, never()).queryVectorsAlpha1(any(), any());
  }

  @Test
  @DisplayName("filters and metadata should send numbers as doubles without changing them")
  public void structNumbersTest() {
    ArgumentCaptor<DaprVectorProtos.QueryVectorsRequestAlpha1> query =
        ArgumentCaptor.forClass(DaprVectorProtos.QueryVectorsRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.QueryVectorsResponseAlpha1.newBuilder()
        .setMetric(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_COSINE)
        .build())).when(daprStub).queryVectorsAlpha1(query.capture(), any());
    ArgumentCaptor<DaprVectorProtos.UpsertVectorsRequestAlpha1> upsert =
        ArgumentCaptor.forClass(DaprVectorProtos.UpsertVectorsRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.UpsertVectorsResponseAlpha1.newBuilder()
        .setAck(DaprSearchProtos.IndexAck.INDEX_ACK_COMPLETED)
        .build())).when(daprStub).upsertVectorsAlpha1(upsert.capture(), any());
    Map<String, Object> numbers = Map.of(
        "int", 5,
        "long", 1L << 53,
        "float", 1.1f,
        "double", 2.5d,
        "decimal", new BigDecimal("1.10"),
        "integer", BigInteger.valueOf(1965),
        "power", 1L << 60,
        "min", Long.MIN_VALUE,
        "bigPower", BigInteger.TWO.pow(70),
        "nested", Map.of("$in", List.of(1.1f)));

    previewClient.queryVectors(new QueryVectorsRequestAlpha1(
        STORE, COLLECTION, VectorQuery.byId("a").setFilter(numbers))).block();
    previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(STORE, COLLECTION, List.of(
        new VectorRecord("a", new float[] {1f}).setMetadata(numbers)))).block();

    assertSentNumbers(query.getValue().getQuery().getFilter());
    assertSentNumbers(upsert.getValue().getRecords(0).getMetadata());
  }

  private static void assertSentNumbers(Struct sent) {
    Map<String, Value> fields = sent.getFieldsMap();
    assertEquals(5d, fields.get("int").getNumberValue());
    assertEquals(0x1p53, fields.get("long").getNumberValue());
    assertEquals(1.1d, fields.get("float").getNumberValue());
    assertEquals(2.5d, fields.get("double").getNumberValue());
    assertEquals(1.1d, fields.get("decimal").getNumberValue());
    assertEquals(1965d, fields.get("integer").getNumberValue());
    assertEquals(0x1p60, fields.get("power").getNumberValue());
    assertEquals(-0x1p63, fields.get("min").getNumberValue());
    assertEquals(0x1p70, fields.get("bigPower").getNumberValue());
    assertEquals(1.1d, fields.get("nested").getStructValue().getFieldsOrThrow("$in").getListValue()
        .getValues(0).getNumberValue());
  }

  @Test
  @DisplayName("filters and metadata should reject numbers that a double cannot hold exactly")
  public void inexactStructNumbersTest() {
    List<Number> numbers = List.of(
        (1L << 53) + 1,
        Long.MAX_VALUE,
        new BigDecimal("0.1000000000000000055"),
        BigInteger.TEN.pow(400));
    for (Number number : numbers) {
      assertInexact(() -> previewClient.queryVectors(new QueryVectorsRequestAlpha1(STORE, COLLECTION,
          VectorQuery.byId("a").setFilter(Map.of("id", number)))));
      assertInexact(() -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(STORE, COLLECTION, List.of(
          new VectorRecord("a", new float[] {1f}).setMetadata(Map.of("id", number))))));
    }
    assertInexact(() -> previewClient.queryVectors(new QueryVectorsRequestAlpha1(STORE, COLLECTION,
        VectorQuery.byId("a").setFilter(Map.of("id", Map.of("$in", List.of((1L << 53) + 1)))))));
    verify(daprStub, never()).upsertVectorsAlpha1(any(), any());
    verify(daprStub, never()).queryVectorsAlpha1(any(), any());
  }

  private static void assertInexact(Supplier<Mono<?>> call) {
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> call.get().block(TIMEOUT));
    assertTrue(error.getMessage().contains("cannot be represented exactly"), error.getMessage());
  }

  @Test
  @DisplayName("upsertVectors should reject missing records, records without an ID or vector, and bad options")
  public void upsertVectorsInvalidArgumentsTest() {
    List<VectorRecord> records = List.of(new VectorRecord("a", new float[] {1f}));
    assertRejected(() -> previewClient.upsertVectors(null));
    assertRejected(() -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(null, COLLECTION, records)));
    assertRejected(() -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(STORE, COLLECTION, null)));
    assertRejected(() -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(
        STORE, COLLECTION, Collections.singletonList(null))));
    assertRejected(() -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(
        STORE, COLLECTION, List.of(new VectorRecord(" ", new float[] {1f})))));
    assertRejected(() -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(
        STORE, COLLECTION, List.of(new VectorRecord("a", null)))));
    assertRejected(() -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(STORE, COLLECTION, records)
        .setOptions(IndexingOptions.waitForCompletion(Duration.ZERO, IndexingWaitTimeoutAction.CONTINUE_ASYNC))));
    assertRejected(() -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(STORE, COLLECTION, records)
        .setOptions(IndexingOptions.waitForCompletion(null, IndexingWaitTimeoutAction.CONTINUE_ASYNC))));
    assertRejected(() -> previewClient.upsertVectors(new UpsertVectorsRequestAlpha1(STORE, COLLECTION, records)
        .setOptions(IndexingOptions.waitForCompletion(Duration.ofSeconds(1), null))));
    verify(daprStub, never()).upsertVectorsAlpha1(any(), any());
  }

  @Test
  @DisplayName("getVectors should send the IDs and map the records back")
  public void getVectorsTest() {
    ArgumentCaptor<DaprVectorProtos.GetVectorsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.GetVectorsRequestAlpha1.class);
    Struct metadata = Struct.newBuilder()
        .putFields("year", Value.newBuilder().setNumberValue(1965).build())
        .putFields("awards", Value.newBuilder().setListValue(ListValue.newBuilder()
            .addValues(Value.newBuilder().setStringValue("Hugo"))
            .addValues(Value.newBuilder().setBoolValue(true))).build())
        .build();
    doAnswer(respond(DaprVectorProtos.GetVectorsResponseAlpha1.newBuilder()
        .addRecords(DaprVectorProtos.VectorRecord.newBuilder()
            .setId("dune")
            .addValues(0.4f)
            .addValues(0.5f)
            .setPayload(ByteString.copyFromUtf8("payload"))
            .setMetadata(metadata))
        .addRecords(DaprVectorProtos.VectorRecord.newBuilder().setId("lotr"))
        .build())).when(daprStub).getVectorsAlpha1(captor.capture(), any());

    List<VectorRecord> records = previewClient.getVectors(
        new GetVectorsRequestAlpha1(STORE, COLLECTION, List.of("dune", "lotr", "unknown"))
            .setIncludeValues(true)
            .setMetadata(Map.of("k", "v"))).block();

    DaprVectorProtos.GetVectorsRequestAlpha1 request = captor.getValue();
    assertEquals(List.of("dune", "lotr", "unknown"), request.getIdsList());
    assertTrue(request.hasIncludeValues());
    assertTrue(request.getIncludeValues());
    assertEquals(Map.of("k", "v"), request.getMetadataMap());

    assertEquals(2, records.size());
    VectorRecord dune = records.get(0);
    assertEquals("dune", dune.getId());
    assertArrayEquals(new float[] {0.4f, 0.5f}, dune.getValues());
    assertArrayEquals("payload".getBytes(), dune.getPayload());
    assertEquals(1965.0, dune.getMetadata().get("year"));
    assertEquals(Arrays.asList("Hugo", true), dune.getMetadata().get("awards"));

    VectorRecord lotr = records.get(1);
    assertArrayEquals(new float[0], lotr.getValues());
    assertNull(lotr.getPayload());
    assertNull(lotr.getMetadata());
  }

  @Test
  @DisplayName("getVectors should leave include-values unset when it is not set")
  public void getVectorsIncludeValuesUnsetTest() {
    ArgumentCaptor<DaprVectorProtos.GetVectorsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.GetVectorsRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.GetVectorsResponseAlpha1.getDefaultInstance()))
        .when(daprStub).getVectorsAlpha1(captor.capture(), any());

    assertEquals(List.of(), previewClient.getVectors(
        new GetVectorsRequestAlpha1(STORE, COLLECTION, List.of("a"))).block());

    assertFalse(captor.getValue().hasIncludeValues());
  }

  @Test
  @DisplayName("getVectors and deleteVectors should reject missing or blank IDs")
  public void vectorIdsInvalidArgumentsTest() {
    List<String> withBlank = Arrays.asList("a", " ");
    List<String> withNull = Arrays.asList("a", null);
    assertRejected(() -> previewClient.getVectors(null));
    assertRejected(() -> previewClient.getVectors(new GetVectorsRequestAlpha1(STORE, COLLECTION, null)));
    assertRejected(() -> previewClient.getVectors(new GetVectorsRequestAlpha1(STORE, COLLECTION, withBlank)));
    assertRejected(() -> previewClient.getVectors(new GetVectorsRequestAlpha1(STORE, "", List.of("a"))));
    assertRejected(() -> previewClient.deleteVectors(null));
    assertRejected(() -> previewClient.deleteVectors(new DeleteVectorsRequestAlpha1(STORE, COLLECTION, null)));
    assertRejected(() -> previewClient.deleteVectors(new DeleteVectorsRequestAlpha1(STORE, COLLECTION, withNull)));
    verify(daprStub, never()).getVectorsAlpha1(any(), any());
    verify(daprStub, never()).deleteVectorsAlpha1(any(), any());
  }

  @Test
  @DisplayName("deleteVectors should send the IDs and indexing options and return the acknowledgement")
  public void deleteVectorsTest() {
    ArgumentCaptor<DaprVectorProtos.DeleteVectorsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.DeleteVectorsRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.DeleteVectorsResponseAlpha1.newBuilder()
        .setAck(DaprSearchProtos.IndexAck.INDEX_ACK_QUEUED)
        .build())).when(daprStub).deleteVectorsAlpha1(captor.capture(), any());

    IndexAck ack = previewClient.deleteVectors(new DeleteVectorsRequestAlpha1(STORE, COLLECTION, List.of("dune"))
        .setMetadata(Map.of("k", "v"))
        .setOptions(IndexingOptions.waitForCompletion(
            Duration.ofSeconds(2), IndexingWaitTimeoutAction.CONTINUE_ASYNC))).block();

    DaprVectorProtos.DeleteVectorsRequestAlpha1 request = captor.getValue();
    assertEquals(List.of("dune"), request.getIdsList());
    assertEquals(Map.of("k", "v"), request.getMetadataMap());
    assertEquals(2, request.getOptions().getWaitTimeout().getSeconds());
    assertEquals(DaprSearchProtos.IndexingWaitTimeoutAction.INDEXING_WAIT_TIMEOUT_ACTION_CONTINUE_ASYNC,
        request.getOptions().getOnWaitTimeout());
    assertEquals(IndexAck.QUEUED, ack);
  }

  @Test
  @DisplayName("queryVectors by vector should send every set option and map the matches")
  public void queryVectorsByVectorTest() {
    ArgumentCaptor<DaprVectorProtos.QueryVectorsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.QueryVectorsRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.QueryVectorsResponseAlpha1.newBuilder()
        .addMatches(DaprVectorProtos.VectorMatch.newBuilder()
            .setRecord(DaprVectorProtos.VectorRecord.newBuilder().setId("lotr").addValues(0.1f))
            .setScore(0.98))
        .addMatches(DaprVectorProtos.VectorMatch.newBuilder()
            .setRecord(DaprVectorProtos.VectorRecord.newBuilder().setId("dune"))
            .setScore(0.71))
        .setMetric(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_EUCLIDEAN)
        .build())).when(daprStub).queryVectorsAlpha1(captor.capture(), any());

    QueryVectorsResponseAlpha1 response = previewClient.queryVectors(new QueryVectorsRequestAlpha1(
        STORE, COLLECTION, VectorQuery.byVector(new float[] {0.1f, 0.2f})
            .setTopK(2)
            .setFilter(Map.of("genre", Map.of("$in", new LinkedHashSet<>(List.of("fantasy", "scifi")))))
            .setIncludeValues(true)
            .setIncludePayload(false)
            .setMetric(DistanceMetric.EUCLIDEAN)
            .setScoreThreshold(0.5))
        .setMetadata(Map.of("k", "v"))).block();

    DaprVectorProtos.QueryVectorsRequestAlpha1 request = captor.getValue();
    assertEquals(Map.of("k", "v"), request.getMetadataMap());
    DaprVectorProtos.VectorQuery query = request.getQuery();
    assertEquals(DaprVectorProtos.VectorQuery.KindCase.VECTOR, query.getKindCase());
    assertEquals(List.of(0.1f, 0.2f), query.getVector().getValuesList());
    assertEquals(2, query.getTopK());
    ListValue genres = query.getFilter().getFieldsOrThrow("genre").getStructValue()
        .getFieldsOrThrow("$in").getListValue();
    assertEquals(2, genres.getValuesCount());
    assertEquals("fantasy", genres.getValues(0).getStringValue());
    assertEquals("scifi", genres.getValues(1).getStringValue());
    assertTrue(query.getIncludeValues());
    assertTrue(query.hasIncludePayload());
    assertFalse(query.getIncludePayload());
    assertEquals(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_EUCLIDEAN, query.getMetric());
    assertEquals(0.5, query.getScoreThreshold());

    assertEquals(DistanceMetric.EUCLIDEAN, response.getMetric());
    assertEquals(2, response.getMatches().size());
    assertEquals("lotr", response.getMatches().get(0).getRecord().getId());
    assertArrayEquals(new float[] {0.1f}, response.getMatches().get(0).getRecord().getValues());
    assertEquals(0.98, response.getMatches().get(0).getScore());
    assertEquals("dune", response.getMatches().get(1).getRecord().getId());
  }

  @Test
  @DisplayName("queryVectors by ID should send only the ID and leave unset options unset")
  public void queryVectorsByIdTest() {
    ArgumentCaptor<DaprVectorProtos.QueryVectorsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.QueryVectorsRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.QueryVectorsResponseAlpha1.newBuilder()
        .setMetric(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_DOT_PRODUCT)
        .build())).when(daprStub).queryVectorsAlpha1(captor.capture(), any());

    QueryVectorsResponseAlpha1 response = previewClient.queryVectors(
        new QueryVectorsRequestAlpha1(STORE, COLLECTION, VectorQuery.byId("lotr"))).block();

    DaprVectorProtos.VectorQuery query = captor.getValue().getQuery();
    assertEquals(DaprVectorProtos.VectorQuery.KindCase.BY_ID, query.getKindCase());
    assertEquals("lotr", query.getById());
    assertFalse(query.hasTopK());
    assertFalse(query.hasFilter());
    assertFalse(query.hasIncludeValues());
    assertFalse(query.hasIncludePayload());
    assertFalse(query.hasScoreThreshold());
    assertEquals(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_UNSPECIFIED, query.getMetric());
    assertTrue(response.getMatches().isEmpty());
    assertEquals(DistanceMetric.DOT_PRODUCT, response.getMetric());
  }

  @Test
  @DisplayName("queryVectors should fail on a match without a record")
  public void queryVectorsMatchWithoutRecordTest() {
    doAnswer(respond(DaprVectorProtos.QueryVectorsResponseAlpha1.newBuilder()
        .addMatches(DaprVectorProtos.VectorMatch.newBuilder().setScore(0.5))
        .setMetric(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_COSINE)
        .build())).when(daprStub).queryVectorsAlpha1(any(), any());

    assertThrowsDaprException("INTERNAL", () -> previewClient.queryVectors(
        new QueryVectorsRequestAlpha1(STORE, COLLECTION, VectorQuery.byId("a"))).block(TIMEOUT));
  }

  @Test
  @DisplayName("queryVectors should reject a missing query, an empty vector, a blank ID and a negative top K")
  public void queryVectorsInvalidArgumentsTest() {
    assertRejected(() -> previewClient.queryVectors(null));
    assertRejected(() -> previewClient.queryVectors(new QueryVectorsRequestAlpha1(STORE, COLLECTION, null)));
    IllegalArgumentException neither = assertThrows(IllegalArgumentException.class, () -> previewClient.queryVectors(
        new QueryVectorsRequestAlpha1(STORE, COLLECTION, VectorQuery.byVector(null))).block(TIMEOUT));
    assertEquals("A query needs exactly one of a vector or a record ID.", neither.getMessage());
    assertRejected(() -> previewClient.queryVectors(new QueryVectorsRequestAlpha1(
        STORE, COLLECTION, VectorQuery.byVector(new float[0]))));
    assertRejected(() -> previewClient.queryVectors(new QueryVectorsRequestAlpha1(
        STORE, COLLECTION, VectorQuery.byId(" "))));
    assertRejected(() -> previewClient.queryVectors(new QueryVectorsRequestAlpha1(
        STORE, COLLECTION, VectorQuery.byId("a").setTopK(-1))));
    assertRejected(() -> previewClient.queryVectors(new QueryVectorsRequestAlpha1(
        STORE, " ", VectorQuery.byId("a"))));
    verify(daprStub, never()).queryVectorsAlpha1(any(), any());
  }

  @Test
  @DisplayName("batchQueryVectors should return each query's matches or error, in query order")
  public void batchQueryVectorsTest() {
    ArgumentCaptor<DaprVectorProtos.BatchQueryVectorsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprVectorProtos.BatchQueryVectorsRequestAlpha1.class);
    doAnswer(respond(DaprVectorProtos.BatchQueryVectorsResponseAlpha1.newBuilder()
        .addResults(DaprVectorProtos.BatchQueryResultAlpha1.newBuilder()
            .setResponse(DaprVectorProtos.QueryVectorsResponseAlpha1.newBuilder()
                .addMatches(DaprVectorProtos.VectorMatch.newBuilder()
                    .setRecord(DaprVectorProtos.VectorRecord.newBuilder().setId("lotr"))
                    .setScore(0.9))
                .setMetric(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_COSINE)))
        .addResults(DaprVectorProtos.BatchQueryResultAlpha1.newBuilder()
            .setError(Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage("record x not found")))
        .build())).when(daprStub).batchQueryVectorsAlpha1(captor.capture(), any());

    List<BatchQueryVectorsResultAlpha1> results = previewClient.batchQueryVectors(
        new BatchQueryVectorsRequestAlpha1(STORE, COLLECTION, List.of(
            VectorQuery.byVector(new float[] {0.1f}).setMetric(DistanceMetric.COSINE), VectorQuery.byId("x")))
            .setMetadata(Map.of("k", "v"))).block();

    assertEquals(2, captor.getValue().getQueriesCount());
    assertEquals(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_COSINE, captor.getValue().getQueries(0).getMetric());
    assertEquals(Map.of("k", "v"), captor.getValue().getMetadataMap());
    assertEquals(2, results.size());
    assertNull(results.get(0).getError());
    assertEquals("lotr", results.get(0).getResponse().getMatches().get(0).getRecord().getId());
    assertEquals(DistanceMetric.COSINE, results.get(0).getResponse().getMetric());
    assertNull(results.get(1).getResponse());
    assertEquals("NOT_FOUND", results.get(1).getError().getErrorCode());
    assertEquals("NOT_FOUND: record x not found", results.get(1).getError().getMessage());
  }

  @Test
  @DisplayName("batchQueryVectors should fail when the reply does not hold one result per query")
  public void batchQueryVectorsResultCountMismatchTest() {
    doAnswer(respond(DaprVectorProtos.BatchQueryVectorsResponseAlpha1.newBuilder()
        .addResults(DaprVectorProtos.BatchQueryResultAlpha1.newBuilder()
            .setResponse(DaprVectorProtos.QueryVectorsResponseAlpha1.newBuilder()
                .setMetric(DaprVectorProtos.DistanceMetric.DISTANCE_METRIC_COSINE)))
        .build())).when(daprStub).batchQueryVectorsAlpha1(any(), any());

    assertThrowsDaprException("INTERNAL", () -> previewClient.batchQueryVectors(new BatchQueryVectorsRequestAlpha1(
        STORE, COLLECTION, List.of(VectorQuery.byId("a"), VectorQuery.byId("b")))).block());
  }

  @Test
  @DisplayName("batchQueryVectors should fail on a result with neither a response nor an error")
  public void batchQueryVectorsEmptyResultTest() {
    doAnswer(respond(DaprVectorProtos.BatchQueryVectorsResponseAlpha1.newBuilder()
        .addResults(DaprVectorProtos.BatchQueryResultAlpha1.getDefaultInstance())
        .build())).when(daprStub).batchQueryVectorsAlpha1(any(), any());

    assertThrowsDaprException("INTERNAL", () -> previewClient.batchQueryVectors(new BatchQueryVectorsRequestAlpha1(
        STORE, COLLECTION, List.of(VectorQuery.byId("a")))).block());
  }

  @Test
  @DisplayName("batchQueryVectors should fail on an error result with an OK status code")
  public void batchQueryVectorsOkErrorTest() {
    doAnswer(respond(DaprVectorProtos.BatchQueryVectorsResponseAlpha1.newBuilder()
        .addResults(DaprVectorProtos.BatchQueryResultAlpha1.newBuilder()
            .setError(Status.newBuilder().setCode(Code.OK_VALUE)))
        .build())).when(daprStub).batchQueryVectorsAlpha1(any(), any());

    assertThrowsDaprException("INTERNAL", () -> previewClient.batchQueryVectors(new BatchQueryVectorsRequestAlpha1(
        STORE, COLLECTION, List.of(VectorQuery.byId("a")))).block());
  }

  @Test
  @DisplayName("batchQueryVectors should reject missing queries and invalid queries")
  public void batchQueryVectorsInvalidArgumentsTest() {
    List<VectorQuery> withNull = new ArrayList<>();
    withNull.add(VectorQuery.byId("a"));
    withNull.add(null);
    assertRejected(() -> previewClient.batchQueryVectors(null));
    assertRejected(() -> previewClient.batchQueryVectors(new BatchQueryVectorsRequestAlpha1(STORE, COLLECTION, null)));
    assertRejected(() -> previewClient.batchQueryVectors(new BatchQueryVectorsRequestAlpha1(
        STORE, COLLECTION, withNull)));
    assertRejected(() -> previewClient.batchQueryVectors(new BatchQueryVectorsRequestAlpha1(
        STORE, COLLECTION, List.of(VectorQuery.byVector(new float[0])))));
    verify(daprStub, never()).batchQueryVectorsAlpha1(any(), any());
  }
}
