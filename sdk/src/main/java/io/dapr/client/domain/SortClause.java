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
 * One sort key of a search query.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class SortClause {

  private final String field;
  private final SortOrder order;

  private SortClause(String field, SortOrder order) {
    this.field = field;
    this.order = order;
  }

  /**
   * Creates a clause that sorts by a field, lowest values first.
   *
   * @param field Top-level or dotted nested key of the document content. Must not be blank.
   * @return the clause
   */
  public static SortClause asc(String field) {
    return new SortClause(field, SortOrder.ASC);
  }

  /**
   * Creates a clause that sorts by a field, highest values first.
   *
   * @param field Top-level or dotted nested key of the document content. Must not be blank.
   * @return the clause
   */
  public static SortClause desc(String field) {
    return new SortClause(field, SortOrder.DESC);
  }

  /**
   * Gets the field to sort by.
   *
   * @return the field
   */
  public String getField() {
    return field;
  }

  /**
   * Gets the sort direction.
   *
   * @return the direction
   */
  public SortOrder getOrder() {
    return order;
  }
}
