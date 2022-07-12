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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Sorting {
  private String key;
  private Order order;

  Sorting() {
    // For JSON
  }

  public Sorting(String key, Order order) {
    this.key = key;
    this.order = order;
  }

  public String getKey() {
    return key;
  }

  public Order getOrder() {
    return order;
  }

  public enum Order {
    ASC("ASC"),
    DESC("DESC");

    private String name;

    Order(String name) {
      this.name = name;
    }

    @JsonValue
    public String getValue() {
      return this.name;
    }

    @JsonCreator
    public static Order fromValue(String value) {
      return Order.valueOf(value);
    }
  }
}
