package io.dapr.examples.workflows.console.activities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dapr.examples.workflows.console.models.PaymentRequest;
import io.dapr.workflows.runtime.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowActivityContext;

public class ProcessPaymentActivity implements WorkflowActivity {
  private static Logger logger = LoggerFactory.getLogger(ProcessPaymentActivity.class);

  @Override
  public Object run(WorkflowActivityContext ctx) {
    PaymentRequest req = ctx.getInput(PaymentRequest.class);
    logger.info("Processing payment: {} for {} {} at ${}",
        req.getRequestId(), req.getAmount(), req.getItemName(), req.getCurrency());

    // Simulate slow processing
    try {
      Thread.sleep(7 * 1000);
    } catch (InterruptedException e) {
    }
    logger.info("Payment for request ID '{}' processed successfully", req.getRequestId());

    return true;
  }

}
