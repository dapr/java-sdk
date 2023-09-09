package io.dapr.examples.workflows.console.models;

public class InventoryResult {
  private boolean success;
  private InventoryItem orderPayload;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public InventoryItem getOrderPayload() {
    return orderPayload;
  }

  public void setOrderPayload(InventoryItem orderPayload) {
    this.orderPayload = orderPayload;
  }

  @Override
  public String toString() {
    return "InventoryResult [success=" + success + ", orderPayload=" + orderPayload + "]";
  }
}
