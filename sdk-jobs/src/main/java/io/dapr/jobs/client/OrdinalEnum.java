package io.dapr.jobs.client;

/**
 * Represents an enumeration that has an associated ordinal rank.
 * This interface is intended to be implemented by enums that need to provide
 * a numerical representation for their values, such as days of the week or months of the year.
 */
public interface OrdinalEnum {

  /**
   * Returns the ordinal rank of the implementing enum.
   *
   * @return the rank as an integer.
   */
  int getRank();
}
