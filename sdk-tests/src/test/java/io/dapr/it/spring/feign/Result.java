package io.dapr.it.spring.feign;

public class Result {
  private final String message;

  public Result(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
