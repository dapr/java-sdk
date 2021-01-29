/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.internal.opencensus;

import javax.annotation.concurrent.Immutable;

/**
 * A class that represents a span identifier. A valid span identifier is an 8-byte array with at
 * least one non-zero byte.
 *
 * <p>Code originally from https://github.com/census-instrumentation/opencensus-java/blob/
 * 446e9bde9b1f6c0317e3f310644997e5d6d5eab2/api/src/main/java/io/opencensus/trace/TraceId.java</p>
 * @since 0.5
 */
@Immutable
final class SpanId implements Comparable<SpanId> {

  /**
   * The size in bytes of the {@code SpanId}.
   *
   * @since 0.5
   */
  public static final int SIZE = 8;

  private static final int BASE16_SIZE = 2 * SIZE;

  private static final long INVALID_ID = 0;

  // The internal representation of the SpanId.
  private final long id;

  private SpanId(long id) {
    this.id = id;
  }

  /**
   * Returns a {@code SpanId} built from a lowercase base16 representation.
   *
   * @param src       the lowercase base16 representation.
   * @param srcOffset the offset in the buffer where the representation of the {@code SpanId}
   *                  begins.
   * @return a {@code SpanId} built from a lowercase base16 representation.
   * @throws NullPointerException     if {@code src} is null.
   * @throws IllegalArgumentException if not enough characters in the {@code src} from the {@code
   *                                  srcOffset}.
   * @since 0.11
   */
  static SpanId fromLowerBase16(CharSequence src, int srcOffset) {
    Utils.checkNotNull(src, "src");
    return new SpanId(BigendianEncoding.longFromBase16String(src, srcOffset));
  }

  /**
   * Returns the byte representation of the {@code SpanId}.
   *
   * @return the byte representation of the {@code SpanId}.
   * @since 0.5
   */
  byte[] getBytes() {
    byte[] bytes = new byte[SIZE];
    BigendianEncoding.longToByteArray(id, bytes, 0);
    return bytes;
  }

  /**
   * Copies the byte array representations of the {@code SpanId} into the {@code dest} beginning at
   * the {@code destOffset} offset.
   *
   * @param dest       the destination buffer.
   * @param destOffset the starting offset in the destination buffer.
   * @throws NullPointerException      if {@code dest} is null.
   * @throws IndexOutOfBoundsException if {@code destOffset+SpanId.SIZE} is greater than {@code
   *                                   dest.length}.
   * @since 0.5
   */
  void copyBytesTo(byte[] dest, int destOffset) {
    BigendianEncoding.longToByteArray(id, dest, destOffset);
  }

  /**
   * Copies the lowercase base16 representations of the {@code SpanId} into the {@code dest}
   * beginning at the {@code destOffset} offset.
   *
   * @param dest       the destination buffer.
   * @param destOffset the starting offset in the destination buffer.
   * @throws IndexOutOfBoundsException if {@code destOffset + 2 * SpanId.SIZE} is greater than
   *                                   {@code dest.length}.
   * @since 0.18
   */
  void copyLowerBase16To(char[] dest, int destOffset) {
    BigendianEncoding.longToBase16String(id, dest, destOffset);
  }

  /**
   * Returns whether the span identifier is valid. A valid span identifier is an 8-byte array with
   * at least one non-zero byte.
   *
   * @return {@code true} if the span identifier is valid.
   * @since 0.5
   */
  boolean isValid() {
    return id != INVALID_ID;
  }

  /**
   * Returns the lowercase base16 encoding of this {@code SpanId}.
   *
   * @return the lowercase base16 encoding of this {@code SpanId}.
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

    if (!(obj instanceof SpanId)) {
      return false;
    }

    SpanId that = (SpanId) obj;
    return id == that.id;
  }

  @Override
  public int hashCode() {
    // Copied from Long.hashCode in java8.
    return (int) (id ^ (id >>> 32));
  }

  @Override
  public String toString() {
    return "SpanId{spanId=" + toLowerBase16() + "}";
  }

  @Override
  public int compareTo(SpanId that) {
    // Copied from Long.compare in java8.
    return (id < that.id) ? -1 : ((id == that.id) ? 0 : 1);
  }
}