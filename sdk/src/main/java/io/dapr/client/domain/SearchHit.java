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
 * A document that matched a search, with its score and highlights.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class SearchHit {

  private final SearchDocument document;
  private final double score;
  private final Map<String, String> highlights;

  /**
   * Constructor for SearchHit.
   *
   * @param document   The matching document.
   * @param score      The relevance score.
   * @param highlights Highlighted fragments, by field name.
   */
  public SearchHit(SearchDocument document, double score, Map<String, String> highlights) {
    this.document = document;
    this.score = score;
    this.highlights = Map.copyOf(highlights);
  }

  /**
   * Gets the matching document.
   *
   * @return the document
   */
  public SearchDocument getDocument() {
    return document;
  }

  /**
   * Gets the relevance score. Scores are specific to the store. Higher is more relevant.
   *
   * @return the score
   */
  public double getScore() {
    return score;
  }

  /**
   * Gets the highlighted fragments, by field name.
   *
   * @return the highlights, empty when the query did not ask for any
   */
  public Map<String, String> getHighlights() {
    return highlights;
  }
}
