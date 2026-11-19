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
 * Request to delete documents from a search index by ID. IDs that do not exist are not an error.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class DeleteSearchDocumentsRequestAlpha1 {

  private final String storeName;
  private final String index;
  private final List<String> ids;
  private Map<String, String> metadata;
  private IndexingOptions options;

  /**
   * Constructor for DeleteSearchDocumentsRequestAlpha1.
   *
   * @param storeName Name of the search store component. Required.
   * @param index     Name of the index. Required.
   * @param ids       IDs of the documents to delete. Required.
   */
  public DeleteSearchDocumentsRequestAlpha1(String storeName, String index, List<String> ids) {
    this.storeName = storeName;
    this.index = index;
    this.ids = ids;
  }

  /**
   * Gets the search store component name.
   *
   * @return the store name
   */
  public String getStoreName() {
    return storeName;
  }

  /**
   * Gets the index name.
   *
   * @return the index name
   */
  public String getIndex() {
    return index;
  }

  /**
   * Gets the IDs of the documents to delete.
   *
   * @return the document IDs
   */
  public List<String> getIds() {
    return ids;
  }

  /**
   * Gets the metadata passed to the search store component.
   *
   * @return the metadata, or null when there is none
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }

  /**
   * Sets metadata passed to the search store component.
   *
   * @param metadata the metadata
   * @return this request for method chaining
   */
  public DeleteSearchDocumentsRequestAlpha1 setMetadata(Map<String, String> metadata) {
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
  public DeleteSearchDocumentsRequestAlpha1 setOptions(IndexingOptions options) {
    this.options = options;
    return this;
  }
}
