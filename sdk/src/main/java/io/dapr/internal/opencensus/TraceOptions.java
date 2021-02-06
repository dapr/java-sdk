/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.internal.opencensus;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;

/**
 * A class that represents global trace options. These options are propagated to all child spans.
 * These determine features such as whether a {@code Span} should be traced. It is implemented as a bitmask.
 *
 * <p>Code originally from https://github.com/census-instrumentation/opencensus-java/blob/
 * 446e9bde9b1f6c0317e3f310644997e5d6d5eab2/api/src/main/java/io/opencensus/trace/TraceOptions.java</p>
 * @since 0.5
 */
@Immutable
final class TraceOptions {
  /**
   * The size in bytes of the {@code TraceOptions}.
   *
   * @since 0.5
   */
  static final int SIZE = 1;

  // The set of enabled features is determined by all the enabled bits.
  private final byte options;

  // Creates a new {@code TraceOptions} with the given options.
  private TraceOptions(byte options) {
    this.options = options;
  }

  /**
   * Returns a {@code TraceOption} built from a lowercase base16 representation.
   *
   * @param src       the lowercase base16 representation.
   * @param srcOffset the offset in the buffer where the representation of the {@code TraceOptions}
   *                  begins.
   * @return a {@code TraceOption} built from a lowercase base16 representation.
   * @throws NullPointerException     if {@code src} is null.
   * @throws IllegalArgumentException if {@code src.length} is not {@code 2 * TraceOption.SIZE} OR
   *                                  if the {@code str} has invalid characters.
   * @since 0.18
   */
  static TraceOptions fromLowerBase16(CharSequence src, int srcOffset) {
    return new TraceOptions(BigendianEncoding.byteFromBase16String(src, srcOffset));
  }

  /**
   * Copies the byte representations of the {@code TraceOptions} into the {@code dest} beginning at
   * the {@code destOffset} offset.
   *
   * <p>Equivalent with (but faster because it avoids any new allocations):
   *
   * <pre>{@code
   * System.arraycopy(getBytes(), 0, dest, destOffset, TraceOptions.SIZE);
   * }</pre>
   *
   * @param dest       the destination buffer.
   * @param destOffset the starting offset in the destination buffer.
   * @throws NullPointerException      if {@code dest} is null.
   * @throws IndexOutOfBoundsException if {@code destOffset+TraceOptions.SIZE} is greater than
   *                                   {@code dest.length}.
   * @since 0.5
   */
  void copyBytesTo(byte[] dest, int destOffset) {
    Utils.checkIndex(destOffset, dest.length);
    dest[destOffset] = options;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (obj == this) {
      return true;
    }

    if (!(obj instanceof TraceOptions)) {
      return false;
    }

    TraceOptions that = (TraceOptions) obj;
    return options == that.options;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new byte[]{options});
  }

}
