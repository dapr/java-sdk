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
 * Request to read records from a vector collection by ID.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class GetVectorsRequestAlpha1 {

  private final String storeName;
  private final String collection;
  private final List<String> ids;
  private Boolean includeValues;
  private Map<String, String> metadata;

  /**
   * Constructor for GetVectorsRequestAlpha1.
   *
   * @param storeName  Name of the vector store component. Required.
   * @param collection Name of the collection. Required.
   * @param ids        IDs of the records to read. Required.
   */
  public GetVectorsRequestAlpha1(String storeName, String collection, List<String> ids) {
    this.storeName = storeName;
    this.collection = collection;
    this.ids = ids;
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
   * Gets the IDs of the records to read.
   *
   * @return the record IDs
   */
  public List<String> getIds() {
    return ids;
  }

  /**
   * Gets whether the records include their vectors.
   *
   * @return true to include vectors, or null for the default (false)
   */
  public Boolean getIncludeValues() {
    return includeValues;
  }

  /**
   * Sets whether the records include their vectors. The default is false.
   *
   * @param includeValues true to include vectors
   * @return this request for method chaining
   */
  public GetVectorsRequestAlpha1 setIncludeValues(Boolean includeValues) {
    this.includeValues = includeValues;
    return this;
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
  public GetVectorsRequestAlpha1 setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }
}
