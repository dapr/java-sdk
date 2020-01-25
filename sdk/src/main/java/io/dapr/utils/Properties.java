/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.utils;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Global properties for Dapr's SDK, using Supplier so they are dynamically resolved.
 */
public class Properties {

  /**
   * Dapr's default HTTP port.
   */
  private static final Integer DEFAULT_HTTP_PORT = 3500;

  /**
   * Dapr's default GRPC port.
   */
  private static final Integer DEFAULT_GRPC_PORT = 50051;

  /**
   * Dapr's default GRPC port.
   */
  private static final Boolean DEFAULT_GRPC_ENABLED = false;

  /**
   * HTTP port for Dapr after checking system property and environment variable.
   */
  public static final Supplier<Integer> HTTP_PORT = () -> getIntOrDefault(
    "dapr.http.port", "DAPR_HTTP_PORT", DEFAULT_HTTP_PORT);

  /**
   * GRPC port for Dapr after checking system property and environment variable.
   */
  public static final Supplier<Integer> GRPC_PORT = () -> getIntOrDefault(
    "dapr.grpc.port", "DAPR_GRPC_PORT", DEFAULT_GRPC_PORT);

  /**
   * Determines if Dapr client will use GRPC to talk to Dapr's side car.
   */
  public static final Supplier<Boolean> USE_GRPC = () -> getBooleanOrDefault(
    "dapr.grpc.enabled", "DAPR_GRPC_ENABLED", DEFAULT_GRPC_ENABLED);

  /**
   * Finds an integer defined by system property first, then env variable or sticks to default.
   * @param propName     Name of the JVM's system property to override (1st).
   * @param envName      Name of env variable (2nd).
   * @param defaultValue Default value if cannot find a valid config (last).
   *
   * @return Integer from system property (1st) or env variable (2nd) or default (last).
   */
  public static Integer getIntOrDefault(String propName, String envName, Integer defaultValue) {
    return getValueOrDefault(propName, envName, defaultValue, s -> Integer.valueOf(s));
  }

  /**
   * Finds a boolean defined by system property first, then env variable or sticks to default.
   * @param propName     Name of the JVM's system property to override (1st).
   * @param envName      Name of env variable (2nd).
   * @param defaultValue Default value if cannot find a valid config (last).
   *
   * @return Boolean from system property (1st) or env variable (2nd) or default (last).
   */
  public static Boolean getBooleanOrDefault(String propName, String envName, Boolean defaultValue) {
    return getValueOrDefault(propName, envName, defaultValue, s -> Boolean.valueOf(s));
  }

  /**
   * Finds a value defined by system property first, then env variable or sticks to default.
   * @param propName     Name of the JVM's system property to override (1st).
   * @param envName      Name of env variable (2nd).
   * @param defaultValue Default value if cannot find a valid config (last).
   *
   * @return Value from system property (1st) or env variable (2nd) or default (last).
   */
  private static <T> T getValueOrDefault(String propName, String envName, T defaultValue, Function<String, T> parser) {
    String propValue = System.getProperty(propName);
    if (propValue != null && !propValue.trim().isEmpty()) {
      try {
        return parser.apply(propValue);
      } catch (Exception e) {
        e.printStackTrace();
        // OK, we tried. Falling back to system environment variable.
      }
    }

    String envValue = System.getenv(envName);
    if (envValue == null || envValue.trim().isEmpty()) {
      return defaultValue;
    }

    try {
      return parser.apply(propValue);
    } catch (Exception e) {
      e.printStackTrace();
      // OK, we tried. Falling back to default.
    }

    return defaultValue;
  }

}
