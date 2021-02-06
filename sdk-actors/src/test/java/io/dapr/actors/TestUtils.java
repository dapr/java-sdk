/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors;

import io.dapr.exceptions.DaprException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

public final class TestUtils {

  private TestUtils() {}

  public static <T extends Throwable> void assertThrowsDaprException(Class<T> expectedType, Executable executable) {
    Throwable cause = Assertions.assertThrows(DaprException.class, executable).getCause();
    Assertions.assertNotNull(cause);
    Assertions.assertEquals(expectedType, cause.getClass());
  }

  public static void assertThrowsDaprException(String expectedErrorCode, Executable executable) {
    DaprException daprException = Assertions.assertThrows(DaprException.class, executable);
    Assertions.assertNull(daprException.getCause());
    Assertions.assertEquals(expectedErrorCode, daprException.getErrorCode());
  }

  public static void assertThrowsDaprException(
      String expectedErrorCode,
      String expectedErrorMessage,
      Executable executable) {
    DaprException daprException = Assertions.assertThrows(DaprException.class, executable);
    Assertions.assertNull(daprException.getCause());
    Assertions.assertEquals(expectedErrorCode, daprException.getErrorCode());
    Assertions.assertEquals(expectedErrorMessage, daprException.getMessage());
  }

  public static <T extends Throwable> void assertThrowsDaprException(
      Class<T> expectedType,
      String expectedErrorCode,
      String expectedErrorMessage,
      Executable executable) {
    DaprException daprException = Assertions.assertThrows(DaprException.class, executable);
    Assertions.assertNotNull(daprException.getCause());
    Assertions.assertEquals(expectedType, daprException.getCause().getClass());
    Assertions.assertEquals(expectedErrorCode, daprException.getErrorCode());
    Assertions.assertEquals(expectedErrorMessage, daprException.getMessage());
  }
}
