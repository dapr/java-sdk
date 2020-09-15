/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.config;

/**
 * Boolean configuration property.
 */
public class BooleanProperty extends Property<Boolean> {

  /**
   * {@inheritDoc}
   */
  BooleanProperty(String name, String envName, Boolean defaultValue) {
    super(name, envName, defaultValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Boolean parse(String value) {
    return Boolean.valueOf(value);
  }

}
