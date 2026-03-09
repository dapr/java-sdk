package io.dapr.springboot4.examples.producer;

public class OrderDTO {

  private String id;
  private String item;
  private Integer amount;

  public OrderDTO() {
  }

  public OrderDTO(String id, String item, Integer amount) {
    this.id = id;
    this.item = item;
    this.amount = amount;
  }

  public String getId() {
    return id;
  }


  public String getItem() {
    return item;
  }

  public Integer getAmount() {
    return amount;
  }

}
