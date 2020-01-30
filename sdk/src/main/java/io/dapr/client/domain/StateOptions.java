/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.dapr.utils.DurationUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StateOptions {
  private final Consistency consistency;
  private final Concurrency concurrency;
  private final RetryPolicy retryPolicy;

  /**
   * Represents options for a Dapr state API call.
   * @param consistency The consistency mode.
   * @param concurrency The concurrency mode.
   * @param retryPolicy The retry policy.
   */
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

  /**
   * Returns state options as a Map of option name to value.
   * @return A map of state options.
   */
  @JsonIgnore
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
          mapOptions.put("retryInterval", String.valueOf(this.getRetryPolicy().getInterval().toMillis()));
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

  public enum Consistency {
    EVENTUAL("eventual"),
    STRONG("strong");

    private final String value;

    Consistency(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return this.value;
    }

    @JsonCreator
    public static Consistency fromValue(String value) {
      return Consistency.valueOf(value);
    }
  }

  public enum Concurrency {
    FIRST_WRITE("first-write"),
    LAST_WRITE("last-write");

    private final String value;

    Concurrency(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return this.value;
    }

    @JsonCreator
    public static Concurrency fromValue(String value) {
      return Concurrency.valueOf(value);
    }
  }

  public static class RetryPolicy {
    public enum Pattern {
      LINEAR("linear"),
      EXPONENTIAL("exponential");

      private String value;

      Pattern(String value) {
        this.value = value;
      }

      @JsonValue
      public String getValue() {
        return this.value;
      }

      @JsonCreator
      public static Pattern fromValue(String value) {
        return Pattern.valueOf(value);
      }
    }

    @JsonSerialize(using = StateOptionDurationSerializer.class)
    @JsonDeserialize(using = StateOptionDurationDeserializer.class)
    private final Duration interval;
    private final Integer threshold;
    private final Pattern pattern;


    /**
     * Represents retry policies on a state operation.
     * @param interval The delay between retries.
     * @param threshold The total number of retries.
     * @param pattern The way to retry: linear or exponential.
     */
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

  public static class StateOptionDurationSerializer extends StdSerializer<Duration> {

    public StateOptionDurationSerializer() {

      super(Duration.class);
    }

    public StateOptionDurationSerializer(Class<Duration> t) {
      super(t);
    }

    @Override
    public void serialize(
        Duration duration,
        JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {
      jsonGenerator.writeNumber(duration.toMillis());
    }
  }

  public static class StateOptionDurationDeserializer extends StdDeserializer<Duration> {
    public StateOptionDurationDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public Duration deserialize(
        JsonParser jsonParser,
        DeserializationContext deserializationContext) throws IOException {
      String durationStr = jsonParser.readValueAs(String.class);
      Duration duration = null;
      if (durationStr != null && !durationStr.trim().isEmpty()) {
        try {
          duration = DurationUtils.convertDurationFromDaprFormat(durationStr);
        } catch (Exception ex) {
          throw InvalidFormatException.from(jsonParser, "Unable to parse duration.", ex);
        }
      }
      return duration;
    }
  }
}
