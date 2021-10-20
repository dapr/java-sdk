/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.utils;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationUtils {

    /**
     * Converts time from the String format used by Dapr into a Duration.
     *
     * @param valueString A String representing time in the Dapr runtime's format (e.g. 4h15m50s60ms).
     * @return A Duration
     */
    public static Duration convertDurationFromDaprFormat(String valueString) {
        // Convert the format returned by the Dapr runtime into Duration
        // An example of the format is: 4h15m50s60ms. It does not include days.
        int hourIndex = valueString.indexOf('h');
        int minuteIndex = valueString.indexOf('m');
        int secondIndex = valueString.indexOf('s');
        int milliIndex = valueString.indexOf("ms");

        String hoursSpan = valueString.substring(0, hourIndex);

        int hours = Integer.parseInt(hoursSpan);
        int days = hours / 24;
        hours = hours % 24;

        String minutesSpan = valueString.substring(hourIndex + 1, minuteIndex);
        int minutes = Integer.parseInt(minutesSpan);

        String secondsSpan = valueString.substring(minuteIndex + 1, secondIndex);
        int seconds = Integer.parseInt(secondsSpan);

        String millisecondsSpan = valueString.substring(secondIndex + 1, milliIndex);
        int milliseconds = Integer.parseInt(millisecondsSpan);

        return Duration.ZERO
                .plusDays(days)
                .plusHours(hours)
                .plusMinutes(minutes)
                .plusSeconds(seconds)
                .plusMillis(milliseconds);
    }

    /**
     * Converts a Duration to the format used by the Dapr runtime.
     *
     * @param value Duration
     * @return The Duration formatted as a String in the format the Dapr runtime uses (e.g. 4h15m50s60ms)
     */
    public static String convertDurationToDaprFormat(Duration value) {
        String stringValue = "";

        // return empty string for anything negative, it'll only happen for reminder "periods", not dueTimes.  A
        // negative "period" means fire once only.
        if (value == Duration.ZERO
                || (value.compareTo(Duration.ZERO) == 1)) {
            long hours = getDaysPart(value) * 24 + getHoursPart(value);

            StringBuilder sb = new StringBuilder();

            sb.append(hours);
            sb.append("h");

            sb.append(getMinutesPart((value)));
            sb.append("m");

            sb.append(getSecondsPart((value)));
            sb.append("s");

            sb.append(getMilliSecondsPart((value)));
            sb.append("ms");

            return sb.toString();
        }

        return stringValue;
    }

    /**
     * Converts a Duration and an amount of repetitions to the format used by the Dapr runtime.
     * The Dapr runtime supports the ISO 8601 interval specification format, which this method will return.
     * ex. R5/PT10S: Repeat 5 times, every 10 seconds.
     * We have chosen to leverage the Duration.toString() method which already returns a ISO 8601 compliant string, without the repetitions.
     *
     * @param repeatedDuration       Duration
     * @return The Duration and repetitions formatted as a String in the ISO 8601 format (e.g. R5/PT10S or PT10S)
     */
    public static String convertDurationWithRepetitionToISO8601Format(RepeatedDuration repeatedDuration) {
        StringBuilder builder = new StringBuilder();
        if (repeatedDuration.getRepetitions().isPresent()) {
            builder.append(String.format("R%s/", repeatedDuration.getRepetitions().get()));
        }

        builder.append(repeatedDuration.getDuration().toString());

        return builder.toString();
    }

    public static RepeatedDuration convertDurationWithRepetitionFromDapr(String input) {
        // We can read 2 formats, either ISO8601 or the classic one.
        // This method takes care of both;
        if (!(input.startsWith("R") || input.startsWith("P"))) {
            return new RepeatedDuration(DurationUtils.convertDurationFromDaprFormat(input));
        } else {
            return convertDurationWithRepetitionFromISO8601Format(input);
        }
    }

    public static RepeatedDuration convertDurationWithRepetitionFromISO8601Format(String input) {
        final String[] parts = input.split("/");

        Duration duration;
        Integer repetitions = null;
        if(parts.length == 1) {
            // Format only contains Duration  (e.g. PT10S)
            duration = Duration.parse(parts[0]);
        } else if(parts.length == 2) {
            // Format only contains both Repetition & Duration  (e.g. R5/PT10S)
            repetitions = Integer.parseInt(parts[0].replace("R",""));
            duration = Duration.parse(parts[1]);
        } else {
            throw new IllegalStateException("Input date '" + input + "' does not comply with ISO8601, so it could not be parsed.");
        }

        return new RepeatedDuration(duration, repetitions);
    }

    /**
     * Helper to get the "days" part of the Duration.  For example if the duration is 26 hours, this returns 1.
     *
     * @param d Duration
     * @return Number of days.
     */
    static long getDaysPart(Duration d) {
        long t = d.getSeconds() / 60 / 60 / 24;
        return t;
    }

    /**
     * Helper to get the "hours" part of the Duration.
     * For example if the duration is 26 hours, this is 1 day, 2 hours, so this returns 2.
     *
     * @param d The duration to parse
     * @return the hour part of the duration
     */
    static long getHoursPart(Duration d) {
        long u = (d.getSeconds() / 60 / 60) % 24;

        return u;
    }

    /**
     * Helper to get the "minutes" part of the Duration.
     *
     * @param d The duration to parse
     * @return the minutes part of the duration
     */
    static long getMinutesPart(Duration d) {
        long u = (d.getSeconds() / 60) % 60;

        return u;
    }

    /**
     * Helper to get the "seconds" part of the Duration.
     *
     * @param d The duration to parse
     * @return the seconds part of the duration
     */
    static long getSecondsPart(Duration d) {
        long u = d.getSeconds() % 60;

        return u;
    }

    /**
     * Helper to get the "millis" part of the Duration.
     *
     * @param d The duration to parse
     * @return the milliseconds part of the duration
     */
    static long getMilliSecondsPart(Duration d) {
        long u = d.toMillis() % 1000;

        return u;
    }

    public static class RepeatedDuration {
        private final Duration duration;
        private final Optional<Integer> repetitions;

        public RepeatedDuration(Duration duration, Integer repetitions) {
            this.duration = duration;
            this.repetitions = Optional.ofNullable(repetitions);
        }

        public RepeatedDuration(Duration duration) {
            this(duration, null);
        }

        public Duration getDuration() {
            return duration;
        }

        public Optional<Integer> getRepetitions() {
            return repetitions;
        }
    }


}
