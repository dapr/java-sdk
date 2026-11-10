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

/**
 * Matches of a vector query, in the order the vector store returned them.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class QueryVectorsResponseAlpha1 {

  private final List<VectorMatch> matches;
  private final DistanceMetric metric;

  /**
   * Constructor for QueryVectorsResponseAlpha1.
   *
   * @param matches The matching records with their scores.
   * @param metric  The metric that produced the scores.
   */
  public QueryVectorsResponseAlpha1(List<VectorMatch> matches, DistanceMetric metric) {
    this.matches = List.copyOf(matches);
    this.metric = metric;
  }

  /**
   * Gets the matching records with their scores.
   *
   * @return the matches
   */
  public List<VectorMatch> getMatches() {
    return matches;
  }

  /**
   * Gets the metric that produced the scores. It is never {@link DistanceMetric#UNSPECIFIED}.
   *
   * @return the metric
   */
  public DistanceMetric getMetric() {
    return metric;
  }
}
