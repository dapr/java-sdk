/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.durabletask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;

final class Helpers {
  static final Duration maxDuration = Duration.ofSeconds(Long.MAX_VALUE, 999999999L);

  static @Nonnull <V> V throwIfArgumentNull(@Nullable V argValue, String argName) {
    if (argValue == null) {
      throw new IllegalArgumentException("The argument '" + argName + "' was null.");
    }

    return argValue;
  }

  static @Nonnull String throwIfArgumentNullOrWhiteSpace(String argValue, String argName) {
    throwIfArgumentNull(argValue, argName);
    if (argValue.trim().length() == 0) {
      throw new IllegalArgumentException("The argument '" + argName + "' was empty or contained only whitespace.");
    }

    return argValue;
  }

  static void throwIfOrchestratorComplete(boolean isComplete) {
    if (isComplete) {
      throw new IllegalStateException("The orchestrator has already completed");
    }
  }

  static boolean isInfiniteTimeout(Duration timeout) {
    return timeout == null || timeout.isNegative() || timeout.equals(maxDuration);
  }

  static double powExact(double base, double exponent) throws ArithmeticException {
    if (base == 0.0) {
      return 0.0;
    }

    double result = Math.pow(base, exponent);

    if (result == Double.POSITIVE_INFINITY) {
      throw new ArithmeticException("Double overflow resulting in POSITIVE_INFINITY");
    } else if (result == Double.NEGATIVE_INFINITY) {
      throw new ArithmeticException("Double overflow resulting in NEGATIVE_INFINITY");
    } else if (Double.compare(-0.0f, result) == 0) {
      throw new ArithmeticException("Double overflow resulting in negative zero");
    } else if (Double.compare(+0.0f, result) == 0) {
      throw new ArithmeticException("Double overflow resulting in positive zero");
    }

    return result;
  }

  static boolean isNullOrEmpty(String s) {
    return s == null || s.isEmpty();
  }

  // Cannot be instantiated
  private Helpers() {
  }
}
