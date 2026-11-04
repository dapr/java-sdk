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

public class OrderStatus {

  public static final String WAITING_FOR_PAYMENT = "WAITING_FOR_PAYMENT";
  public static final String COMPLETED = "COMPLETED";
  public static final String CANCELLED = "CANCELLED";

  private String orderId;
  private String status;

  public OrderStatus() {
  }

  /**
   * Creates a new OrderStatus.
   *
   * @param orderId id of the order
   * @param status  current status of the order
   */
  public OrderStatus(String orderId, String status) {
    this.orderId = orderId;
    this.status = status;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return "OrderStatus{" + "orderId='" + orderId + '\'' + ", status='" + status + '\'' + '}';
  }
}
