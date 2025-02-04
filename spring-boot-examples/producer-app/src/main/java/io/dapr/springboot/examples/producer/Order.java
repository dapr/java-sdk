package io.dapr.springboot.examples.producer;

import org.springframework.data.annotation.Id;

public class Order {

  @Id
  private String id;
  private String item;
  private Integer amount;

  public Order() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getItem() {
    return item;
  }

  public void setItem(String item) {
    this.item = item;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }
}
