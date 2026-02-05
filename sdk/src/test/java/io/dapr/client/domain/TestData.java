package io.dapr.client.domain;

public class TestData {
  private final String name;
  private final int age;

  public TestData(String name, int age) {
    this.name = name;
    this.age = age;
  }

  public int getAge() {
    return age;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "TestData{" +
        "name='" + name + "'" +
        ", age=" + age +
        "}";
  }
}
