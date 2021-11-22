package io.dapr.actors;

import java.time.Duration;
import java.util.Optional;

public final class RepeatedDuration {

  /**
   * The minimum amount of repetitions TTL and Period can have.
   */
  private static final Integer MIN_AMOUNT_REPETITIONS = 0;

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
   * Instantiates a new instance for a repeated duration
   *
   * @param duration The interval until an action
   */
  public RepeatedDuration(Duration duration) {
    this(duration, null);
  }

  /**
   * Instantiates a new instance for a repeated duration
   *
   * @param duration    The interval until an action
   * @param repetitions The amount of times to invoke the action. May be <code>null</code>.
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

  public Duration getDuration() {
    return duration;
  }

  public Optional<Integer> getRepetitions() {
    return Optional.ofNullable(repetitions);
  }
}
