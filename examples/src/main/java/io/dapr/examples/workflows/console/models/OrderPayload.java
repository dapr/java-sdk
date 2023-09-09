package io.dapr.examples.workflows.console.models;

public class OrderPayload {

  private String name;
  private double totalCost;
  private int quantity;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getTotalCost() {
    return totalCost;
  }

  public void setTotalCost(double totalCost) {
    this.totalCost = totalCost;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  @Override
  public String toString() {
    return "OrderPayload [name=" + name + ", totalCost=" + totalCost + ", quantity=" + quantity + "]";
  }

}
