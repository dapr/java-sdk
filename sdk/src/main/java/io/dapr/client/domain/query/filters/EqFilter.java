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
import java.util.Map;

public class EqFilter<T> extends Filter<T> {

  @JsonValue
  private Map.Entry<String, T> eq;

  public EqFilter() {
    super("EQ");
  }

  public EqFilter(String key, T value) {
    super("EQ");
    eq = new AbstractMap.SimpleImmutableEntry<>(key, value);
  }

  @JsonCreator
  EqFilter(Map.Entry<String, T> eq) {
    super("EQ");
    this.eq = eq;
  }

  @JsonIgnore
  public String getKey() {
    return eq != null ? eq.getKey() : null;
  }

  @JsonIgnore
  public T getValue() {
    return eq != null ? eq.getValue() : null;
  }

  @Override
  public String getRepresentation() {
    return this.getKey() + " EQ " + this.getValue();
  }

  @Override
  public Boolean isValid() {
    return eq != null && eq.getKey() != null && !eq.getKey().isEmpty() && !eq.getKey().trim().isEmpty();
  }
}
