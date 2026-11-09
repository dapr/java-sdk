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
 * A similarity query against a vector collection, by vector or by the ID of a stored record.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class VectorQuery {

  private final float[] vector;
  private final String byId;
  private Integer topK;
  private Map<String, Object> filter;
  private Boolean includeValues;
  private Boolean includePayload;
  private DistanceMetric metric = DistanceMetric.UNSPECIFIED;
  private Double scoreThreshold;

  private VectorQuery(float[] vector, String byId) {
    this.vector = vector;
    this.byId = byId;
  }

  /**
   * Creates a query for the records closest to a vector.
   *
   * @param vector the query vector. Must not be empty.
   * @return the query
   */
  public static VectorQuery byVector(float[] vector) {
    return new VectorQuery(vector, null);
  }

  /**
   * Creates a query for the records closest to a stored record.
   *
   * @param id ID of the stored record. Must not be blank.
   * @return the query
   */
  public static VectorQuery byId(String id) {
    return new VectorQuery(null, id);
  }

  /**
   * Gets the query vector.
   *
   * @return the vector, or null for a query by ID
   */
  public float[] getVector() {
    return vector;
  }

  /**
   * Gets the ID of the stored record to query by.
   *
   * @return the record ID, or null for a query by vector
   */
  public String getById() {
    return byId;
  }

  /**
   * Gets the maximum number of matches.
   *
   * @return the limit, or null for the component's default
   */
  public Integer getTopK() {
    return topK;
  }

  /**
   * Sets the maximum number of matches. 0 or null uses the component's default.
   *
   * @param topK the limit. Must not be negative.
   * @return this query for method chaining
   */
  public VectorQuery setTopK(Integer topK) {
    this.topK = topK;
    return this;
  }

  /**
   * Gets the filter on record metadata.
   *
   * @return the filter, or null when the query has none
   */
  public Map<String, Object> getFilter() {
    return filter;
  }

  /**
   * Sets a filter on record metadata, written in the Dapr filter format.
   *
   * <p>Values can be null, a Boolean, a Number, a String, a Collection or a Map with string keys. Other types
   * are rejected with an {@link IllegalArgumentException} when the query is sent, and so are numbers that a double
   * cannot hold exactly, such as a long above 2^53.
   *
   * @param filter the filter
   * @return this query for method chaining
   */
  public VectorQuery setFilter(Map<String, Object> filter) {
    this.filter = filter;
    return this;
  }

  /**
   * Gets whether matches include their vectors.
   *
   * @return true to include vectors, or null for the default (false)
   */
  public Boolean getIncludeValues() {
    return includeValues;
  }

  /**
   * Sets whether matches include their vectors. The default is false.
   *
   * @param includeValues true to include vectors
   * @return this query for method chaining
   */
  public VectorQuery setIncludeValues(Boolean includeValues) {
    this.includeValues = includeValues;
    return this;
  }

  /**
   * Gets whether matches include their payloads.
   *
   * @return true to include payloads, or null for the default (false)
   */
  public Boolean getIncludePayload() {
    return includePayload;
  }

  /**
   * Sets whether matches include their payloads. The default is false.
   *
   * @param includePayload true to include payloads
   * @return this query for method chaining
   */
  public VectorQuery setIncludePayload(Boolean includePayload) {
    this.includePayload = includePayload;
    return this;
  }

  /**
   * Gets the metric that scores the matches.
   *
   * @return the metric
   */
  public DistanceMetric getMetric() {
    return metric;
  }

  /**
   * Sets the metric that scores the matches. The default, {@link DistanceMetric#UNSPECIFIED}, uses the
   * collection's metric.
   *
   * @param metric the metric
   * @return this query for method chaining
   */
  public VectorQuery setMetric(DistanceMetric metric) {
    this.metric = metric;
    return this;
  }

  /**
   * Gets the score cutoff.
   *
   * @return the cutoff, or null when the query has none
   */
  public Double getScoreThreshold() {
    return scoreThreshold;
  }

  /**
   * Sets a score cutoff. Matches scoring at least this value are kept for cosine and dot product, and matches
   * scoring at most this value are kept for Euclidean distance.
   *
   * @param scoreThreshold the cutoff
   * @return this query for method chaining
   */
  public VectorQuery setScoreThreshold(Double scoreThreshold) {
    this.scoreThreshold = scoreThreshold;
    return this;
  }
}
