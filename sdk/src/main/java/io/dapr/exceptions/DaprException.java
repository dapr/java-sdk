/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.exceptions;

import io.grpc.StatusRuntimeException;
import reactor.core.publisher.Mono;

import java.util.concurrent.Callable;

/**
 * A Dapr's specific exception.
 */
public class DaprException extends RuntimeException {

  /**
   * Dapr's error code for this exception.
   */
  private String errorCode;

  /**
   * New exception from a server-side generated error code and message.
   *
   * @param daprError Server-side error.
   */
  public DaprException(DaprError daprError) {
    this(daprError.getErrorCode(), daprError.getMessage());
  }

  /**
   * New exception from a server-side generated error code and message.
   * @param daprError Client-side error.
   * @param cause     the cause (which is saved for later retrieval by the
   *                  {@link #getCause()} method).  (A {@code null} value is
   *                  permitted, and indicates that the cause is nonexistent or
   *                  unknown.)
   */
  public DaprException(DaprError daprError, Exception cause) {
    this(daprError.getErrorCode(), daprError.getMessage(), cause);
  }

  /**
   * Wraps an exception into a DaprException.
   * @param exception the exception to be wrapped.
   */
  public DaprException(Exception exception) {
    this("UNKNOWN", exception.getMessage(), exception);
  }

  /**
   * New Exception from a client-side generated error code and message.
   *
   * @param errorCode Client-side error code.
   * @param message   Client-side error message.
   */
  public DaprException(String errorCode, String message) {
    super(String.format("%s: %s", errorCode, message));
    this.errorCode = errorCode;
  }

  /**
   * New exception from a server-side generated error code and message.
   * @param errorCode Client-side error code.
   * @param message   Client-side error message.
   * @param cause     the cause (which is saved for later retrieval by the
   *                  {@link #getCause()} method).  (A {@code null} value is
   *                  permitted, and indicates that the cause is nonexistent or
   *                  unknown.)
   */
  public DaprException(String errorCode, String message, Exception cause) {
    super(String.format("%s: %s", errorCode, emptyIfNull(message)), cause);
    this.errorCode = errorCode;
  }

  /**
   * Returns the exception's error code.
   *
   * @return Error code.
   */
  public String getErrorCode() {
    return this.errorCode;
  }

  /**
   * Wraps an exception into DaprException (if not already DaprException).
   *
   * @param exception Exception to be wrapped.
   */
  public static void wrap(Exception exception) {
    if (exception == null) {
      return;
    }

    if (exception instanceof DaprException) {
      throw (DaprException) exception;
    }

    Throwable e = exception;
    while (e != null) {
      if (e instanceof StatusRuntimeException) {
        StatusRuntimeException statusRuntimeException = (StatusRuntimeException) e;
        throw new DaprException(
            statusRuntimeException.getStatus().getCode().toString(),
            statusRuntimeException.getStatus().getDescription(),
            exception);
      }

      e = e.getCause();
    }

    if (exception instanceof IllegalArgumentException) {
      throw (IllegalArgumentException) exception;
    }

    throw new DaprException(exception);
  }

  /**
   * Wraps a callable with a try-catch to throw DaprException.
   * @param callable callable to be invoked.
   * @param <T> type to be returned
   * @return object of type T.
   */
  public static <T> Callable<T> wrap(Callable<T> callable) {
    return () -> {
      try {
        return callable.call();
      } catch (Exception e) {
        wrap(e);
        return null;
      }
    };
  }

  /**
   * Wraps an exception into DaprException (if not already DaprException).
   *
   * @param exception Exception to be wrapped.
   * @param <T> Mono's response type.
   * @return Mono containing DaprException.
   */
  public static <T> Mono<T> wrapMono(Exception exception) {
    try {
      wrap(exception);
    } catch (Exception e) {
      return Mono.error(e);
    }

    return Mono.empty();
  }

  private static String emptyIfNull(String str) {
    if (str == null) {
      return "";
    }

    return str;
  }
}
