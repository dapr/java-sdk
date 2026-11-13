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
 * One page of the hits of a search.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class SearchResponseAlpha1 {

  private final List<SearchHit> hits;
  private final Long totalHits;
  private final String continuationToken;
  private final TotalHitsRelation totalHitsRelation;

  /**
   * Constructor for SearchResponseAlpha1.
   *
   * @param hits              The hits of this page.
   * @param totalHits         The total number of hits, or null when the store did not report one.
   * @param continuationToken The token of the next page, or null when there are no more hits.
   * @param totalHitsRelation How accurate the total is.
   */
  public SearchResponseAlpha1(
      List<SearchHit> hits, Long totalHits, String continuationToken, TotalHitsRelation totalHitsRelation) {
    this.hits = List.copyOf(hits);
    this.totalHits = totalHits;
    this.continuationToken = continuationToken;
    this.totalHitsRelation = totalHitsRelation;
  }

  /**
   * Gets the hits of this page, in the order the store returned them.
   *
   * @return the hits
   */
  public List<SearchHit> getHits() {
    return hits;
  }

  /**
   * Gets the total number of hits across all pages. This is best effort: see {@link #getTotalHitsRelation()}.
   *
   * @return the total, or null when the store did not report one
   */
  public Long getTotalHits() {
    return totalHits;
  }

  /**
   * Gets the token of the next page, for {@link SearchQuery#setContinuationToken(String)}.
   *
   * @return the token, or null when there are no more hits
   */
  public String getContinuationToken() {
    return continuationToken;
  }

  /**
   * Gets how accurate the total is.
   *
   * @return the relation, {@link TotalHitsRelation#UNSPECIFIED} when the store reported no total or did not say how
   *     accurate it is
   */
  public TotalHitsRelation getTotalHitsRelation() {
    return totalHitsRelation;
  }
}
