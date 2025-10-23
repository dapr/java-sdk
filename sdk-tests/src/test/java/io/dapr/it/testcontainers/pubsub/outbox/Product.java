package io.dapr.it.testcontainers.pubsub.outbox;

import java.util.UUID;

public class Product {
  private String id;
  private String name;
  private double price;

  public Product() {
  }

  public Product(String name, double price) {
    this.id = UUID.randomUUID().toString();
    this.name = name;
    this.price = price;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  @Override
  public String toString() {
    return "Product{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        ", price=" + price +
        '}';
  }
}
