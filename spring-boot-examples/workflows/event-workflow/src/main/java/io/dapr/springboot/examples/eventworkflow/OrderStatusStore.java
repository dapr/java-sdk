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

package io.dapr.springboot.examples.eventworkflow;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the last status received from the order-status topic for each order.
 */
@Component
public class OrderStatusStore {

  private final Map<String, OrderStatus> statuses = new ConcurrentHashMap<>();

  public void put(OrderStatus orderStatus) {
    statuses.put(orderStatus.getOrderId(), orderStatus);
  }

  public OrderStatus get(String orderId) {
    return statuses.get(orderId);
  }

  public void clear() {
    statuses.clear();
  }
}
