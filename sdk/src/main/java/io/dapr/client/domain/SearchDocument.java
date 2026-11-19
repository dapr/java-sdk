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
 * A document in a search index: an ID, its content as a JSON object, and optional metadata.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class SearchDocument {

  private final String id;
  private final byte[] content;
  private Map<String, String> metadata;

  /**
   * Constructor for SearchDocument.
   *
   * @param id      ID of the document, unique within the index. Required.
   * @param content UTF-8 encoded JSON object. Required. The sidecar reports content that is not a JSON object
   *                as a failed item of the write.
   */
  public SearchDocument(String id, byte[] content) {
    this.id = id;
    this.content = content;
  }

  /**
   * Gets the ID of the document.
   *
   * @return the document ID
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the content of the document.
   *
   * @return the UTF-8 encoded JSON object, or null when a read did not ask for it
   */
  public byte[] getContent() {
    return content;
  }

  /**
   * Gets the metadata stored with the document.
   *
   * @return the metadata, or null when there is none
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }

  /**
   * Sets opaque metadata to store with the document. The store returns it unchanged. It is not indexed, and
   * queries cannot filter on it.
   *
   * @param metadata the metadata
   * @return this document for method chaining
   */
  public SearchDocument setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }
}
