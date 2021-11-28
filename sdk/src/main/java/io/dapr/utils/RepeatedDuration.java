/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.utils;

import java.time.Duration;
import java.util.Optional;

/**
 * Represents a duration with an optional amount of repetitions.
 */
public final class RepeatedDuration {

  /**
   * The minimum amount of repetitions.
   */
  private static final Integer MIN_AMOUNT_REPETITIONS = 1;

  /**
   * The duration.
   */
  private final Duration duration;

  /**
   * The amount of times the duration will be repeated.
   * This is an optional field.
   */
  private final Integer repetitions;

  /**
   * Instantiates a new instance of a repeated duration.
   *
   * @param duration The interval until an action.
   */
  public RepeatedDuration(Duration duration) {
    this(duration, null);
  }

  /**
   * Instantiates a new instance for a repeated duration.
   *
   * @param duration    The interval until an action.
   * @param repetitions The amount of times to invoke the action.
   */
  public RepeatedDuration(Duration duration, Integer repetitions) {
    validateDuration(duration);
    validateRepetitions(repetitions);
    this.duration = duration;
    this.repetitions = repetitions;
  }

  /**
   * Validates the duration.
   *
   * @param duration The duration to validate.
   */
  private static void validateDuration(Duration duration) {
    if (duration == null) {
      throw new IllegalArgumentException("Duration can not be null.");
    }
  }

  /**
   * Validates the repetitions of TTL and Period.
   *
   * @param repetitions The amount of repetitions checked.
   */
  private static void validateRepetitions(Integer repetitions) {
    if (repetitions != null && repetitions < MIN_AMOUNT_REPETITIONS) {
      String message = String.format(
          "argName: Amount of repetitions - specified value must be greater than %s",
          MIN_AMOUNT_REPETITIONS);
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Gets the {@link Duration}.
   *
   * @return The {@link Duration}.
   */
  public Duration getDuration() {
    return duration;
  }

  /**
   * Gets the amount of repetitions.
   *
   * @return The amount of repetitions as {@link Optional}.
   */
  public Optional<Integer> getRepetitions() {
    return Optional.ofNullable(repetitions);
  }
}
