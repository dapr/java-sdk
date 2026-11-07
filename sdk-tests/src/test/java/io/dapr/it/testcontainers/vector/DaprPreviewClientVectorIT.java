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

package io.dapr.it.testcontainers.vector;

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
import io.dapr.exceptions.DaprException;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
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
import java.util.UUID;
import java.util.stream.Collectors;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Dapr vector Alpha1 API.
 */
@Testcontainers
@Tag("testcontainers")
public class DaprPreviewClientVectorIT {

  private static final String STORE_NAME = "vectorstore";

  private static final IndexingOptions WAIT = IndexingOptions.waitForCompletion(
      Duration.ofSeconds(5), IndexingWaitTimeoutAction.FAIL_REQUEST);

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("vector-test-app")
      .withComponent(new Component(STORE_NAME, "vector.in-memory", "v1", Collections.emptyList()));

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
  public void shouldCreateGetListAndDeleteCollection() {
    String collection = createCollection(DistanceMetric.EUCLIDEAN);

    GetVectorCollectionResponseAlpha1 created = daprPreviewClient.getVectorCollection(STORE_NAME, collection).block();
    assertEquals(collection, created.getCollection());
    assertEquals(3, created.getDimensions());
    assertEquals(DistanceMetric.EUCLIDEAN, created.getMetric());
    assertTrue(daprPreviewClient.listVectorCollections(STORE_NAME).block().contains(collection));

    daprPreviewClient.deleteVectorCollection(STORE_NAME, collection).block();

    assertFalse(daprPreviewClient.listVectorCollections(STORE_NAME).block().contains(collection));
    DaprException error = assertThrows(DaprException.class,
        () -> daprPreviewClient.getVectorCollection(STORE_NAME, collection).block());
    assertEquals("NOT_FOUND", error.getErrorCode());
  }

  @Test
  public void shouldStoreAndReadRecordsWithPayloadAndMetadata() {
    String collection = createCollection(DistanceMetric.COSINE);
    byte[] payload = "Arrakis".getBytes(StandardCharsets.UTF_8);
    VectorRecord dune = new VectorRecord("dune", new float[] {0.9f, 0.1f, 0.0f})
        .setPayload(payload)
        .setMetadata(Map.of("genre", "scifi", "year", 1965, "tags", List.of("classic", true)));

    UpsertVectorsResponseAlpha1 upsert = daprPreviewClient.upsertVectors(
        new UpsertVectorsRequestAlpha1(STORE_NAME, collection, List.of(dune)).setOptions(WAIT)).block();
    assertEquals(IndexAck.COMPLETED, upsert.getAck());
    assertTrue(upsert.getFailedItems().isEmpty());

    List<VectorRecord> records = daprPreviewClient.getVectors(
        new GetVectorsRequestAlpha1(STORE_NAME, collection, List.of("dune", "missing")).setIncludeValues(true))
        .block();

    assertEquals(1, records.size());
    VectorRecord read = records.get(0);
    assertEquals("dune", read.getId());
    assertArrayEquals(new float[] {0.9f, 0.1f, 0.0f}, read.getValues());
    assertArrayEquals(payload, read.getPayload());
    assertEquals("scifi", read.getMetadata().get("genre"));
    assertEquals(1965.0, read.getMetadata().get("year"));
    assertEquals(List.of("classic", true), read.getMetadata().get("tags"));

    IndexAck deleted = daprPreviewClient.deleteVectors(
        new DeleteVectorsRequestAlpha1(STORE_NAME, collection, List.of("dune")).setOptions(WAIT)).block();
    assertEquals(IndexAck.COMPLETED, deleted);
    assertTrue(daprPreviewClient.getVectors(new GetVectorsRequestAlpha1(STORE_NAME, collection, List.of("dune")))
        .block().isEmpty());
  }

  @Test
  public void shouldQueryByVectorByIdAndWithFilter() {
    String collection = createCollectionWithBooks();
    float[] scifiVector = {0.85f, 0.15f, 0.05f};

    QueryVectorsResponseAlpha1 closest = daprPreviewClient.queryVectors(new QueryVectorsRequestAlpha1(
        STORE_NAME, collection, VectorQuery.byVector(scifiVector).setTopK(2))).block();
    assertEquals(List.of("dune", "foundation"), ids(closest));
    assertEquals(DistanceMetric.COSINE, closest.getMetric());
    assertNull(closest.getMatches().get(0).getRecord().getPayload());

    QueryVectorsResponseAlpha1 fantasy = daprPreviewClient.queryVectors(new QueryVectorsRequestAlpha1(
        STORE_NAME, collection, VectorQuery.byVector(scifiVector)
            .setFilter(Map.of("genre", Map.of("$eq", "fantasy"))))).block();
    assertEquals(List.of("hobbit"), ids(fantasy));

    QueryVectorsResponseAlpha1 likeFoundation = daprPreviewClient.queryVectors(new QueryVectorsRequestAlpha1(
        STORE_NAME, collection, VectorQuery.byId("foundation").setTopK(2))).block();
    assertEquals(List.of("foundation", "dune"), ids(likeFoundation));
  }

  @Test
  public void shouldReportEachBatchQueryOnItsOwn() {
    String collection = createCollectionWithBooks();

    List<BatchQueryVectorsResultAlpha1> results = daprPreviewClient.batchQueryVectors(
        new BatchQueryVectorsRequestAlpha1(STORE_NAME, collection, List.of(
            VectorQuery.byVector(new float[] {0.1f, 0.9f, 0.2f}).setTopK(1),
            VectorQuery.byId("missing")))).block();

    assertEquals(2, results.size());
    assertNull(results.get(0).getError());
    assertEquals(List.of("hobbit"), ids(results.get(0).getResponse()));
    assertNull(results.get(1).getResponse());
    assertEquals("NOT_FOUND", results.get(1).getError().getErrorCode());
  }

  @Test
  public void shouldFailOnMissingCollection() {
    String collection = "missing-" + UUID.randomUUID();

    DaprException error = assertThrows(DaprException.class, () -> daprPreviewClient.queryVectors(
        new QueryVectorsRequestAlpha1(STORE_NAME, collection, VectorQuery.byVector(new float[] {1f, 0f, 0f})))
        .block());

    assertEquals("NOT_FOUND", error.getErrorCode());
  }

  private static String createCollection(DistanceMetric metric) {
    String collection = "books-" + UUID.randomUUID();
    daprPreviewClient.createVectorCollection(
        new CreateVectorCollectionRequestAlpha1(STORE_NAME, collection, 3).setMetric(metric)).block();
    return collection;
  }

  private static String createCollectionWithBooks() {
    String collection = createCollection(DistanceMetric.COSINE);
    List<VectorRecord> books = List.of(
        new VectorRecord("dune", new float[] {0.9f, 0.1f, 0.0f}).setMetadata(Map.of("genre", "scifi")),
        new VectorRecord("foundation", new float[] {0.8f, 0.2f, 0.1f}).setMetadata(Map.of("genre", "scifi")),
        new VectorRecord("hobbit", new float[] {0.1f, 0.9f, 0.2f}).setMetadata(Map.of("genre", "fantasy")));
    daprPreviewClient.upsertVectors(
        new UpsertVectorsRequestAlpha1(STORE_NAME, collection, books).setOptions(WAIT)).block();
    return collection;
  }

  private static List<String> ids(QueryVectorsResponseAlpha1 response) {
    return response.getMatches().stream()
        .map(VectorMatch::getRecord)
        .map(VectorRecord::getId)
        .collect(Collectors.toList());
  }
}
