/*
 * Copyright 2026 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.actors.runtime;

import java.time.Duration;

/**
 * Options to register an actor timer.
 *
 * <p>All fields are optional: without a due time the timer fires immediately, without a period it fires only
 * once, without repetitions it fires until unregistered (or until the TTL expires) and without a TTL it never
 * expires. A TTL requires a positive period.</p>
 */
public final class ActorTimerOptions {

  private final Duration dueTime;

  private final Duration period;

  private final Integer repetitions;

  private final Duration ttl;

  private ActorTimerOptions(Builder builder) {
    this.dueTime = builder.dueTime;
    this.period = builder.period;
    this.repetitions = builder.repetitions;
    this.ttl = builder.ttl;
  }

  /**
   * Creates a new builder.
   *
   * @return Builder for timer options.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the time the timer is due for the 1st time.
   *
   * @return Due time or null.
   */
  public Duration getDueTime() {
    return dueTime;
  }

  /**
   * Gets the interval between triggers.
   *
   * @return Period or null.
   */
  public Duration getPeriod() {
    return period;
  }

  /**
   * Gets the number of times the timer is triggered.
   *
   * @return Repetitions or null.
   */
  public Integer getRepetitions() {
    return repetitions;
  }

  /**
   * Gets the time after which the timer expires.
   *
   * @return TTL or null.
   */
  public Duration getTtl() {
    return ttl;
  }

  /**
   * Builder for {@link ActorTimerOptions}.
   */
  public static final class Builder {

    private Duration dueTime;

    private Duration period;

    private Integer repetitions;

    private Duration ttl;

    private Builder() {
    }

    /**
     * Sets the time the timer is due for the 1st time.
     *
     * @param dueTime Due time.
     * @return This builder.
     */
    public Builder setDueTime(Duration dueTime) {
      this.dueTime = dueTime;
      return this;
    }

    /**
     * Sets the interval between triggers.
     *
     * @param period Period.
     * @return This builder.
     */
    public Builder setPeriod(Duration period) {
      this.period = period;
      return this;
    }

    /**
     * Sets the number of times the timer is triggered, requires a period in whole seconds.
     *
     * @param repetitions Repetitions.
     * @return This builder.
     */
    public Builder setRepetitions(Integer repetitions) {
      this.repetitions = repetitions;
      return this;
    }

    /**
     * Sets the time after which the timer expires, requires a positive period.
     *
     * @param ttl TTL.
     * @return This builder.
     */
    public Builder setTtl(Duration ttl) {
      this.ttl = ttl;
      return this;
    }

    /**
     * Builds the timer options.
     *
     * @return Reminder options.
     */
    public ActorTimerOptions build() {
      return new ActorTimerOptions(this);
    }
  }
}
