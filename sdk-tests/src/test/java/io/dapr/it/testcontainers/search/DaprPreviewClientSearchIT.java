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

package io.dapr.it.testcontainers.search;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.DeleteSearchDocumentsRequestAlpha1;
import io.dapr.client.domain.FailedItem;
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
import io.dapr.client.domain.TotalHitsRelation;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.MetadataEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Dapr search Alpha1 API.
 */
@Testcontainers
@Tag("testcontainers")
public class DaprPreviewClientSearchIT {

  private static final String STORE_NAME = "searchstore";

  // Applies every write a few seconds late, so that writes come back as queued and short waits time out.
  private static final String DELAYED_STORE_NAME = "delayedsearchstore";

  private static final IndexingOptions WAIT = IndexingOptions.waitForCompletion(
      Duration.ofSeconds(30), IndexingWaitTimeoutAction.FAIL_REQUEST);

  private static final byte[] DUNE = json("{\"title\":\"Dune\",\"genre\":\"scifi\",\"year\":1965}");

  private static final byte[] FOUNDATION = json("{\"title\":\"Foundation\",\"genre\":\"scifi\",\"year\":1951}");

  private static final byte[] HOBBIT = json("{\"title\":\"The Hobbit\",\"genre\":\"fantasy\",\"year\":1937}");

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("search-test-app")
      .withComponent(new Component(STORE_NAME, "search.in-memory", "v1", Collections.emptyList()))
      .withComponent(new Component(DELAYED_STORE_NAME, "search.in-memory", "v1",
          List.of(new MetadataEntry("indexingDelay", "3s"))));

  private static DaprPreviewClient daprPreviewClient;

  @BeforeAll
  static void setUp() {
    daprPreviewClient = new DaprClientBuilder()
        .withPropertyOverride(Properties.HTTP_ENDPOINT, DAPR_CONTAINER.getHttpEndpoint())
        .withPropertyOverride(Properties.GRPC_ENDPOINT, DAPR_CONTAINER.getGrpcEndpoint())
        .buildPreviewClient();
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (daprPreviewClient != null) {
      daprPreviewClient.close();
    }
  }

  @Test
  public void shouldCreateGetListAndDeleteIndex() {
    String index = createIndex(STORE_NAME);

    GetSearchIndexResponseAlpha1 created = daprPreviewClient.getSearchIndex(STORE_NAME, index).block();
    assertEquals(index, created.getIndex());
    assertEquals(0L, created.getDocumentCount());
    assertTrue(daprPreviewClient.listSearchIndexes(STORE_NAME).block().contains(index));
    assertErrorCode("ALREADY_EXISTS", () -> daprPreviewClient.createSearchIndex(STORE_NAME, index).block());

    daprPreviewClient.deleteSearchIndex(STORE_NAME, index).block();

    assertFalse(daprPreviewClient.listSearchIndexes(STORE_NAME).block().contains(index));
    assertErrorCode("NOT_FOUND", () -> daprPreviewClient.getSearchIndex(STORE_NAME, index).block());
  }

  @Test
  public void shouldStoreAndReadDocumentsInRequestOrder() {
    String index = createBooksIndex();

    assertEquals(3L, daprPreviewClient.getSearchIndex(STORE_NAME, index).block().getDocumentCount());

    List<SearchDocument> documents = daprPreviewClient.getSearchDocuments(
        new GetSearchDocumentsRequestAlpha1(STORE_NAME, index, List.of("hobbit", "missing", "dune"))
            .setIncludeContent(true)).block();
    assertEquals(List.of("hobbit", "dune"), documentIds(documents));
    assertArrayEquals(HOBBIT, documents.get(0).getContent());
    assertArrayEquals(DUNE, documents.get(1).getContent());
    assertEquals(Map.of("shelf", "b2"), documents.get(1).getMetadata());
    assertNull(documents.get(0).getMetadata());

    List<SearchDocument> withoutContent = daprPreviewClient.getSearchDocuments(
        new GetSearchDocumentsRequestAlpha1(STORE_NAME, index, List.of("dune"))).block();
    assertNull(withoutContent.get(0).getContent());
    assertEquals(Map.of("shelf", "b2"), withoutContent.get(0).getMetadata());
  }

  @Test
  public void shouldReportContentThatIsNotJsonAsFailedItem() {
    String index = createIndex(STORE_NAME);

    IndexSearchDocumentsResponseAlpha1 response = daprPreviewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(STORE_NAME, index, List.of(
            new SearchDocument("dune", DUNE),
            new SearchDocument("notjson", json("not json")),
            new SearchDocument("array", json("[1,2]"))))
            .setOptions(WAIT)).block();

    assertEquals(IndexAck.COMPLETED, response.getAck());
    assertEquals(List.of("notjson", "array"),
        response.getFailedItems().stream().map(FailedItem::getId).collect(Collectors.toList()));
    for (FailedItem failedItem : response.getFailedItems()) {
      assertEquals("INVALID_ARGUMENT", failedItem.getError().getErrorCode());
    }
    assertEquals(List.of("dune"), documentIds(daprPreviewClient.getSearchDocuments(
        new GetSearchDocumentsRequestAlpha1(STORE_NAME, index, List.of("dune", "notjson", "array"))).block()));
  }

  @Test
  public void shouldRejectDuplicateIds() {
    String index = createIndex(STORE_NAME);

    assertErrorCode("INVALID_ARGUMENT", () -> daprPreviewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(STORE_NAME, index, List.of(
            new SearchDocument("dune", DUNE),
            new SearchDocument("dune", FOUNDATION)))).block());
  }

  @Test
  public void shouldSearchByTextFilterAndSort() {
    String index = createBooksIndex();

    SearchResponseAlpha1 byText = search(index, SearchQuery.byText("hobbit"));
    assertEquals(List.of("hobbit"), hitIds(byText));
    assertTrue(byText.getHits().get(0).getScore() > 0);
    assertEquals(1L, byText.getTotalHits());
    assertEquals(TotalHitsRelation.EXACT, byText.getTotalHitsRelation());
    assertNull(byText.getContinuationToken());
    assertNull(byText.getHits().get(0).getDocument().getContent());

    assertEquals(List.of(), hitIds(search(index, SearchQuery.byText("hobbit").setSearchFields(List.of("genre")))));

    assertEquals(List.of("dune", "foundation"), hitIds(search(index,
        SearchQuery.matchAll().setFilter(Map.of("genre", "scifi")))));
    assertEquals(List.of("foundation", "hobbit"), hitIds(search(index,
        SearchQuery.matchAll().setFilter(Map.of("year", Map.of("$in", Set.of(1951, 1937)))))));
    assertEquals(List.of("dune"), hitIds(search(index,
        SearchQuery.matchAll().setFilter(Map.of("year", Map.of("$gt", 1951))))));

    assertEquals(List.of("hobbit", "foundation", "dune"), hitIds(search(index,
        SearchQuery.matchAll().setSort(List.of(SortClause.asc("year"))))));
    assertEquals(List.of("dune", "foundation", "hobbit"), hitIds(search(index,
        SearchQuery.matchAll().setSort(List.of(SortClause.desc("year"))))));

    assertEquals(List.of("dune", "foundation", "hobbit"), hitIds(daprPreviewClient.search(
        new SearchRequestAlpha1(STORE_NAME, index, null)).block()));
  }

  @Test
  public void shouldReturnContentAndSelectedFields() {
    String index = createBooksIndex();

    SearchHit full = search(index, SearchQuery.byText("hobbit").setIncludeContent(true)).getHits().get(0);
    assertArrayEquals(HOBBIT, full.getDocument().getContent());

    SearchHit selected = search(index, SearchQuery.byText("hobbit")
        .setIncludeContent(true)
        .setReturnFields(List.of("title"))).getHits().get(0);
    assertEquals("{\"title\":\"The Hobbit\"}", new String(selected.getDocument().getContent(), StandardCharsets.UTF_8));
  }

  @Test
  public void shouldPageThroughHits() {
    String index = createBooksIndex();

    SearchResponseAlpha1 first = search(index, SearchQuery.matchAll().setTopK(2));
    assertEquals(List.of("dune", "foundation"), hitIds(first));
    assertEquals(3L, first.getTotalHits());
    assertNotNull(first.getContinuationToken());

    SearchResponseAlpha1 second = search(index,
        SearchQuery.matchAll().setTopK(2).setContinuationToken(first.getContinuationToken()));
    assertEquals(List.of("hobbit"), hitIds(second));
    assertNull(second.getContinuationToken());

    assertErrorCode("INVALID_ARGUMENT", () -> search(index,
        SearchQuery.byText("dune").setContinuationToken(first.getContinuationToken())));
  }

  @Test
  public void shouldSendFeaturesTheStoreDoesNotSupport() {
    String index = createIndex(STORE_NAME);

    assertErrorCode("UNIMPLEMENTED", () -> search(index, SearchQuery.byNative(Map.of("match", "dune"))));
    assertErrorCode("UNIMPLEMENTED", () -> search(index, SearchQuery.matchAll().setHighlightFields(List.of("title"))));
  }

  @Test
  public void shouldDeleteDocuments() {
    String index = createBooksIndex();

    IndexAck ack = daprPreviewClient.deleteSearchDocuments(
        new DeleteSearchDocumentsRequestAlpha1(STORE_NAME, index, List.of("dune", "missing")).setOptions(WAIT)).block();

    assertEquals(IndexAck.COMPLETED, ack);
    assertEquals(List.of("foundation", "hobbit"), documentIds(daprPreviewClient.getSearchDocuments(
        new GetSearchDocumentsRequestAlpha1(STORE_NAME, index, List.of("dune", "foundation", "hobbit"))).block()));
  }

  @Test
  public void shouldFollowIndexingOptionsOnQueuedWrites() {
    String index = createIndex(DELAYED_STORE_NAME);
    List<SearchDocument> documents = List.of(new SearchDocument("dune", DUNE));
    Duration shortWait = Duration.ofMillis(200);

    assertEquals(IndexAck.QUEUED, daprPreviewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(DELAYED_STORE_NAME, index, documents)).block().getAck());
    assertEquals(IndexAck.QUEUED, daprPreviewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(DELAYED_STORE_NAME, index, documents)
            .setOptions(IndexingOptions.returnOnAcceptance())).block().getAck());
    assertEquals(IndexAck.QUEUED, daprPreviewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(DELAYED_STORE_NAME, index, documents)
            .setOptions(IndexingOptions.waitForCompletion(shortWait, IndexingWaitTimeoutAction.CONTINUE_ASYNC)))
        .block().getAck());
    assertErrorCode("DEADLINE_EXCEEDED", () -> daprPreviewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(DELAYED_STORE_NAME, index, documents)
            .setOptions(IndexingOptions.waitForCompletion(shortWait, IndexingWaitTimeoutAction.FAIL_REQUEST)))
        .block());
    assertEquals(IndexAck.COMPLETED, daprPreviewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(DELAYED_STORE_NAME, index, documents).setOptions(WAIT)).block().getAck());
  }

  private static String createIndex(String storeName) {
    String index = "books-" + UUID.randomUUID();
    daprPreviewClient.createSearchIndex(storeName, index).block();
    return index;
  }

  private static String createBooksIndex() {
    String index = createIndex(STORE_NAME);
    IndexSearchDocumentsResponseAlpha1 response = daprPreviewClient.indexSearchDocuments(
        new IndexSearchDocumentsRequestAlpha1(STORE_NAME, index, List.of(
            new SearchDocument("dune", DUNE).setMetadata(Map.of("shelf", "b2")),
            new SearchDocument("foundation", FOUNDATION),
            new SearchDocument("hobbit", HOBBIT)))
            .setOptions(WAIT)).block();
    assertEquals(IndexAck.COMPLETED, response.getAck());
    assertTrue(response.getFailedItems().isEmpty());
    return index;
  }

  private static SearchResponseAlpha1 search(String index, SearchQuery query) {
    return daprPreviewClient.search(new SearchRequestAlpha1(STORE_NAME, index, query)).block();
  }

  private static List<String> hitIds(SearchResponseAlpha1 response) {
    return response.getHits().stream()
        .map(SearchHit::getDocument)
        .map(SearchDocument::getId)
        .collect(Collectors.toList());
  }

  private static List<String> documentIds(List<SearchDocument> documents) {
    return documents.stream().map(SearchDocument::getId).collect(Collectors.toList());
  }

  private static void assertErrorCode(String code, Runnable call) {
    assertEquals(code, assertThrows(DaprException.class, call::run).getErrorCode());
  }

  private static byte[] json(String json) {
    return json.getBytes(StandardCharsets.UTF_8);
  }
}
