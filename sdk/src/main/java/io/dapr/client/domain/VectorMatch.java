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

/**
 * A record that matched a vector query, with its score.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class VectorMatch {

  private final VectorRecord record;
  private final double score;

  /**
   * Constructor for VectorMatch.
   *
   * @param record the matching record.
   * @param score  the score under the query's metric.
   */
  public VectorMatch(VectorRecord record, double score) {
    this.record = record;
    this.score = score;
  }

  /**
   * Gets the matching record.
   *
   * @return the record
   */
  public VectorRecord getRecord() {
    return record;
  }

  /**
   * Gets the score. Higher is closer for cosine and dot product; lower is closer for Euclidean distance.
   *
   * @return the score
   */
  public double getScore() {
    return score;
  }
}
