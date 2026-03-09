/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.it.springboot4.testcontainers;

import io.dapr.exceptions.DaprErrorDetails;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.TypeRef;
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

  public static <T extends Throwable> void assertThrowsDaprException(
      String expectedErrorCode,
      String expectedErrorMessage,
      Executable executable) {
    DaprException daprException = Assertions.assertThrows(DaprException.class, executable);
    Assertions.assertEquals(expectedErrorCode, daprException.getErrorCode());
    Assertions.assertEquals(expectedErrorMessage, daprException.getMessage());
  }

  public static <T extends Throwable> void assertThrowsDaprExceptionWithReason(
      String expectedErrorCode,
      String expectedErrorMessage,
      String expectedReason,
      Executable executable) {
    DaprException daprException = Assertions.assertThrows(DaprException.class, executable);
    Assertions.assertEquals(expectedErrorCode, daprException.getErrorCode());
    Assertions.assertEquals(expectedErrorMessage, daprException.getMessage());
    Assertions.assertNotNull(daprException.getErrorDetails());
    Assertions.assertEquals(
        expectedReason,
        daprException.getErrorDetails().get(
            DaprErrorDetails.ErrorDetailType.ERROR_INFO,
            "reason",
            TypeRef.STRING
        ));
  }

  public static <T extends Throwable> void assertThrowsDaprExceptionSubstring(
      String expectedErrorCode,
      String expectedErrorMessageSubstring,
      Executable executable) {
    DaprException daprException = Assertions.assertThrows(DaprException.class, executable);
    Assertions.assertEquals(expectedErrorCode, daprException.getErrorCode());
    Assertions.assertTrue(daprException.getMessage().contains(expectedErrorMessageSubstring));
  }
}
