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
 * Request to create a collection in a vector store component.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class CreateVectorCollectionRequestAlpha1 {

  private final String storeName;
  private final String collection;
  private final int dimensions;
  private DistanceMetric metric = DistanceMetric.UNSPECIFIED;
  private Map<String, String> metadata;

  /**
   * Constructor for CreateVectorCollectionRequestAlpha1.
   *
   * @param storeName  Name of the vector store component. Required.
   * @param collection Name of the collection to create. Required.
   * @param dimensions Length of every vector in the collection. Must be positive.
   */
  public CreateVectorCollectionRequestAlpha1(String storeName, String collection, int dimensions) {
    this.storeName = storeName;
    this.collection = collection;
    this.dimensions = dimensions;
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
   * Gets the length of every vector in the collection.
   *
   * @return the dimensions
   */
  public int getDimensions() {
    return dimensions;
  }

  /**
   * Gets the distance metric of the collection.
   *
   * @return the metric
   */
  public DistanceMetric getMetric() {
    return metric;
  }

  /**
   * Sets the distance metric of the collection. The default, {@link DistanceMetric#UNSPECIFIED}, uses the
   * component's default metric. A metric the component cannot provide fails the call with INVALID_ARGUMENT.
   *
   * @param metric the metric
   * @return this request for method chaining
   */
  public CreateVectorCollectionRequestAlpha1 setMetric(DistanceMetric metric) {
    this.metric = metric;
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
  public CreateVectorCollectionRequestAlpha1 setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }
}
