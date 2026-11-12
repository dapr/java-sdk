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

package io.dapr.client.domain;

import java.util.List;
import java.util.Map;

/**
 * Request to run several queries against one vector collection in one call.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class BatchQueryVectorsRequestAlpha1 {

  private final String storeName;
  private final String collection;
  private final List<VectorQuery> queries;
  private Map<String, String> metadata;

  /**
   * Constructor for BatchQueryVectorsRequestAlpha1.
   *
   * @param storeName  Name of the vector store component. Required.
   * @param collection Name of the collection. Required.
   * @param queries    The queries. Required.
   */
  public BatchQueryVectorsRequestAlpha1(String storeName, String collection, List<VectorQuery> queries) {
    this.storeName = storeName;
    this.collection = collection;
    this.queries = queries;
  }

  /**
   * Gets the vector store component name.
   *
   * @return the store name
   */
  public String getStoreName() {
    return storeName;
  }

  /**
   * Gets the collection name.
   *
   * @return the collection name
   */
  public String getCollection() {
    return collection;
  }

  /**
   * Gets the queries.
   *
   * @return the queries
   */
  public List<VectorQuery> getQueries() {
    return queries;
  }

  /**
   * Gets the metadata passed to the vector store component for every query.
   *
   * @return the metadata, or null when there is none
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }

  /**
   * Sets metadata passed to the vector store component for every query.
   *
   * @param metadata the metadata
   * @return this request for method chaining
   */
  public BatchQueryVectorsRequestAlpha1 setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }
}
