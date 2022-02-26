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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AndFilter.class, name = "AND"),
    @JsonSubTypes.Type(value = InFilter.class, name = "IN"),
    @JsonSubTypes.Type(value = OrFilter.class, name = "OR"),
    @JsonSubTypes.Type(value = EqFilter.class, name = "EQ")
})
public abstract class Filter<T> {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @JsonIgnore
  private String name;

  Filter() {
    // For JSON Serialization
  }

  public Filter(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @JsonIgnore
  public abstract String getRepresentation();

  @JsonIgnore
  public abstract Boolean isValid();
}
