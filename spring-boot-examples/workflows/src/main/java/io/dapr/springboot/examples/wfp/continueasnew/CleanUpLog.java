package io.dapr.springboot.examples.wfp.continueasnew;

import java.util.ArrayList;
import java.util.List;

public class CleanUpLog {
  private List<String> cleanUpTimes = new ArrayList<>();

  public CleanUpLog() {
  }

  public List<String> getCleanUpTimes() {
    return cleanUpTimes;
  }

  @Override
  public String toString() {
    return "CleanUpLog{" +
            "cleanUpTimes=" + cleanUpTimes +
            '}';
  }
}
