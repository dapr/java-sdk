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

package io.dapr.client.domain.query.filters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AndFilter extends Filter<Void> {
  @JsonIgnore
  private final List<Filter<?>> and;

  public AndFilter() {
    super("AND");
    this.and = new ArrayList<>();
  }

  @JsonCreator
  AndFilter(Filter<?>[] filters) {
    super("AND");
    this.and = Arrays.asList(filters);
  }

  public <V extends Filter<?>> AndFilter addClause(V filter) {
    this.and.add(filter);
    return this;
  }

  @JsonValue
  public Filter<?>[] getClauses() {
    return this.and.toArray(new Filter[0]);
  }

  @Override
  @JsonIgnore
  public String getRepresentation() {
    return this.and.stream().map(Filter::getRepresentation).collect(Collectors.joining(" AND "));
  }

  @Override
  public Boolean isValid() {
    boolean validAnd = and != null && and.size() >= 2;
    if (validAnd) {
      for (Filter<?> filter : and) {
        if (!filter.isValid()) {
          return false;
        }
      }
    }
    return validAnd;
  }
}
