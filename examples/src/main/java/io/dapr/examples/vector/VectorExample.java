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

package io.dapr.examples.vector;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
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
import io.dapr.client.domain.VectorMatch;
import io.dapr.client.domain.VectorQuery;
import io.dapr.client.domain.VectorRecord;
import io.dapr.config.Properties;
import io.dapr.config.Property;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stores, reads, queries, and deletes vectors with the Dapr vector API (alpha).
 *
 * <p>Run with the in-memory vector store component in components/vector.
 */
public class VectorExample {

  private static final String STORE_NAME = "vectorstore";
  private static final String COLLECTION = "books";

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
      System.out.println("=== Dapr Vector Example ===");

      client.createVectorCollection(new CreateVectorCollectionRequestAlpha1(STORE_NAME, COLLECTION, 3)
          .setMetric(DistanceMetric.COSINE)).block();
      System.out.println("Created collection: " + COLLECTION);

      // The vectors are made up; an app would get them from an embedding model.
      List<VectorRecord> books = List.of(
          new VectorRecord("dune", new float[] {0.9f, 0.1f, 0.0f}).setMetadata(Map.of("genre", "scifi")),
          new VectorRecord("foundation", new float[] {0.8f, 0.2f, 0.1f}).setMetadata(Map.of("genre", "scifi")),
          new VectorRecord("hobbit", new float[] {0.1f, 0.9f, 0.2f}).setMetadata(Map.of("genre", "fantasy")));
      UpsertVectorsResponseAlpha1 upsert = client.upsertVectors(
          new UpsertVectorsRequestAlpha1(STORE_NAME, COLLECTION, books)
              .setOptions(IndexingOptions.waitForCompletion(
                  Duration.ofSeconds(5), IndexingWaitTimeoutAction.FAIL_REQUEST))).block();
      System.out.println("Stored books: " + books.size() + ", " + upsert.getAck()
          + ", failed: " + upsert.getFailedItems().size());

      GetVectorCollectionResponseAlpha1 collection = client.getVectorCollection(STORE_NAME, COLLECTION).block();
      System.out.println("Collection " + collection.getCollection() + ": " + collection.getDimensions()
          + " dimensions, " + collection.getMetric());

      float[] scifiVector = {0.85f, 0.15f, 0.05f};
      QueryVectorsResponseAlpha1 closest = client.queryVectors(new QueryVectorsRequestAlpha1(
          STORE_NAME, COLLECTION, VectorQuery.byVector(scifiVector).setTopK(2))).block();
      System.out.println("Closest two books: " + ids(closest));

      QueryVectorsResponseAlpha1 fantasy = client.queryVectors(new QueryVectorsRequestAlpha1(
          STORE_NAME, COLLECTION, VectorQuery.byVector(scifiVector)
              .setFilter(Map.of("genre", Map.of("$eq", "fantasy"))))).block();
      System.out.println("Closest fantasy book: " + ids(fantasy));

      // Each query in a batch succeeds or fails on its own.
      List<BatchQueryVectorsResultAlpha1> batch = client.batchQueryVectors(new BatchQueryVectorsRequestAlpha1(
          STORE_NAME, COLLECTION, List.of(
              VectorQuery.byId("hobbit").setTopK(1),
              VectorQuery.byId("missing")))).block();
      for (int i = 0; i < batch.size(); i++) {
        BatchQueryVectorsResultAlpha1 result = batch.get(i);
        if (result.getError() != null) {
          System.out.println("Batch query " + i + " failed: " + result.getError().getErrorCode());
        } else {
          System.out.println("Batch query " + i + ": " + ids(result.getResponse()));
        }
      }

      List<VectorRecord> found = client.getVectors(
          new GetVectorsRequestAlpha1(STORE_NAME, COLLECTION, List.of("dune", "missing"))).block();
      System.out.println("Read records: " + found.stream().map(VectorRecord::getId).collect(Collectors.toList()));

      IndexAck deleted = client.deleteVectors(
          new DeleteVectorsRequestAlpha1(STORE_NAME, COLLECTION, List.of("dune"))).block();
      System.out.println("Deleted record dune: " + deleted);

      client.deleteVectorCollection(STORE_NAME, COLLECTION).block();
      System.out.println("Deleted collection: " + COLLECTION);
      System.out.println("SUCCESS: The vector example finished.");
    }
  }

  private static List<String> ids(QueryVectorsResponseAlpha1 response) {
    return response.getMatches().stream()
        .map(VectorMatch::getRecord)
        .map(VectorRecord::getId)
        .collect(Collectors.toList());
  }
}
