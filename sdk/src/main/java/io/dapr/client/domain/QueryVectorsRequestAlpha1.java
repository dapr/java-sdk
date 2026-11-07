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

import java.util.Map;

/**
 * Request to find the records in a vector collection that are closest to a query.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class QueryVectorsRequestAlpha1 {

  private final String storeName;
  private final String collection;
  private final VectorQuery query;
  private Map<String, String> metadata;

  /**
   * Constructor for QueryVectorsRequestAlpha1.
   *
   * @param storeName  Name of the vector store component. Required.
   * @param collection Name of the collection. Required.
   * @param query      The query. Required.
   */
  public QueryVectorsRequestAlpha1(String storeName, String collection, VectorQuery query) {
    this.storeName = storeName;
    this.collection = collection;
    this.query = query;
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
   * Gets the query.
   *
   * @return the query
   */
  public VectorQuery getQuery() {
    return query;
  }

  /**
   * Gets the metadata passed to the vector store component.
   *
   * @return the metadata, or null when there is none
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }

  /**
   * Sets metadata passed to the vector store component.
   *
   * @param metadata the metadata
   * @return this request for method chaining
   */
  public QueryVectorsRequestAlpha1 setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }
}
