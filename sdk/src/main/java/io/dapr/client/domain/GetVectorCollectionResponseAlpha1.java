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
 * A collection in a vector store component, as returned by the store.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class GetVectorCollectionResponseAlpha1 {

  private final String collection;
  private final long recordCount;
  private final Map<String, String> properties;
  private final int dimensions;
  private final DistanceMetric metric;

  /**
   * Constructor for GetVectorCollectionResponseAlpha1.
   *
   * @param collection  Name of the collection.
   * @param recordCount Approximate number of records.
   * @param properties  Component-specific properties of the collection.
   * @param dimensions  Length of every vector in the collection.
   * @param metric      Distance metric of the collection.
   */
  public GetVectorCollectionResponseAlpha1(
      String collection, long recordCount, Map<String, String> properties, int dimensions, DistanceMetric metric) {
    this.collection = collection;
    this.recordCount = recordCount;
    this.properties = Map.copyOf(properties);
    this.dimensions = dimensions;
    this.metric = metric;
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
   * Gets the approximate number of records. Stores that cannot count records cheaply return 0.
   *
   * @return the record count
   */
  public long getRecordCount() {
    return recordCount;
  }

  /**
   * Gets the component-specific properties of the collection.
   *
   * @return the properties
   */
  public Map<String, String> getProperties() {
    return properties;
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
   * Gets the distance metric of the collection. It is never {@link DistanceMetric#UNSPECIFIED}.
   *
   * @return the metric
   */
  public DistanceMetric getMetric() {
    return metric;
  }
}
