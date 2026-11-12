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
 * Request to insert or replace records in a vector collection, by record ID.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class UpsertVectorsRequestAlpha1 {

  private final String storeName;
  private final String collection;
  private final List<VectorRecord> records;
  private Map<String, String> metadata;
  private IndexingOptions options;

  /**
   * Constructor for UpsertVectorsRequestAlpha1.
   *
   * @param storeName  Name of the vector store component. Required.
   * @param collection Name of the collection. Required.
   * @param records    Records to store. Required. Each needs an ID and a vector.
   */
  public UpsertVectorsRequestAlpha1(String storeName, String collection, List<VectorRecord> records) {
    this.storeName = storeName;
    this.collection = collection;
    this.records = records;
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
   * Gets the records to store.
   *
   * @return the records
   */
  public List<VectorRecord> getRecords() {
    return records;
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
  public UpsertVectorsRequestAlpha1 setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }

  /**
   * Gets when the write returns.
   *
   * @return the indexing options, or null to return once the store accepts the write
   */
  public IndexingOptions getOptions() {
    return options;
  }

  /**
   * Sets when the write returns. Without options, it returns once the store accepts the write.
   *
   * @param options the indexing options
   * @return this request for method chaining
   */
  public UpsertVectorsRequestAlpha1 setOptions(IndexingOptions options) {
    this.options = options;
    return this;
  }
}
