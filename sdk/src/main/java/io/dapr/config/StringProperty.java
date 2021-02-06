/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.config;

/**
 * String configuration property.
 */
public class StringProperty extends Property<String> {

  /**
   * {@inheritDoc}
   */
  StringProperty(String name, String envName, String defaultValue) {
    super(name, envName, defaultValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String parse(String value) {
    return value;
  }

}
