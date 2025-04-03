package io.dapr.it.spring.feign;

public class Result {
  private String message;

  public Result() {}

  public Result(String message) {
    this.message = message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
