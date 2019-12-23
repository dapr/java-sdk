// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
// ------------------------------------------------------------

package io.dapr.actors.runtime;

import java.time.Duration;

final class ActorReminderInfo {

    private static final Duration MIN_TIME_PERIOD = Duration.ofMillis(-1);

    private final String data;

    private final Duration dueTime;

    private final Duration period;

    ActorReminderInfo(String data, Duration dueTime, Duration period) {
        ValidateDueTime("DueTime", dueTime);
        ValidatePeriod("Period", period);
        this.data = data;
        this.dueTime = dueTime;
        this.period = period;
    }

    Duration getDueTime() {
        return dueTime;
    }

    Duration getPeriod() {
        return period;
    }

    String getData() {
        return data;
    }

    private static void ValidateDueTime(String argName, Duration value) {
        if (value.compareTo(Duration.ZERO) < 0) {
            String message = String.format(
                    "argName: %s - Duration toMillis() - specified value must be greater than %s", argName, Duration.ZERO);
            throw new IllegalArgumentException(message);
        }
    }

    private static void ValidatePeriod(String argName, Duration value) throws IllegalArgumentException {
        if (value.compareTo(MIN_TIME_PERIOD) < 0) {
            String message = String.format(
                    "argName: %s - Duration toMillis() - specified value must be greater than %s", argName, Duration.ZERO);
            throw new IllegalArgumentException(message);
        }
    }
}
