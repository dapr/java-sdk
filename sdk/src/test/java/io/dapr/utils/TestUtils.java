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

package io.dapr.utils;

import io.dapr.exceptions.DaprErrorDetails;
import io.dapr.exceptions.DaprException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.net.ServerSocket;

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

  public static <T extends Throwable> void assertThrowsDaprException(
          Class<T> expectedType,
          String expectedErrorCode,
          String expectedErrorMessage,
          DaprErrorDetails expectedStatusDetails,
          Executable executable) {
    DaprException daprException = Assertions.assertThrows(DaprException.class, executable);
    Assertions.assertNotNull(daprException.getCause());
    Assertions.assertEquals(expectedType, daprException.getCause().getClass());
    Assertions.assertEquals(expectedErrorCode, daprException.getErrorCode());
    Assertions.assertEquals(expectedErrorMessage, daprException.getMessage());
    Assertions.assertEquals(expectedStatusDetails, daprException.getErrorDetails());
  }

  public static int findFreePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    }
  }

  public static String formatIpAddress(final String ipAddress) {
    String formattedIpAddress = ipAddress;
    if(NetworkUtils.isIPv6(ipAddress)) {
      formattedIpAddress = "[" + ipAddress + "]"; // per URL spec https://url.spec.whatwg.org/#host-writing
    }
    return formattedIpAddress;
  }
}