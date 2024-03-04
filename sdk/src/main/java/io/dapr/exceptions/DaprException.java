/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.exceptions;

import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A Dapr's specific exception.
 */
public class DaprException extends RuntimeException {

  /**
   * Dapr's error code for this exception.
   */
  private final String errorCode;

  /**
   * The status details for the error.
   */
  private final DaprErrorDetails errorDetails;

  /**
   * Optional payload, if the exception came from a response body.
   */
  private final byte[] payload;

  /**
   * Optional HTTP status code, if error happened for an HTTP call (0 if not set).
   */
  private final int httpStatusCode;

  /**
   * New exception from a server-side generated error code and message.
   *
   * @param daprError Server-side error.
   * @param payload Optional payload containing the error.
   * @param httpStatusCode Optional http Status Code (0 if not set).
   */
  public DaprException(DaprError daprError, byte[] payload, int httpStatusCode) {
    this(daprError.getErrorCode(), daprError.getMessage(), daprError.getDetails(), payload, httpStatusCode);
  }

  /**
   * New exception from a server-side generated error code and message.
   * @param daprError Client-side error.
   * @param cause     the cause (which is saved for later retrieval by the
   *                  {@link #getCause()} method).  (A {@code null} value is
   *                  permitted, and indicates that the cause is nonexistent or
   *                  unknown.)
   */
  public DaprException(DaprError daprError, Throwable cause) {
    this(daprError.getErrorCode(), daprError.getMessage(), cause);
  }

  /**
   * Wraps an exception into a DaprException.
   * @param exception the exception to be wrapped.
   */
  public DaprException(Throwable exception) {
    this("UNKNOWN", exception.getMessage(), exception);
  }

  /**
   * New Exception from a client-side generated error code and message.
   *
   * @param errorCode Client-side error code.
   * @param message   Client-side error message.
   * @param payload Optional payload containing the error.
   * @param httpStatusCode Optional http Status Code (0 if not set).
   */
  public  DaprException(String errorCode, String message, byte[] payload, int httpStatusCode) {
    this(errorCode, message, DaprErrorDetails.EMPTY_INSTANCE, payload, httpStatusCode);
  }

  /**
   * New Exception from a client-side generated error code and message.
   *
   * @param errorCode Client-side error code.
   * @param message   Client-side error message.
   * @param errorDetails Details of the error from runtime.
   * @param payload Optional payload containing the error.
   * @param httpStatusCode Optional http Status Code (0 if not set).
   */
  public DaprException(
      String errorCode, String message, List<Map<String, Object>> errorDetails, byte[] payload, int httpStatusCode) {
    this(errorCode, message, new DaprErrorDetails(errorDetails), payload, httpStatusCode);
  }

  /**
   * New Exception from a client-side generated error code and message.
   *
   * @param errorCode Client-side error code.
   * @param message   Client-side error message.
   * @param errorDetails Details of the error from runtime.
   * @param payload Optional payload containing the error.
   */
  public DaprException(String errorCode, String message, DaprErrorDetails errorDetails, byte[] payload) {
    this(errorCode, message, errorDetails, payload, 0);
  }

  /**
   * New Exception from a client-side generated error code and message.
   *
   * @param errorCode Client-side error code.
   * @param message   Client-side error message.
   * @param errorDetails Details of the error from runtime.
   * @param payload Optional payload containing the error.
   * @param httpStatusCode Optional http Status Code (0 if not set).
   */
  public DaprException(
      String errorCode,
      String message,
      DaprErrorDetails errorDetails,
      byte[] payload,
      int httpStatusCode) {
    super(buildErrorMessage(errorCode, httpStatusCode, message));
    this.httpStatusCode = httpStatusCode;
    this.errorCode = errorCode;
    this.errorDetails = errorDetails;
    this.payload = payload;
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
  public DaprException(String errorCode, String message, Throwable cause) {
    super(buildErrorMessage(errorCode, 0, message), cause);
    this.httpStatusCode = 0;
    this.errorCode = errorCode;
    this.errorDetails = DaprErrorDetails.EMPTY_INSTANCE;
    this.payload = null;
  }

  /**
   * New exception from a server-side generated error code and message.
   * @param errorCode Client-side error code.
   * @param message   Client-side error message.
   * @param cause     the cause (which is saved for later retrieval by the
   *                  {@link #getCause()} method).  (A {@code null} value is
   *                  permitted, and indicates that the cause is nonexistent or
   *                  unknown.)
   * @param errorDetails the status details for the error.
   * @param payload Raw error payload.
   */
  public DaprException(
      String errorCode, String message, Throwable cause, DaprErrorDetails errorDetails, byte[] payload) {
    super(buildErrorMessage(errorCode, 0, message), cause);
    this.httpStatusCode = 0;
    this.errorCode = errorCode;
    this.errorDetails = errorDetails == null ? DaprErrorDetails.EMPTY_INSTANCE : errorDetails;
    this.payload = payload;
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
   * Returns the exception's error details.
   *
   * @return Error details.
   */
  public DaprErrorDetails getErrorDetails() {
    return this.errorDetails;
  }

  /**
   * Returns the exception's error payload (optional).
   *
   * @return Error's payload.
   */
  public byte[] getPayload() {
    return this.payload == null ? null : this.payload.clone();
  }

  /**
   * Returns the exception's http status code, 0 if not applicable.
   *
   * @return Http status code (0 if not applicable).
   */
  public int getHttpStatusCode() {
    return this.httpStatusCode;
  }

  /**
   * Wraps an exception into DaprException (if not already DaprException).
   *
   * @param exception Exception to be wrapped.
   */
  public static void wrap(Throwable exception) {
    if (exception == null) {
      return;
    }

    throw propagate(exception);
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
   * Wraps a runnable with a try-catch to throw DaprException.
   * @param runnable runnable to be invoked.
   * @return object of type T.
   */
  public static Runnable wrap(Runnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (Exception e) {
        wrap(e);
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

  /**
   * Wraps an exception into DaprException (if not already DaprException).
   *
   * @param exception Exception to be wrapped.
   * @param <T> Flux's response type.
   * @return Flux containing DaprException.
   */
  public static <T> Flux<T> wrapFlux(Exception exception) {
    try {
      wrap(exception);
    } catch (Exception e) {
      return Flux.error(e);
    }

    return Flux.empty();
  }

  /**
   * Wraps an exception into DaprException (if not already DaprException).
   *
   * @param exception Exception to be wrapped.
   * @return wrapped RuntimeException
   */
  public static RuntimeException propagate(Throwable exception) {
    Exceptions.throwIfFatal(exception);

    if (exception instanceof DaprException) {
      return (DaprException) exception;
    }

    Throwable e = exception;
    while (e != null) {
      if (e instanceof StatusRuntimeException) {
        StatusRuntimeException statusRuntimeException = (StatusRuntimeException) e;
        Status status = StatusProto.fromThrowable(statusRuntimeException);

        DaprErrorDetails errorDetails  = new DaprErrorDetails(status);

        return new DaprException(
                statusRuntimeException.getStatus().getCode().toString(),
                statusRuntimeException.getStatus().getDescription(),
                exception,
                errorDetails,
                status.toByteArray());

      }

      e = e.getCause();
    }

    if (exception instanceof IllegalArgumentException) {
      return (IllegalArgumentException) exception;
    }

    return new DaprException(exception);
  }

  private static String buildErrorMessage(String errorCode, int httpStatusCode, String message) {
    String result = ((errorCode == null) || errorCode.isEmpty()) ? "UNKNOWN: " : errorCode + ": ";
    if ((message == null) || message.isEmpty()) {
      if (httpStatusCode > 0) {
        return result + "HTTP status code: " + httpStatusCode;
      }
      return result;
    }

    if (httpStatusCode > 0) {
      return result + message + " (HTTP status code: " + httpStatusCode + ")";
    }
    return result + message;
  }
}
