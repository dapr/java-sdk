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
 * A record in a vector collection: an ID, a dense vector, and optional payload and metadata.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class VectorRecord {

  private final String id;
  private final float[] values;
  private byte[] payload;
  private Map<String, Object> metadata;

  /**
   * Constructor for VectorRecord.
   *
   * @param id     ID of the record. Required.
   * @param values dense vector. Required. Its length must equal the collection's dimensions.
   */
  public VectorRecord(String id, float[] values) {
    this.id = id;
    this.values = values;
  }

  /**
   * Gets the ID of the record.
   *
   * @return the record ID
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the dense vector.
   *
   * @return the vector, empty when a read did not ask for values
   */
  public float[] getValues() {
    return values;
  }

  /**
   * Gets the opaque payload stored with the record.
   *
   * @return the payload, or null when there is none or a read did not ask for it
   */
  public byte[] getPayload() {
    return payload;
  }

  /**
   * Sets an opaque payload to store with the record. The store returns it unchanged and cannot filter on it.
   *
   * @param payload the payload
   * @return this record for method chaining
   */
  public VectorRecord setPayload(byte[] payload) {
    this.payload = payload;
    return this;
  }

  /**
   * Gets the metadata of the record.
   *
   * <p>Numbers come back as {@link Double}, because a protobuf Struct keeps every number as a double.
   *
   * @return the metadata, or null when there is none
   */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  /**
   * Sets structured attributes that queries can filter on.
   *
   * <p>Values can be null, a Boolean, a Number, a String, a Collection or a Map with string keys. Other types
   * are rejected with an {@link IllegalArgumentException} when the record is sent.
   *
   * @param metadata the metadata
   * @return this record for method chaining
   */
  public VectorRecord setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
    return this;
  }
}
