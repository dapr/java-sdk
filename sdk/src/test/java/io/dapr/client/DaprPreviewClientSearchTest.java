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
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import io.dapr.client.domain.DeleteSearchDocumentsRequestAlpha1;
import io.dapr.client.domain.GetSearchDocumentsRequestAlpha1;
import io.dapr.client.domain.GetSearchIndexResponseAlpha1;
import io.dapr.client.domain.IndexAck;
import io.dapr.client.domain.IndexSearchDocumentsRequestAlpha1;
import io.dapr.client.domain.IndexSearchDocumentsResponseAlpha1;
import io.dapr.client.domain.IndexingOptions;
import io.dapr.client.domain.IndexingWaitTimeoutAction;
import io.dapr.client.domain.SearchDocument;
import io.dapr.client.domain.SearchHit;
import io.dapr.client.domain.SearchQuery;
import io.dapr.client.domain.SearchRequestAlpha1;
import io.dapr.client.domain.SearchResponseAlpha1;
import io.dapr.client.domain.SortClause;
import io.dapr.client.domain.SortOrder;
import io.dapr.client.domain.TotalHitsRelation;
import io.dapr.exceptions.DaprErrorDetails;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprSearchProtos;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
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

public class DaprPreviewClientSearchTest {

  private static final String STORE = "searchstore";

  private static final String INDEX = "books";

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private static final byte[] DUNE = "{\"title\":\"Dune\"}".getBytes(StandardCharsets.UTF_8);

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

  private static void assertInexact(Supplier<Mono<?>> call) {
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> call.get().block(TIMEOUT));
    assertTrue(error.getMessage().contains("cannot be represented exactly"), error.getMessage());
  }

  private static Value string(String value) {
    return Value.newBuilder().setStringValue(value).build();
  }

  private static DaprSearchProtos.IndexDocumentsResponseAlpha1 completed() {
    return DaprSearchProtos.IndexDocumentsResponseAlpha1.newBuilder()
        .setAck(DaprSearchProtos.IndexAck.INDEX_ACK_COMPLETED)
        .build();
  }

  private DaprSearchProtos.SearchRequestAlpha1 sentBy(SearchQuery query) {
    ArgumentCaptor<DaprSearchProtos.SearchRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.SearchRequestAlpha1.class);
    doAnswer(respond(DaprSearchProtos.SearchResponseAlpha1.getDefaultInstance()))
        .when(daprStub).searchAlpha1(captor.capture(), any());
    previewClient.search(new SearchRequestAlpha1(STORE, INDEX, query)).block(TIMEOUT);
    return captor.getValue();
  }

  @Test
  @DisplayName("createSearchIndex should send the names and metadata")
  public void createSearchIndexTest() {
    ArgumentCaptor<DaprSearchProtos.CreateIndexRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.CreateIndexRequestAlpha1.class);
    doAnswer(respond(Empty.getDefaultInstance())).when(daprStub).createIndexAlpha1(captor.capture(), any());

    previewClient.createSearchIndex(STORE, INDEX, Map.of("primaryKey", "id")).block(TIMEOUT);

    DaprSearchProtos.CreateIndexRequestAlpha1 sent = captor.getValue();
    assertEquals(STORE, sent.getStoreName());
    assertEquals(INDEX, sent.getIndex());
    assertEquals(Map.of("primaryKey", "id"), sent.getMetadataMap());
  }

  @Test
  @DisplayName("createSearchIndex without metadata should send the names only")
  public void createSearchIndexWithoutMetadataTest() {
    ArgumentCaptor<DaprSearchProtos.CreateIndexRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.CreateIndexRequestAlpha1.class);
    doAnswer(respond(Empty.getDefaultInstance())).when(daprStub).createIndexAlpha1(captor.capture(), any());

    previewClient.createSearchIndex(STORE, INDEX).block(TIMEOUT);

    assertEquals(STORE, captor.getValue().getStoreName());
    assertEquals(INDEX, captor.getValue().getIndex());
    assertTrue(captor.getValue().getMetadataMap().isEmpty());
  }

  @Test
  @DisplayName("getSearchIndex should send the names and metadata and map the reply")
  public void getSearchIndexTest() {
    ArgumentCaptor<DaprSearchProtos.GetIndexRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.GetIndexRequestAlpha1.class);
    doAnswer(respond(DaprSearchProtos.GetIndexResponseAlpha1.newBuilder()
        .setIndex(INDEX)
        .setDocumentCount(42)
        .putProperties("primaryKey", "id")
        .build())).when(daprStub).getIndexAlpha1(captor.capture(), any());

    GetSearchIndexResponseAlpha1 response =
        previewClient.getSearchIndex(STORE, INDEX, Map.of("consistency", "strong")).block(TIMEOUT);

    assertEquals(STORE, captor.getValue().getStoreName());
    assertEquals(INDEX, captor.getValue().getIndex());
    assertEquals(Map.of("consistency", "strong"), captor.getValue().getMetadataMap());
    assertEquals(INDEX, response.getIndex());
    assertEquals(42L, response.getDocumentCount());
    assertEquals(Map.of("primaryKey", "id"), response.getProperties());
  }

  @Test
  @DisplayName("getSearchIndex without metadata should send the names only")
  public void getSearchIndexWithoutMetadataTest() {
    ArgumentCaptor<DaprSearchProtos.GetIndexRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.GetIndexRequestAlpha1.class);
    doAnswer(respond(DaprSearchProtos.GetIndexResponseAlpha1.getDefaultInstance()))
        .when(daprStub).getIndexAlpha1(captor.capture(), any());

    previewClient.getSearchIndex(STORE, INDEX).block(TIMEOUT);

    assertEquals(STORE, captor.getValue().getStoreName());
    assertEquals(INDEX, captor.getValue().getIndex());
    assertTrue(captor.getValue().getMetadataMap().isEmpty());
  }

  @Test
  @DisplayName("listSearchIndexes should send the store and metadata and return the names")
  public void listSearchIndexesTest() {
    ArgumentCaptor<DaprSearchProtos.ListIndexesRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.ListIndexesRequestAlpha1.class);
    doAnswer(respond(DaprSearchProtos.ListIndexesResponseAlpha1.newBuilder()
        .addIndexes("books")
        .addIndexes("films")
        .build())).when(daprStub).listIndexesAlpha1(captor.capture(), any());

    List<String> indexes = previewClient.listSearchIndexes(STORE, Map.of("prefix", "b")).block(TIMEOUT);

    assertEquals(STORE, captor.getValue().getStoreName());
    assertEquals(Map.of("prefix", "b"), captor.getValue().getMetadataMap());
    assertEquals(List.of("books", "films"), indexes);
  }

  @Test
  @DisplayName("listSearchIndexes without metadata should send the store only")
  public void listSearchIndexesWithoutMetadataTest() {
    ArgumentCaptor<DaprSearchProtos.ListIndexesRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.ListIndexesRequestAlpha1.class);
    doAnswer(respond(DaprSearchProtos.ListIndexesResponseAlpha1.getDefaultInstance()))
        .when(daprStub).listIndexesAlpha1(captor.capture(), any());

    assertTrue(previewClient.listSearchIndexes(STORE).block(TIMEOUT).isEmpty());

    assertEquals(STORE, captor.getValue().getStoreName());
    assertTrue(captor.getValue().getMetadataMap().isEmpty());
  }

  @Test
  @DisplayName("deleteSearchIndex should send the names and metadata, with and without the overload")
  public void deleteSearchIndexTest() {
    ArgumentCaptor<DaprSearchProtos.DeleteIndexRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.DeleteIndexRequestAlpha1.class);
    doAnswer(respond(Empty.getDefaultInstance())).when(daprStub).deleteIndexAlpha1(captor.capture(), any());

    previewClient.deleteSearchIndex(STORE, INDEX, Map.of("force", "true")).block(TIMEOUT);
    assertEquals(STORE, captor.getValue().getStoreName());
    assertEquals(INDEX, captor.getValue().getIndex());
    assertEquals(Map.of("force", "true"), captor.getValue().getMetadataMap());

    previewClient.deleteSearchIndex(STORE, INDEX).block(TIMEOUT);
    assertEquals(STORE, captor.getValue().getStoreName());
    assertEquals(INDEX, captor.getValue().getIndex());
    assertTrue(captor.getValue().getMetadataMap().isEmpty());
  }

  @Test
  @DisplayName("every call should reject a null or blank store or index name")
  public void invalidNamesTest() {
    List<SearchDocument> documents = List.of(new SearchDocument("a", DUNE));
    List<String> ids = List.of("a");
    for (String blank : Arrays.asList(null, "", "  ")) {
      assertRejected(() -> previewClient.createSearchIndex(blank, INDEX));
      assertRejected(() -> previewClient.createSearchIndex(STORE, blank));
      assertRejected(() -> previewClient.getSearchIndex(blank, INDEX));
      assertRejected(() -> previewClient.getSearchIndex(STORE, blank));
      assertRejected(() -> previewClient.listSearchIndexes(blank));
      assertRejected(() -> previewClient.deleteSearchIndex(blank, INDEX));
      assertRejected(() -> previewClient.deleteSearchIndex(STORE, blank));
      assertRejected(() -> previewClient.indexSearchDocuments(
          new IndexSearchDocumentsRequestAlpha1(blank, INDEX, documents)));
      assertRejected(() -> previewClient.indexSearchDocuments(
          new IndexSearchDocumentsRequestAlpha1(STORE, blank, documents)));
      assertRejected(() -> previewClient.getSearchDocuments(new GetSearchDocumentsRequestAlpha1(blank, INDEX, ids)));
      assertRejected(() -> previewClient.getSearchDocuments(new GetSearchDocumentsRequestAlpha1(STORE, blank, ids)));
      assertRejected(() -> previewClient.deleteSearchDocuments(
          new DeleteSearchDocumentsRequestAlpha1(blank, INDEX, ids)));
      assertRejected(() -> previewClient.deleteSearchDocuments(
          new DeleteSearchDocumentsRequestAlpha1(STORE, blank, ids)));
      assertRejected(() -> previewClient.search(new SearchRequestAlpha1(blank, INDEX, SearchQuery.matchAll())));
      assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, blank, SearchQuery.matchAll())));
    }
    verify(daprStub, never()).createIndexAlpha1(any(), any());
    verify(daprStub, never()).getIndexAlpha1(any(), any());
    verify(daprStub, never()).listIndexesAlpha1(any(), any());
    verify(daprStub, never()).deleteIndexAlpha1(any(), any());
    verify(daprStub, never()).indexDocumentsAlpha1(any(), any());
    verify(daprStub, never()).getDocumentsAlpha1(any(), any());
    verify(daprStub, never()).deleteDocumentsAlpha1(any(), any());
    verify(daprStub, never()).searchAlpha1(any(), any());
  }

  @Test
  @DisplayName("every RPC should surface a sidecar error with its gRPC code")
  public void sidecarErrorTest() {
    Answer<Void> notFound = invocation -> {
      StreamObserver<?> observer = (StreamObserver<?>) invocation.getArguments()[1];
      observer.onError(newStatusRuntimeException("NOT_FOUND", "index books not found"));
      return null;
    };
    doAnswer(notFound).when(daprStub).createIndexAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).getIndexAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).listIndexesAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).deleteIndexAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).indexDocumentsAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).getDocumentsAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).deleteDocumentsAlpha1(any(), any());
    doAnswer(notFound).when(daprStub).searchAlpha1(any(), any());
    List<String> ids = List.of("a");
    List<Supplier<Mono<?>>> calls = List.of(
        () -> previewClient.createSearchIndex(STORE, INDEX),
        () -> previewClient.getSearchIndex(STORE, INDEX),
        () -> previewClient.listSearchIndexes(STORE),
        () -> previewClient.deleteSearchIndex(STORE, INDEX),
        () -> previewClient.indexSearchDocuments(
            new IndexSearchDocumentsRequestAlpha1(STORE, INDEX, List.of(new SearchDocument("a", DUNE)))),
        () -> previewClient.getSearchDocuments(new GetSearchDocumentsRequestAlpha1(STORE, INDEX, ids)),
        () -> previewClient.deleteSearchDocuments(new DeleteSearchDocumentsRequestAlpha1(STORE, INDEX, ids)),
        () -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX, null)));

    for (Supplier<Mono<?>> call : calls) {
      DaprException exception = assertThrows(DaprException.class, () -> call.get().block(TIMEOUT));
      assertEquals("NOT_FOUND", exception.getErrorCode());
    }
  }

  @Test
  @DisplayName("indexSearchDocuments should send every field and map the reply")
  public void indexSearchDocumentsTest() {
    ArgumentCaptor<DaprSearchProtos.IndexDocumentsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.IndexDocumentsRequestAlpha1.class);
    doAnswer(respond(DaprSearchProtos.IndexDocumentsResponseAlpha1.newBuilder()
        .setAck(DaprSearchProtos.IndexAck.INDEX_ACK_QUEUED)
        .build())).when(daprStub).indexDocumentsAlpha1(captor.capture(), any());

    IndexSearchDocumentsResponseAlpha1 response = previewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(STORE, INDEX, List.of(
            new SearchDocument("dune", DUNE).setMetadata(Map.of("source", "library")),
            new SearchDocument("hobbit", "{}".getBytes(StandardCharsets.UTF_8))))
            .setMetadata(Map.of("batch", "1"))
            .setOptions(IndexingOptions.waitForCompletion(
                Duration.ofSeconds(3, 500), IndexingWaitTimeoutAction.FAIL_REQUEST))).block(TIMEOUT);

    DaprSearchProtos.IndexDocumentsRequestAlpha1 sent = captor.getValue();
    assertEquals(STORE, sent.getStoreName());
    assertEquals(INDEX, sent.getIndex());
    assertEquals(Map.of("batch", "1"), sent.getMetadataMap());
    assertEquals(2, sent.getDocumentsCount());
    assertEquals("dune", sent.getDocuments(0).getId());
    assertEquals(ByteString.copyFrom(DUNE), sent.getDocuments(0).getContent());
    assertEquals(Map.of("source", "library"), sent.getDocuments(0).getMetadataMap());
    assertEquals("hobbit", sent.getDocuments(1).getId());
    assertTrue(sent.getDocuments(1).getMetadataMap().isEmpty());
    assertEquals(DaprSearchProtos.IndexingMode.INDEXING_MODE_WAIT_FOR_COMPLETION, sent.getOptions().getMode());
    assertEquals(3, sent.getOptions().getWaitTimeout().getSeconds());
    assertEquals(500, sent.getOptions().getWaitTimeout().getNanos());
    assertEquals(DaprSearchProtos.IndexingWaitTimeoutAction.INDEXING_WAIT_TIMEOUT_ACTION_FAIL_REQUEST,
        sent.getOptions().getOnWaitTimeout());
    assertEquals(IndexAck.QUEUED, response.getAck());
    assertTrue(response.getFailedItems().isEmpty());
  }

  @Test
  @DisplayName("indexSearchDocuments should omit the options and metadata when they are not set")
  public void indexSearchDocumentsDefaultsTest() {
    ArgumentCaptor<DaprSearchProtos.IndexDocumentsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.IndexDocumentsRequestAlpha1.class);
    doAnswer(respond(completed())).when(daprStub).indexDocumentsAlpha1(captor.capture(), any());

    IndexSearchDocumentsResponseAlpha1 response = previewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(STORE, INDEX, List.of(new SearchDocument("dune", DUNE)))).block(TIMEOUT);

    assertFalse(captor.getValue().hasOptions());
    assertTrue(captor.getValue().getMetadataMap().isEmpty());
    assertEquals(IndexAck.COMPLETED, response.getAck());
  }

  @Test
  @DisplayName("indexSearchDocuments should leave an empty list to the sidecar")
  public void indexSearchDocumentsEmptyListTest() {
    ArgumentCaptor<DaprSearchProtos.IndexDocumentsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.IndexDocumentsRequestAlpha1.class);
    doAnswer(respond(completed())).when(daprStub).indexDocumentsAlpha1(captor.capture(), any());

    previewClient.indexSearchDocuments(new IndexSearchDocumentsRequestAlpha1(STORE, INDEX, List.of())).block(TIMEOUT);

    assertEquals(0, captor.getValue().getDocumentsCount());
  }

  @Test
  @DisplayName("indexSearchDocuments should send each indexing mode")
  public void indexSearchDocumentsIndexingModesTest() {
    ArgumentCaptor<DaprSearchProtos.IndexDocumentsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.IndexDocumentsRequestAlpha1.class);
    doAnswer(respond(completed())).when(daprStub).indexDocumentsAlpha1(captor.capture(), any());
    List<SearchDocument> documents = List.of(new SearchDocument("dune", DUNE));

    previewClient.indexSearchDocuments(new IndexSearchDocumentsRequestAlpha1(STORE, INDEX, documents)
        .setOptions(IndexingOptions.returnOnAcceptance())).block(TIMEOUT);
    assertEquals(DaprSearchProtos.IndexingMode.INDEXING_MODE_RETURN_ON_ACCEPTANCE,
        captor.getValue().getOptions().getMode());
    assertFalse(captor.getValue().getOptions().hasWaitTimeout());

    previewClient.indexSearchDocuments(new IndexSearchDocumentsRequestAlpha1(STORE, INDEX, documents)
        .setOptions(IndexingOptions.waitForCompletion(
            Duration.ofSeconds(1), IndexingWaitTimeoutAction.CONTINUE_ASYNC))).block(TIMEOUT);
    assertEquals(DaprSearchProtos.IndexingWaitTimeoutAction.INDEXING_WAIT_TIMEOUT_ACTION_CONTINUE_ASYNC,
        captor.getValue().getOptions().getOnWaitTimeout());
  }

  @Test
  @DisplayName("indexSearchDocuments should return per-item failures with their code and details")
  public void indexSearchDocumentsFailedItemsTest() {
    doAnswer(respond(DaprSearchProtos.IndexDocumentsResponseAlpha1.newBuilder()
        .setAck(DaprSearchProtos.IndexAck.INDEX_ACK_COMPLETED)
        .addFailedItems(DaprSearchProtos.FailedItem.newBuilder()
            .setId("dune")
            .setError(Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage("content must be a JSON object")
                .addDetails(Any.pack(ErrorInfo.newBuilder().setReason("INVALID_CONTENT").build()))))
        .build())).when(daprStub).indexDocumentsAlpha1(any(), any());

    IndexSearchDocumentsResponseAlpha1 response = previewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(STORE, INDEX, List.of(
            new SearchDocument("dune", "not json".getBytes(StandardCharsets.UTF_8))))).block(TIMEOUT);

    assertEquals(IndexAck.COMPLETED, response.getAck());
    assertEquals(1, response.getFailedItems().size());
    assertEquals("dune", response.getFailedItems().get(0).getId());
    DaprException error = response.getFailedItems().get(0).getError();
    assertEquals("INVALID_ARGUMENT", error.getErrorCode());
    assertEquals("INVALID_ARGUMENT: content must be a JSON object", error.getMessage());
    assertEquals("INVALID_CONTENT",
        error.getErrorDetails().get(DaprErrorDetails.ErrorDetailType.ERROR_INFO, "reason", TypeRef.STRING));
  }

  @Test
  @DisplayName("indexSearchDocuments should fail on a failed item with an OK status code")
  public void indexSearchDocumentsOkFailedItemTest() {
    doAnswer(respond(DaprSearchProtos.IndexDocumentsResponseAlpha1.newBuilder()
        .setAck(DaprSearchProtos.IndexAck.INDEX_ACK_COMPLETED)
        .addFailedItems(DaprSearchProtos.FailedItem.newBuilder()
            .setId("dune")
            .setError(Status.newBuilder().setCode(Code.OK_VALUE)))
        .build())).when(daprStub).indexDocumentsAlpha1(any(), any());

    assertThrowsDaprException("INTERNAL", () -> previewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(STORE, INDEX, List.of(new SearchDocument("dune", DUNE)))).block(TIMEOUT));
  }

  @Test
  @DisplayName("indexSearchDocuments should reject missing or invalid documents and options")
  public void indexSearchDocumentsInvalidArgumentsTest() {
    List<SearchDocument> valid = List.of(new SearchDocument("dune", DUNE));
    assertRejected(() -> previewClient.indexSearchDocuments(null));
    assertRejected(() -> previewClient.indexSearchDocuments(new IndexSearchDocumentsRequestAlpha1(STORE, INDEX, null)));
    assertRejected(() -> previewClient.indexSearchDocuments(new IndexSearchDocumentsRequestAlpha1(
        STORE, INDEX, Collections.singletonList(null))));
    for (String blank : Arrays.asList(null, "", "  ")) {
      assertRejected(() -> previewClient.indexSearchDocuments(new IndexSearchDocumentsRequestAlpha1(
          STORE, INDEX, List.of(new SearchDocument(blank, DUNE)))));
    }
    assertRejected(() -> previewClient.indexSearchDocuments(new IndexSearchDocumentsRequestAlpha1(
        STORE, INDEX, List.of(new SearchDocument("dune", null)))));
    assertRejected(() -> previewClient.indexSearchDocuments(new IndexSearchDocumentsRequestAlpha1(STORE, INDEX, valid)
        .setOptions(IndexingOptions.waitForCompletion(Duration.ZERO, IndexingWaitTimeoutAction.FAIL_REQUEST))));
    assertRejected(() -> previewClient.indexSearchDocuments(new IndexSearchDocumentsRequestAlpha1(STORE, INDEX, valid)
        .setOptions(IndexingOptions.waitForCompletion(Duration.ofSeconds(1), null))));
    verify(daprStub, never()).indexDocumentsAlpha1(any(), any());
  }

  @Test
  @DisplayName("getSearchDocuments should send every field and keep the reply's order")
  public void getSearchDocumentsTest() {
    ArgumentCaptor<DaprSearchProtos.GetDocumentsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.GetDocumentsRequestAlpha1.class);
    doAnswer(respond(DaprSearchProtos.GetDocumentsResponseAlpha1.newBuilder()
        .addDocuments(DaprSearchProtos.SearchDocument.newBuilder()
            .setId("dune")
            .setContent(ByteString.copyFrom(DUNE))
            .putMetadata("source", "library"))
        .addDocuments(DaprSearchProtos.SearchDocument.newBuilder().setId("hobbit"))
        .build())).when(daprStub).getDocumentsAlpha1(captor.capture(), any());

    List<SearchDocument> documents = previewClient.getSearchDocuments(
        new GetSearchDocumentsRequestAlpha1(STORE, INDEX, List.of("dune", "missing", "hobbit"))
            .setIncludeContent(true)
            .setMetadata(Map.of("consistency", "strong"))).block(TIMEOUT);

    DaprSearchProtos.GetDocumentsRequestAlpha1 sent = captor.getValue();
    assertEquals(STORE, sent.getStoreName());
    assertEquals(INDEX, sent.getIndex());
    assertEquals(List.of("dune", "missing", "hobbit"), sent.getIdsList());
    assertTrue(sent.hasIncludeContent());
    assertTrue(sent.getIncludeContent());
    assertEquals(Map.of("consistency", "strong"), sent.getMetadataMap());
    assertEquals(2, documents.size());
    assertEquals("dune", documents.get(0).getId());
    assertArrayEquals(DUNE, documents.get(0).getContent());
    assertEquals(Map.of("source", "library"), documents.get(0).getMetadata());
    assertEquals("hobbit", documents.get(1).getId());
    assertNull(documents.get(1).getContent());
    assertNull(documents.get(1).getMetadata());
  }

  @Test
  @DisplayName("getSearchDocuments should omit the include flag when it is not set")
  public void getSearchDocumentsIncludeContentUnsetTest() {
    ArgumentCaptor<DaprSearchProtos.GetDocumentsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.GetDocumentsRequestAlpha1.class);
    doAnswer(respond(DaprSearchProtos.GetDocumentsResponseAlpha1.getDefaultInstance()))
        .when(daprStub).getDocumentsAlpha1(captor.capture(), any());

    previewClient.getSearchDocuments(new GetSearchDocumentsRequestAlpha1(STORE, INDEX, List.of("dune"))).block(TIMEOUT);

    assertFalse(captor.getValue().hasIncludeContent());
  }

  @Test
  @DisplayName("deleteSearchDocuments should send every field and return the acknowledgement")
  public void deleteSearchDocumentsTest() {
    ArgumentCaptor<DaprSearchProtos.DeleteDocumentsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.DeleteDocumentsRequestAlpha1.class);
    doAnswer(respond(DaprSearchProtos.DeleteDocumentsResponseAlpha1.newBuilder()
        .setAck(DaprSearchProtos.IndexAck.INDEX_ACK_QUEUED)
        .build())).when(daprStub).deleteDocumentsAlpha1(captor.capture(), any());

    IndexAck ack = previewClient.deleteSearchDocuments(
        new DeleteSearchDocumentsRequestAlpha1(STORE, INDEX, List.of("dune", "hobbit"))
            .setMetadata(Map.of("batch", "1"))
            .setOptions(IndexingOptions.returnOnAcceptance())).block(TIMEOUT);

    DaprSearchProtos.DeleteDocumentsRequestAlpha1 sent = captor.getValue();
    assertEquals(STORE, sent.getStoreName());
    assertEquals(INDEX, sent.getIndex());
    assertEquals(List.of("dune", "hobbit"), sent.getIdsList());
    assertEquals(Map.of("batch", "1"), sent.getMetadataMap());
    assertEquals(DaprSearchProtos.IndexingMode.INDEXING_MODE_RETURN_ON_ACCEPTANCE, sent.getOptions().getMode());
    assertEquals(IndexAck.QUEUED, ack);
  }

  @Test
  @DisplayName("deleteSearchDocuments should omit the options when they are not set")
  public void deleteSearchDocumentsDefaultsTest() {
    ArgumentCaptor<DaprSearchProtos.DeleteDocumentsRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.DeleteDocumentsRequestAlpha1.class);
    doAnswer(respond(DaprSearchProtos.DeleteDocumentsResponseAlpha1.newBuilder()
        .setAck(DaprSearchProtos.IndexAck.INDEX_ACK_COMPLETED)
        .build())).when(daprStub).deleteDocumentsAlpha1(captor.capture(), any());

    IndexAck ack = previewClient.deleteSearchDocuments(
        new DeleteSearchDocumentsRequestAlpha1(STORE, INDEX, List.of("dune"))).block(TIMEOUT);

    assertFalse(captor.getValue().hasOptions());
    assertEquals(IndexAck.COMPLETED, ack);
  }

  @Test
  @DisplayName("document reads and deletes should reject missing or blank IDs")
  public void documentIdsInvalidArgumentsTest() {
    assertRejected(() -> previewClient.getSearchDocuments(null));
    assertRejected(() -> previewClient.deleteSearchDocuments(null));
    assertRejected(() -> previewClient.getSearchDocuments(new GetSearchDocumentsRequestAlpha1(STORE, INDEX, null)));
    assertRejected(() -> previewClient.deleteSearchDocuments(
        new DeleteSearchDocumentsRequestAlpha1(STORE, INDEX, null)));
    for (String blank : Arrays.asList(null, "", "  ")) {
      List<String> ids = Arrays.asList("dune", blank);
      assertRejected(() -> previewClient.getSearchDocuments(new GetSearchDocumentsRequestAlpha1(STORE, INDEX, ids)));
      assertRejected(() -> previewClient.deleteSearchDocuments(
          new DeleteSearchDocumentsRequestAlpha1(STORE, INDEX, ids)));
    }
    assertRejected(() -> previewClient.deleteSearchDocuments(
        new DeleteSearchDocumentsRequestAlpha1(STORE, INDEX, List.of("dune"))
            .setOptions(IndexingOptions.waitForCompletion(null, IndexingWaitTimeoutAction.FAIL_REQUEST))));
    verify(daprStub, never()).getDocumentsAlpha1(any(), any());
    verify(daprStub, never()).deleteDocumentsAlpha1(any(), any());
  }

  @Test
  @DisplayName("writes should fail on an invalid or unspecified acknowledgement")
  public void invalidAckResponseTest() {
    doAnswer(respond(DaprSearchProtos.IndexDocumentsResponseAlpha1.getDefaultInstance()))
        .when(daprStub).indexDocumentsAlpha1(any(), any());
    doAnswer(respond(DaprSearchProtos.DeleteDocumentsResponseAlpha1.newBuilder().setAckValue(9).build()))
        .when(daprStub).deleteDocumentsAlpha1(any(), any());

    assertThrowsDaprException("INTERNAL", () -> previewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(STORE, INDEX, List.of(new SearchDocument("a", DUNE)))).block(TIMEOUT));
    assertThrowsDaprException("INTERNAL", () -> previewClient.deleteSearchDocuments(
        new DeleteSearchDocumentsRequestAlpha1(STORE, INDEX, List.of("a"))).block(TIMEOUT));
  }

  @Test
  @DisplayName("search by text should send every query field and map the reply")
  public void searchByTextTest() {
    ArgumentCaptor<DaprSearchProtos.SearchRequestAlpha1> captor =
        ArgumentCaptor.forClass(DaprSearchProtos.SearchRequestAlpha1.class);
    doAnswer(respond(DaprSearchProtos.SearchResponseAlpha1.newBuilder()
        .addHits(DaprSearchProtos.SearchHit.newBuilder()
            .setDocument(DaprSearchProtos.SearchDocument.newBuilder()
                .setId("dune")
                .setContent(ByteString.copyFrom(DUNE))
                .putMetadata("source", "library"))
            .setScore(1.5)
            .putHighlights("title", "<em>Dune</em>"))
        .setTotalHits(7)
        .setContinuationToken("next")
        .setTotalHitsRelation(DaprSearchProtos.TotalHitsRelation.TOTAL_HITS_RELATION_EXACT)
        .build())).when(daprStub).searchAlpha1(captor.capture(), any());

    SearchResponseAlpha1 response = previewClient.search(new SearchRequestAlpha1(STORE, INDEX,
        SearchQuery.byText("dune")
            .setFilter(Map.of("genre", Map.of("$in", new LinkedHashSet<>(List.of("scifi", "fantasy")))))
            .setTopK(5)
            .setContinuationToken("first")
            .setReturnFields(List.of("title"))
            .setIncludeContent(true)
            .setSearchFields(List.of("title", "summary"))
            .setSort(List.of(SortClause.desc("year"), SortClause.asc("title")))
            .setHighlightFields(List.of("summary")))
        .setMetadata(Map.of("consistency", "strong"))).block(TIMEOUT);

    DaprSearchProtos.SearchRequestAlpha1 sent = captor.getValue();
    assertEquals(STORE, sent.getStoreName());
    assertEquals(INDEX, sent.getIndex());
    assertEquals(Map.of("consistency", "strong"), sent.getMetadataMap());
    DaprSearchProtos.SearchQuery query = sent.getQuery();
    assertEquals(DaprSearchProtos.SearchQuery.KindCase.TEXT, query.getKindCase());
    assertEquals("dune", query.getText());
    assertEquals(List.of(string("scifi"), string("fantasy")),
        query.getFilter().getFieldsOrThrow("genre").getStructValue().getFieldsOrThrow("$in")
            .getListValue().getValuesList());
    assertEquals(5, query.getTopK());
    assertEquals("first", query.getContinuationToken());
    assertEquals(List.of("title"), query.getReturnFieldsList());
    assertTrue(query.getIncludeContent());
    assertEquals(List.of("title", "summary"), query.getSearchFieldsList());
    assertEquals(2, query.getSortCount());
    assertEquals("year", query.getSort(0).getField());
    assertEquals(DaprSearchProtos.SortOrder.SORT_ORDER_DESC, query.getSort(0).getOrder());
    assertEquals("title", query.getSort(1).getField());
    assertEquals(DaprSearchProtos.SortOrder.SORT_ORDER_ASC, query.getSort(1).getOrder());
    assertEquals(List.of("summary"), query.getHighlightFieldsList());

    assertEquals(1, response.getHits().size());
    SearchHit hit = response.getHits().get(0);
    assertEquals("dune", hit.getDocument().getId());
    assertArrayEquals(DUNE, hit.getDocument().getContent());
    assertEquals(Map.of("source", "library"), hit.getDocument().getMetadata());
    assertEquals(1.5, hit.getScore());
    assertEquals(Map.of("title", "<em>Dune</em>"), hit.getHighlights());
    assertEquals(7L, response.getTotalHits());
    assertEquals("next", response.getContinuationToken());
    assertEquals(TotalHitsRelation.EXACT, response.getTotalHitsRelation());
  }

  @Test
  @DisplayName("search by native query should send the query and no text")
  public void searchByNativeTest() {
    DaprSearchProtos.SearchRequestAlpha1 sent =
        sentBy(SearchQuery.byNative(Map.of("match", Map.of("title", "dune"))));

    assertEquals(DaprSearchProtos.SearchQuery.KindCase.NATIVE, sent.getQuery().getKindCase());
    assertEquals(string("dune"),
        sent.getQuery().getNative().getFieldsOrThrow("match").getStructValue().getFieldsOrThrow("title"));
  }

  @Test
  @DisplayName("search matching everything should send neither text nor a native query, and keep the filter")
  public void searchMatchAllTest() {
    DaprSearchProtos.SearchRequestAlpha1 sent = sentBy(SearchQuery.matchAll().setFilter(Map.of("genre", "scifi")));

    assertTrue(sent.hasQuery());
    assertEquals(DaprSearchProtos.SearchQuery.KindCase.KIND_NOT_SET, sent.getQuery().getKindCase());
    assertEquals(string("scifi"), sent.getQuery().getFilter().getFieldsOrThrow("genre"));
  }

  @Test
  @DisplayName("search without a query should omit it")
  public void searchWithoutQueryTest() {
    assertFalse(sentBy(null).hasQuery());
  }

  @Test
  @DisplayName("search should omit the query fields that are not set")
  public void searchQueryDefaultsTest() {
    DaprSearchProtos.SearchQuery query = sentBy(SearchQuery.byText("dune")).getQuery();

    assertFalse(query.hasFilter());
    assertFalse(query.hasTopK());
    assertFalse(query.hasContinuationToken());
    assertFalse(query.hasIncludeContent());
    assertEquals(0, query.getReturnFieldsCount());
    assertEquals(0, query.getSearchFieldsCount());
    assertEquals(0, query.getSortCount());
    assertEquals(0, query.getHighlightFieldsCount());
  }

  @Test
  @DisplayName("search should send query fields set to their zero values")
  public void searchExplicitZeroValuesTest() {
    DaprSearchProtos.SearchQuery query = sentBy(SearchQuery.matchAll()
        .setTopK(0)
        .setContinuationToken("")
        .setIncludeContent(false)).getQuery();

    assertTrue(query.hasTopK());
    assertEquals(0, query.getTopK());
    assertTrue(query.hasContinuationToken());
    assertTrue(query.hasIncludeContent());
    assertFalse(query.getIncludeContent());
  }

  @Test
  @DisplayName("search should map a reply with no total, token, content or highlights")
  public void searchResponseDefaultsTest() {
    doAnswer(respond(DaprSearchProtos.SearchResponseAlpha1.newBuilder()
        .addHits(DaprSearchProtos.SearchHit.newBuilder()
            .setDocument(DaprSearchProtos.SearchDocument.newBuilder().setId("dune")))
        .build())).when(daprStub).searchAlpha1(any(), any());

    SearchResponseAlpha1 response = previewClient.search(new SearchRequestAlpha1(STORE, INDEX, null)).block(TIMEOUT);

    assertNull(response.getTotalHits());
    assertNull(response.getContinuationToken());
    assertEquals(TotalHitsRelation.UNSPECIFIED, response.getTotalHitsRelation());
    SearchHit hit = response.getHits().get(0);
    assertNull(hit.getDocument().getContent());
    assertNull(hit.getDocument().getMetadata());
    assertTrue(hit.getHighlights().isEmpty());
  }

  @Test
  @DisplayName("search should keep a total of zero apart from a missing total")
  public void searchZeroTotalTest() {
    doAnswer(respond(DaprSearchProtos.SearchResponseAlpha1.newBuilder()
        .setTotalHits(0)
        .setTotalHitsRelation(DaprSearchProtos.TotalHitsRelation.TOTAL_HITS_RELATION_EXACT)
        .build())).when(daprStub).searchAlpha1(any(), any());

    assertEquals(0L, previewClient.search(new SearchRequestAlpha1(STORE, INDEX, null)).block(TIMEOUT).getTotalHits());
  }

  @Test
  @DisplayName("search should map every total hits relation")
  public void searchTotalHitsRelationsTest() {
    Map<DaprSearchProtos.TotalHitsRelation, TotalHitsRelation> relations = Map.of(
        DaprSearchProtos.TotalHitsRelation.TOTAL_HITS_RELATION_EXACT, TotalHitsRelation.EXACT,
        DaprSearchProtos.TotalHitsRelation.TOTAL_HITS_RELATION_LOWER_BOUND, TotalHitsRelation.LOWER_BOUND,
        DaprSearchProtos.TotalHitsRelation.TOTAL_HITS_RELATION_ESTIMATE, TotalHitsRelation.ESTIMATE);
    relations.forEach((proto, expected) -> {
      doAnswer(respond(DaprSearchProtos.SearchResponseAlpha1.newBuilder()
          .setTotalHits(1)
          .setTotalHitsRelation(proto)
          .build())).when(daprStub).searchAlpha1(any(), any());

      assertEquals(expected, previewClient.search(new SearchRequestAlpha1(STORE, INDEX, null)).block(TIMEOUT)
          .getTotalHitsRelation());
    });
  }

  @Test
  @DisplayName("search should keep the hits when the total hits relation is unknown")
  public void searchUnknownTotalHitsRelationTest() {
    doAnswer(respond(DaprSearchProtos.SearchResponseAlpha1.newBuilder()
        .addHits(DaprSearchProtos.SearchHit.newBuilder()
            .setDocument(DaprSearchProtos.SearchDocument.newBuilder().setId("dune")))
        .setTotalHits(1)
        .setTotalHitsRelationValue(42)
        .build())).when(daprStub).searchAlpha1(any(), any());

    SearchResponseAlpha1 response = previewClient.search(new SearchRequestAlpha1(STORE, INDEX, null)).block(TIMEOUT);

    assertEquals("dune", response.getHits().get(0).getDocument().getId());
    assertEquals(1L, response.getTotalHits());
    assertEquals(TotalHitsRelation.UNSPECIFIED, response.getTotalHitsRelation());
  }

  @Test
  @DisplayName("search should fail on a hit without a document")
  public void searchHitWithoutDocumentTest() {
    doAnswer(respond(DaprSearchProtos.SearchResponseAlpha1.newBuilder()
        .addHits(DaprSearchProtos.SearchHit.newBuilder().setScore(1))
        .build())).when(daprStub).searchAlpha1(any(), any());

    assertThrowsDaprException("INTERNAL",
        () -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX, null)).block(TIMEOUT));
  }

  @Test
  @DisplayName("search should reject an empty text or native query rather than match every document")
  public void searchEmptyQueryTest() {
    assertRejected(() -> previewClient.search(null));
    for (String blank : Arrays.asList(null, "", "  ")) {
      assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX, SearchQuery.byText(blank))));
    }
    assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX, SearchQuery.byNative(null))));
    assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX, SearchQuery.byNative(Map.of()))));
    verify(daprStub, never()).searchAlpha1(any(), any());
  }

  @Test
  @DisplayName("search should reject invalid query fields")
  public void searchInvalidQueryFieldsTest() {
    assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX,
        SearchQuery.matchAll().setTopK(-1))));
    assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX,
        SearchQuery.matchAll().setSort(Collections.singletonList(null)))));
    for (String blank : Arrays.asList(null, "", "  ")) {
      List<String> fields = Arrays.asList("title", blank);
      assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX,
          SearchQuery.matchAll().setSort(List.of(SortClause.asc(blank))))));
      assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX,
          SearchQuery.matchAll().setReturnFields(fields))));
      assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX,
          SearchQuery.matchAll().setSearchFields(fields))));
      assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX,
          SearchQuery.matchAll().setHighlightFields(fields))));
    }
    verify(daprStub, never()).searchAlpha1(any(), any());
  }

  @Test
  @DisplayName("search should reject filter and native values it cannot send faithfully")
  public void searchUnsupportedValuesTest() {
    assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX,
        SearchQuery.matchAll().setFilter(Map.of("since", Duration.ofDays(1))))));
    assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX,
        SearchQuery.byNative(Map.of("terms", new String[] {"dune"})))));
    assertRejected(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX,
        SearchQuery.byNative(Map.of("nested", Map.of(1, "dune"))))));
    verify(daprStub, never()).searchAlpha1(any(), any());
  }

  @Test
  @DisplayName("search should send numbers as doubles, without changing them")
  public void searchNumbersTest() {
    DaprSearchProtos.SearchQuery query = sentBy(SearchQuery.matchAll().setFilter(Map.of(
        "int", 5,
        "long", 1L << 53,
        "float", 1.1f,
        "double", 2.5d,
        "decimal", new BigDecimal("1.10"),
        "big", BigInteger.valueOf(1965),
        "power", 1L << 60,
        "min", Long.MIN_VALUE,
        "bigPower", BigInteger.TWO.pow(70)))).getQuery();

    Map<String, Value> sent = query.getFilter().getFieldsMap();
    assertEquals(5d, sent.get("int").getNumberValue());
    assertEquals(9007199254740992d, sent.get("long").getNumberValue());
    assertEquals(1.1d, sent.get("float").getNumberValue());
    assertEquals(2.5d, sent.get("double").getNumberValue());
    assertEquals(1.1d, sent.get("decimal").getNumberValue());
    assertEquals(1965d, sent.get("big").getNumberValue());
    assertEquals(0x1p60, sent.get("power").getNumberValue());
    assertEquals(-0x1p63, sent.get("min").getNumberValue());
    assertEquals(0x1p70, sent.get("bigPower").getNumberValue());
  }

  @Test
  @DisplayName("search should reject numbers that a double cannot hold exactly")
  public void searchInexactNumbersTest() {
    List<Number> numbers = List.of(
        (1L << 53) + 1,
        Long.MAX_VALUE,
        new BigDecimal("0.1000000000000000055"),
        BigInteger.TEN.pow(400));
    for (Number number : numbers) {
      assertInexact(() -> previewClient.search(new SearchRequestAlpha1(STORE, INDEX,
          SearchQuery.matchAll().setFilter(Map.of("id", number)))));
    }
    verify(daprStub, never()).searchAlpha1(any(), any());
  }

  @Test
  @DisplayName("invalid input should fail through the Mono, not when the method is called")
  public void validationFailsThroughMonoTest() {
    Mono<SearchResponseAlpha1> search = previewClient.search(null);
    Mono<List<String>> list = previewClient.listSearchIndexes(null);

    assertThrows(IllegalArgumentException.class, () -> search.block(TIMEOUT));
    assertThrows(IllegalArgumentException.class, () -> list.block(TIMEOUT));
  }

  @Test
  @DisplayName("sort clauses should expose their field and direction")
  public void sortClauseFactoriesTest() {
    assertEquals("title", SortClause.asc("title").getField());
    assertEquals(SortOrder.ASC, SortClause.asc("title").getOrder());
    assertEquals(SortOrder.DESC, SortClause.desc("title").getOrder());
  }
}
