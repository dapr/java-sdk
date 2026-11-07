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
 * Distance metric of a vector collection or a vector query.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public enum DistanceMetric {

  /**
   * When creating a collection, the component's default metric. In a query, the collection's metric.
   */
  UNSPECIFIED,

  /**
   * Cosine similarity. Higher scores are closer matches.
   */
  COSINE,

  /**
   * Dot-product similarity. Higher scores are closer matches.
   */
  DOT_PRODUCT,

  /**
   * Euclidean distance. Lower scores are closer matches.
   */
  EUCLIDEAN
}
