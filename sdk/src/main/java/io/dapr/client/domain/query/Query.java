/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.client.domain.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dapr.client.domain.query.filters.Filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Query {
  private Filter<?> filter;

  @JsonProperty
  private Sorting[] sort = new Sorting[]{};

  @JsonProperty("page")
  private Pagination pagination = new Pagination();

  public Filter<?> getFilter() {
    return filter;
  }

  /**
   * Set the filter field in the instance.
   * @param filter Valid filter value.
   * @return this instance.
   */
  public Query setFilter(Filter<?> filter) {
    if (!filter.isValid()) {
      throw new IllegalArgumentException("the given filter is invalid configuration");
    }
    this.filter = filter;
    return this;
  }

  public List<Sorting> getSort() {
    return Collections.unmodifiableList(Arrays.asList(sort));
  }

  /**
   * Validate and set sorting field.
   *
   * @param sort List of sorting objects.
   * @return This instance.
   */
  public Query setSort(List<Sorting> sort) {
    if (sort == null || sort.size() == 0) {
      throw new IllegalArgumentException("Sorting list is null or empty");
    }
    this.sort =  sort.toArray(new Sorting[0]);
    return this;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public Query setPagination(Pagination pagination) {
    this.pagination = pagination;
    return this;
  }
}