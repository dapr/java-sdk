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
 * Request to search an index.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class SearchRequestAlpha1 {

  private final String storeName;
  private final String index;
  private final SearchQuery query;
  private Map<String, String> metadata;

  /**
   * Constructor for SearchRequestAlpha1.
   *
   * @param storeName Name of the search store component. Required.
   * @param index     Name of the index. Required.
   * @param query     The query. Null matches every document.
   */
  public SearchRequestAlpha1(String storeName, String index, SearchQuery query) {
    this.storeName = storeName;
    this.index = index;
    this.query = query;
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
   * Gets the query.
   *
   * @return the query, or null to match every document
   */
  public SearchQuery getQuery() {
    return query;
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
  public SearchRequestAlpha1 setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }
}
