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

@SuppressWarnings("rawtypes")
public class OrFilter extends Filter {

  @JsonIgnore
  private List<Filter<?>> or;

  public OrFilter() {
    super("OR");
    this.or = new ArrayList<>();
  }

  @JsonCreator
  OrFilter(Filter<?>[] filters) {
    super("OR");
    this.or = Arrays.asList(filters);
  }

  public <V extends Filter> OrFilter addClause(V filter) {
    this.or.add(filter);
    return this;
  }

  @JsonValue
  public Filter<?>[] getClauses() {
    return this.or.toArray(new Filter[0]);
  }

  @Override
  @JsonIgnore
  public String getRepresentation() {
    return this.or.stream().map(Filter::getRepresentation).collect(Collectors.joining(" OR "));
  }

  @Override
  public Boolean isValid() {
    boolean validAnd = or != null && or.size() >= 2;
    if (validAnd) {
      for (Filter<?> filter : or) {
        if (!filter.isValid()) {
          return false;
        }
      }
    }
    return validAnd;
  }
}
