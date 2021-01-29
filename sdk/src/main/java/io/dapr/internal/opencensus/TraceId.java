/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.internal.opencensus;

import javax.annotation.concurrent.Immutable;

/**
 * A class that represents a trace identifier. A valid trace identifier is a 16-byte array with at
 * least one non-zero byte.
 *
 * <p>Code originally from https://github.com/census-instrumentation/opencensus-java/blob/
 *   446e9bde9b1f6c0317e3f310644997e5d6d5eab2/api/src/main/java/io/opencensus/trace/TraceId.java.</p>
 * @since 0.5
 */
@Immutable
final class TraceId implements Comparable<TraceId> {
  /**
   * The size in bytes of the {@code TraceId}.
   *
   * @since 0.5
   */
  static final int SIZE = 16;
  private static final int BASE16_SIZE = 2 * BigendianEncoding.LONG_BASE16;
  private static final long INVALID_ID = 0;

  // The internal representation of the TraceId.
  private final long idHi;
  private final long idLo;

  private TraceId(long idHi, long idLo) {
    this.idHi = idHi;
    this.idLo = idLo;
  }

  /**
   * Returns a {@code TraceId} built from a lowercase base16 representation.
   *
   * @param src       the lowercase base16 representation.
   * @param srcOffset the offset in the buffer where the representation of the {@code TraceId}
   *                  begins.
   * @return a {@code TraceId} built from a lowercase base16 representation.
   * @throws NullPointerException     if {@code src} is null.
   * @throws IllegalArgumentException if not enough characters in the {@code src} from the {@code
   *                                  srcOffset}.
   * @since 0.11
   */
  static TraceId fromLowerBase16(CharSequence src, int srcOffset) {
    Utils.checkNotNull(src, "src");
    return new TraceId(
        BigendianEncoding.longFromBase16String(src, srcOffset),
        BigendianEncoding.longFromBase16String(src, srcOffset + BigendianEncoding.LONG_BASE16));
  }

  /**
   * Copies the byte array representations of the {@code TraceId} into the {@code dest} beginning at
   * the {@code destOffset} offset.
   *
   * @param dest       the destination buffer.
   * @param destOffset the starting offset in the destination buffer.
   * @throws NullPointerException      if {@code dest} is null.
   * @throws IndexOutOfBoundsException if {@code destOffset+TraceId.SIZE} is greater than {@code
   *                                   dest.length}.
   * @since 0.5
   */
  void copyBytesTo(byte[] dest, int destOffset) {
    BigendianEncoding.longToByteArray(idHi, dest, destOffset);
    BigendianEncoding.longToByteArray(idLo, dest, destOffset + BigendianEncoding.LONG_BYTES);
  }

  /**
   * Copies the lowercase base16 representations of the {@code TraceId} into the {@code dest}
   * beginning at the {@code destOffset} offset.
   *
   * @param dest       the destination buffer.
   * @param destOffset the starting offset in the destination buffer.
   * @throws IndexOutOfBoundsException if {@code destOffset + 2 * TraceId.SIZE} is greater than
   *                                   {@code dest.length}.
   * @since 0.18
   */
  void copyLowerBase16To(char[] dest, int destOffset) {
    BigendianEncoding.longToBase16String(idHi, dest, destOffset);
    BigendianEncoding.longToBase16String(idLo, dest, destOffset + BASE16_SIZE / 2);
  }

  /**
   * Returns whether the {@code TraceId} is valid. A valid trace identifier is a 16-byte array with
   * at least one non-zero byte.
   *
   * @return {@code true} if the {@code TraceId} is valid.
   * @since 0.5
   */
  boolean isValid() {
    return idHi != INVALID_ID || idLo != INVALID_ID;
  }

  /**
   * Returns the lowercase base16 encoding of this {@code TraceId}.
   *
   * @return the lowercase base16 encoding of this {@code TraceId}.
   * @since 0.11
   */
  String toLowerBase16() {
    char[] chars = new char[BASE16_SIZE];
    copyLowerBase16To(chars, 0);
    return new String(chars);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (obj == this) {
      return true;
    }

    if (!(obj instanceof TraceId)) {
      return false;
    }

    TraceId that = (TraceId) obj;
    return idHi == that.idHi && idLo == that.idLo;
  }

  @Override
  public int hashCode() {
    // Copied from Arrays.hashCode(long[])
    int result = 1;
    result = 31 * result + ((int) (idHi ^ (idHi >>> 32)));
    result = 31 * result + ((int) (idLo ^ (idLo >>> 32)));
    return result;
  }

  @Override
  public String toString() {
    return "TraceId{traceId=" + toLowerBase16() + "}";
  }

  @Override
  public int compareTo(TraceId that) {
    if (idHi == that.idHi) {
      if (idLo == that.idLo) {
        return 0;
      }
      return idLo < that.idLo ? -1 : 1;
    }
    return idHi < that.idHi ? -1 : 1;
  }
}