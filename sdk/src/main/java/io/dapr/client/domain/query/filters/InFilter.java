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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InFilter<T> extends Filter<T> {
  @JsonValue
  private Map.Entry<String, List<T>> in;

  public InFilter() {
    super("IN");
  }

  public InFilter(String key, List<T> value) {
    super("IN");
    in = new AbstractMap.SimpleEntry<>(key, value);
  }

  @JsonCreator
  InFilter(Map.Entry<String, List<T>> in) {
    super("IN");
    this.in = in;
  }

  /**
   * constructor for InFilter.
   * @param key value of the key in the state store.
   * @param values var args values list.
   */
  public InFilter(String key, T... values) {
    super("IN");
    if (values == null || values.length == 0) {
      throw new IllegalArgumentException("list of values must be at least 1");
    }
    in = new AbstractMap.SimpleImmutableEntry<>(key, Collections.unmodifiableList(Arrays.asList(values)));
  }

  @JsonIgnore
  public String getKey() {
    return in != null ? in.getKey() : null;
  }

  @JsonIgnore
  public List<T> getValues() {
    return in != null ? in.getValue() : null;
  }

  @Override
  public String getRepresentation() {
    return this.getKey() + " IN ["
        + this.getValues().stream().map(Object::toString).collect(Collectors.joining(","))
        + "]";
  }

  @Override
  public Boolean isValid() {
    return in != null && in.getKey() != null && !in.getKey().isEmpty() && !in.getKey().trim().isEmpty()
        && in.getValue() != null && in.getValue().size() > 0;
  }
}
