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

package io.dapr.examples.search;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
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
import io.dapr.config.Properties;
import io.dapr.config.Property;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stores, reads, searches, and deletes documents with the Dapr search API (alpha).
 *
 * <p>Run with the in-memory search store component in components/search.
 */
public class SearchExample {

  private static final String STORE_NAME = "searchstore";
  private static final String INDEX = "books";

  /**
   * The main method of the example.
   *
   * @param args Command line arguments (unused).
   * @throws Exception if the client fails to close.
   */
  public static void main(String[] args) throws Exception {
    Map<Property<?>, String> overrides = Map.of(
        Properties.HTTP_PORT, "3500",
        Properties.GRPC_PORT, "50001"
    );

    try (DaprPreviewClient client = new DaprClientBuilder().withPropertyOverrides(overrides).buildPreviewClient()) {
      System.out.println("=== Dapr Search Example ===");

      client.createSearchIndex(STORE_NAME, INDEX).block();
      System.out.println("Created index: " + INDEX);

      List<SearchDocument> books = List.of(
          book("dune", "{\"title\":\"Dune\",\"genre\":\"scifi\",\"year\":1965}"),
          book("foundation", "{\"title\":\"Foundation\",\"genre\":\"scifi\",\"year\":1951}"),
          book("hobbit", "{\"title\":\"The Hobbit\",\"genre\":\"fantasy\",\"year\":1937}"));
      IndexSearchDocumentsResponseAlpha1 indexed = client.indexSearchDocuments(
          new IndexSearchDocumentsRequestAlpha1(STORE_NAME, INDEX, books)
              .setOptions(IndexingOptions.waitForCompletion(
                  Duration.ofSeconds(5), IndexingWaitTimeoutAction.FAIL_REQUEST))).block();
      System.out.println("Stored books: " + books.size() + ", " + indexed.getAck()
          + ", failed: " + indexed.getFailedItems().size());

      GetSearchIndexResponseAlpha1 index = client.getSearchIndex(STORE_NAME, INDEX).block();
      System.out.println("Index " + index.getIndex() + ": " + index.getDocumentCount() + " documents");

      SearchResponseAlpha1 text = client.search(
          new SearchRequestAlpha1(STORE_NAME, INDEX, SearchQuery.byText("hobbit"))).block();
      System.out.println("Full-text search: " + ids(text));

      SearchResponseAlpha1 scifi = client.search(new SearchRequestAlpha1(STORE_NAME, INDEX,
          SearchQuery.matchAll()
              .setFilter(Map.of("genre", Map.of("$eq", "scifi")))
              .setSort(List.of(SortClause.asc("year"))))).block();
      System.out.println("Science fiction, oldest first: " + ids(scifi));

      // A continuation token is bound to its query, so each page sends the same query with the new token.
      String token = null;
      int page = 1;
      do {
        SearchResponseAlpha1 response = client.search(new SearchRequestAlpha1(STORE_NAME, INDEX,
            SearchQuery.matchAll().setTopK(2).setContinuationToken(token))).block();
        System.out.println("Page " + page + " of " + response.getTotalHits() + " books: " + ids(response));
        token = response.getContinuationToken();
        page++;
      } while (token != null);

      List<SearchDocument> found = client.getSearchDocuments(
          new GetSearchDocumentsRequestAlpha1(STORE_NAME, INDEX, List.of("dune", "missing"))
              .setIncludeContent(true)).block();
      for (SearchDocument document : found) {
        String content = new String(document.getContent(), StandardCharsets.UTF_8);
        System.out.println("Read " + document.getId() + ": " + content);
      }

      IndexAck deleted = client.deleteSearchDocuments(
          new DeleteSearchDocumentsRequestAlpha1(STORE_NAME, INDEX, List.of("dune"))).block();
      System.out.println("Deleted document dune: " + deleted);

      client.deleteSearchIndex(STORE_NAME, INDEX).block();
      System.out.println("Deleted index: " + INDEX);
      System.out.println("SUCCESS: The search example finished.");
    }
  }

  private static SearchDocument book(String id, String json) {
    return new SearchDocument(id, json.getBytes(StandardCharsets.UTF_8));
  }

  private static List<String> ids(SearchResponseAlpha1 response) {
    return response.getHits().stream()
        .map(SearchHit::getDocument)
        .map(SearchDocument::getId)
        .collect(Collectors.toList());
  }
}
