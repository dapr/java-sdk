/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import io.dapr.utils.DurationUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

  public Map<String, String> getStateOptionsAsMap() {
    Map<String, String> mapOptions = null;
    if (this != null) {
      mapOptions = new HashMap<>();
      if (this.getConsistency() != null) {
        mapOptions.put("consistency", this.getConsistency().getValue());
      }
      if (this.getConcurrency() != null) {
        mapOptions.put("concurrency", this.getConcurrency().getValue());
      }
      if (this.getRetryPolicy() != null) {
        if (this.getRetryPolicy().getInterval() != null) {
          mapOptions.put("retryInterval", DurationUtils.ConvertDurationToDaprFormat(this.getRetryPolicy().getInterval()));
        }
        if (this.getRetryPolicy().getThreshold() != null) {
          mapOptions.put("retryThreshold", this.getRetryPolicy().getThreshold().toString());
        }
        if (this.getRetryPolicy().getPattern() != null) {
          mapOptions.put("retryPattern", this.getRetryPolicy().getPattern().getValue());
        }
      }
    }
    return Collections.unmodifiableMap(Optional.ofNullable(mapOptions).orElse(Collections.EMPTY_MAP));
  }

  public static enum Consistency {
    EVENTUAL("eventual"),
    STRONG("strong");

    private final String value;

    private Consistency(String value) {
      this.value = value;
    }

    @JsonValue
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

    @JsonValue
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
    private final Integer threshold;
    private final Pattern pattern;


    public RetryPolicy(Duration interval, Integer threshold, Pattern pattern) {
      this.interval = interval;
      this.threshold = threshold;
      this.pattern = pattern;
    }

    public Duration getInterval() {
      return interval;
    }

    public Integer getThreshold() {
      return threshold;
    }

    public Pattern getPattern() {
      return pattern;
    }

  }
}
