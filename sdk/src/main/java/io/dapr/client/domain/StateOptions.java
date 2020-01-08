package io.dapr.client.domain;

public class StateOptions {
  private final String consistency;

  public StateOptions(String consistency) {
    this.consistency = consistency;
  }

  public String getConsistency() {
    return consistency;
  }

}
