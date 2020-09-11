/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.config;

import java.util.function.Function;

/**
 * Configuration property for any type.
 */
public class GenericProperty<T> extends Property<T> {

  private final Function<String, T> parser;

  /**
   * {@inheritDoc}
   */
  GenericProperty(String name, String envName, T defaultValue, Function<String, T> parser) {
    super(name, envName, defaultValue);
    this.parser = parser;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected T parse(String value) {
    return parser.apply(value);
  }

}
