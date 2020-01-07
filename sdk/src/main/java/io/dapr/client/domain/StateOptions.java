package io.dapr.client.domain;

public class StateOptions {
  private String consistency;

  public String getConsistency() {
    return consistency;
  }

  public void setConsistency(String consistency) {
    this.consistency = consistency;
  }
}
