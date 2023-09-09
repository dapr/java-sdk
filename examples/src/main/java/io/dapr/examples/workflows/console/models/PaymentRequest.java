package io.dapr.examples.workflows.console.models;

public class PaymentRequest {
  private String requestId;
  private String itemName;
  private int amount;
  private double currency;

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public double getCurrency() {
    return currency;
  }

  public void setCurrency(double currency) {
    this.currency = currency;
  }

  @Override
  public String toString() {
    return "PaymentRequest [requestId=" + requestId + ", itemName=" + itemName + ", amount=" + amount
        + ", currency=" + currency + "]";
  }

}
