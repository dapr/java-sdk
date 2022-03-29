/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.internal.opencensus;

/**
 * General internal utility methods.
 *
 * <p>Code originally from https://github.com/census-instrumentation/opencensus-java/blob/
 * 446e9bde9b1f6c0317e3f310644997e5d6d5eab2/api/src/main/java/io/opencensus/internal/Utils.java</p>
 */
final class Utils {

  private Utils() {
  }

  /**
   * Throws an {@link IllegalArgumentException} if the argument is false. This method is similar to
   * {@code Preconditions.checkArgument(boolean, Object)} from Guava.
   *
   * @param isValid      whether the argument check passed.
   * @param errorMessage the message to use for the exception. Will be converted to a string using
   *                     {@link String#valueOf(Object)}.
   */
  static void checkArgument(
      boolean isValid, @javax.annotation.Nullable Object errorMessage) {
    if (!isValid) {
      throw new IllegalArgumentException(String.valueOf(errorMessage));
    }
  }

  /**
   * Throws an {@link IllegalArgumentException} if the argument is false. This method is similar to
   * {@code Preconditions.checkArgument(boolean, Object)} from Guava.
   *
   * @param expression           a boolean expression
   * @param errorMessageTemplate a template for the exception message should the check fail. The
   *                             message is formed by replacing each {@code %s} placeholder in the template with an
   *                             argument. These are matched by position - the first {@code %s} gets {@code
   *                             errorMessageArgs[0]}, etc. Unmatched arguments will be appended to the formatted
   *                             message in
   *                             square braces. Unmatched placeholders will be left as-is.
   * @param errorMessageArgs     the arguments to be substituted into the message template. Arguments
   *                             are converted to strings using {@link String#valueOf(Object)}.
   * @throws IllegalArgumentException if {@code expression} is false
   * @throws NullPointerException     if the check fails and either {@code errorMessageTemplate} or
   *                                  {@code errorMessageArgs} is null (don't let this happen)
   */
  static void checkArgument(
      boolean expression,
      String errorMessageTemplate,
      @javax.annotation.Nullable Object... errorMessageArgs) {
    if (!expression) {
      throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageArgs));
    }
  }

  /**
   * Throws an {@link IllegalStateException} if the argument is false. This method is similar to
   * {@code Preconditions.checkState(boolean, Object)} from Guava.
   *
   * @param isValid      whether the state check passed.
   * @param errorMessage the message to use for the exception. Will be converted to a string using
   *                     {@link String#valueOf(Object)}.
   */
  static void checkState(boolean isValid, @javax.annotation.Nullable Object errorMessage) {
    if (!isValid) {
      throw new IllegalStateException(String.valueOf(errorMessage));
    }
  }

  /**
   * Validates an index in an array or other container. This method throws an {@link
   * IllegalArgumentException} if the size is negative and throws an {@link
   * IndexOutOfBoundsException} if the index is negative or greater than or equal to the size. This
   * method is similar to {@code Preconditions.checkElementIndex(int, int)} from Guava.
   *
   * @param index the index to validate.
   * @param size  the size of the array or container.
   */
  static void checkIndex(int index, int size) {
    if (size < 0) {
      throw new IllegalArgumentException("Negative size: " + size);
    }
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("Index out of bounds: size=" + size + ", index=" + index);
    }
  }

  /**
   * Throws a {@link NullPointerException} if the argument is null. This method is similar to {@code
   * Preconditions.checkNotNull(Object, Object)} from Guava.
   *
   * @param arg          the argument to check for null.
   * @param errorMessage the message to use for the exception. Will be converted to a string using
   *                     {@link String#valueOf(Object)}.
   * @param <T>          Object checked.
   * @return the argument, if it passes the null check.
   */
  public static <T> T checkNotNull(T arg, @javax.annotation.Nullable Object errorMessage) {
    if (arg == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return arg;
  }

  /**
   * Substitutes each {@code %s} in {@code template} with an argument. These are matched by
   * position: the first {@code %s} gets {@code args[0]}, etc. If there are more arguments than
   * placeholders, the unmatched arguments will be appended to the end of the formatted message in
   * square braces.
   *
   * <p>Copied from {@code Preconditions.format(String, Object...)} from Guava
   *
   * @param template a non-null string containing 0 or more {@code %s} placeholders.
   * @param args     the arguments to be substituted into the message template. Arguments are converted
   *                 to strings using {@link String#valueOf(Object)}. Arguments can be null.
   */
  // Note that this is somewhat-improperly used from Verify.java as well.
  private static String format(String template, @javax.annotation.Nullable Object... args) {
    // If no arguments return the template.
    if (args == null) {
      return template;
    }

    // start substituting the arguments into the '%s' placeholders
    StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
    int templateStart = 0;
    int i = 0;
    while (i < args.length) {
      int placeholderStart = template.indexOf("%s", templateStart);
      if (placeholderStart == -1) {
        break;
      }
      builder.append(template, templateStart, placeholderStart);
      builder.append(args[i++]);
      templateStart = placeholderStart + 2;
    }
    builder.append(template, templateStart, template.length());

    // if we run out of placeholders, append the extra args in square braces
    if (i < args.length) {
      builder.append(" [");
      builder.append(args[i++]);
      while (i < args.length) {
        builder.append(", ");
        builder.append(args[i++]);
      }
      builder.append(']');
    }

    return builder.toString();
  }
}
