/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.config;

/**
 * Integer configuration property.
 */
public class IntegerProperty extends Property<Integer> {

  /**
   * {@inheritDoc}
   */
  IntegerProperty(String name, String envName, Integer defaultValue) {
    super(name, envName, defaultValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Integer parse(String value) {
    return Integer.valueOf(value);
  }

}
