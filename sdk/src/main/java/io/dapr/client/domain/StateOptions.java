/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client.domain;

import java.time.Duration;

public class StateOptions {
  private final Consistency consistency;
  private final Concurrency concurrency;
  private final RetryPolicy retryPolicy;

  public StateOptions(Consistency consistency, Concurrency concurrency, RetryPolicy retryPolicy) {
    this.consistency = consistency;
    this.concurrency = concurrency;
    this.retryPolicy = retryPolicy;
  }

  public Concurrency getConcurrency() {
    return concurrency;
  }

  public Consistency getConsistency() {
    return consistency;
  }

  public RetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  public static enum Consistency {
    EVENTUAL("eventual"),
    STRONG("strong");

    private final String value;

    private Consistency(String value) {
      this.value = value;
    }

    public String getValue() {
      return this.value;
    }
  }

  public static enum Concurrency {
    FIRST_WRITE("first-write"),
    LAST_WRITE ("last-write");

    private final String value;

    private Concurrency(String value) {
      this.value = value;
    }

    public String getValue() {
      return this.value;
    }
  }

  public static class RetryPolicy {
    public static enum Pattern {
      LINEAR("linear"),
      EXPONENTIAL("exponential");

      private String value;

      private Pattern(String value) {
        this.value = value;
      }

      public String getValue() {
        return this.value;
      }
    }

    private final Duration interval;
    private final String threshold;
    private final Pattern pattern;


    public RetryPolicy(Duration interval, String threshold, Pattern pattern) {
      this.interval = interval;
      this.threshold = threshold;
      this.pattern = pattern;
    }

    public Duration getInterval() {
      return interval;
    }

    public String getThreshold() {
      return threshold;
    }

    public Pattern getPattern() {
      return pattern;
    }
  }
}
