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

import io.dapr.durabletask.TaskCanceledException;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Workflow started by an order event and continued by a payment event, both received through Pub/Sub.
 * Status changes are published back to Pub/Sub from an activity, as workflows must not perform I/O directly.
 */
@Component
public class OrderProcessingWorkflow implements Workflow {

  public static final String PAYMENT_RECEIVED_EVENT = "PaymentReceived";

  private static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(5);

  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());
      Order order = ctx.getInput(Order.class);

      ctx.callActivity(ReserveInventoryActivity.class.getName(), order).await();

      OrderStatus waiting = new OrderStatus(order.getId(), OrderStatus.WAITING_FOR_PAYMENT);
      ctx.callActivity(PublishOrderStatusActivity.class.getName(), waiting).await();

      OrderStatus result;
      try {
        Payment payment = ctx.waitForExternalEvent(PAYMENT_RECEIVED_EVENT, PAYMENT_TIMEOUT, Payment.class).await();
        ctx.getLogger().info("Payment received for order " + order.getId() + ": " + payment);
        result = new OrderStatus(order.getId(), OrderStatus.COMPLETED);
      } catch (TaskCanceledException e) {
        ctx.getLogger().info("Payment not received in time for order " + order.getId());
        result = new OrderStatus(order.getId(), OrderStatus.CANCELLED);
      }

      ctx.callActivity(PublishOrderStatusActivity.class.getName(), result).await();
      ctx.complete(result);
    };
  }
}
