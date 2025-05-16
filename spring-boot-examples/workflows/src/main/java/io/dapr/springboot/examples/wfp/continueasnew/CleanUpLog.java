package io.dapr.springboot.examples.wfp.continueasnew;

public class CleanUpLog {
  private Integer cleanUpTimes = 0;

  public CleanUpLog() {
  }
  public void increment() {
    this.cleanUpTimes += 1;
  }

  public Integer getCleanUpTimes() {
    return cleanUpTimes;
  }
}
