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
 * An index in a search store component, as returned by the store.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class GetSearchIndexResponseAlpha1 {

  private final String index;
  private final long documentCount;
  private final Map<String, String> properties;

  /**
   * Constructor for GetSearchIndexResponseAlpha1.
   *
   * @param index         Name of the index.
   * @param documentCount Approximate number of documents.
   * @param properties    Component-specific properties of the index.
   */
  public GetSearchIndexResponseAlpha1(String index, long documentCount, Map<String, String> properties) {
    this.index = index;
    this.documentCount = documentCount;
    this.properties = Map.copyOf(properties);
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
   * Gets the approximate number of documents. Stores that cannot count documents cheaply return 0.
   *
   * @return the document count
   */
  public long getDocumentCount() {
    return documentCount;
  }

  /**
   * Gets the component-specific properties of the index.
   *
   * @return the properties
   */
  public Map<String, String> getProperties() {
    return properties;
  }
}
