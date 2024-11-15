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

package io.dapr.internal.exceptions;

import io.grpc.Metadata;

import java.util.concurrent.ExecutionException;

/**
 * Internal exception for propagating HTTP status code.
 */
public class DaprHttpException extends ExecutionException {

  /**
   * This is the metadata from HTTP binding that we want to automatically parse and make it as part of the exception.
   */
  private static final Metadata.Key<String> GRPC_METADATA_KEY_HTTP_STATUS_CODE =
      Metadata.Key.of("metadata.statuscode", Metadata.ASCII_STRING_MARSHALLER);

  private final int statusCode;

  private final byte[] payload;

  /**
   * Instantiates a new DaprHttpException, without http body.
   * @param statusCode HTTP status code.
   * @param cause Exception thrown.
   */
  private DaprHttpException(int statusCode, Throwable cause) {
    super(cause);
    this.statusCode = statusCode;
    this.payload = null;
  }

  /**
   * Instantiates a new DaprHttpException with a given HTTP payload.
   * @param statusCode HTTP status code.
   * @param payload HTTP payload.
   */
  public DaprHttpException(int statusCode, byte[] payload) {
    super();
    this.statusCode = statusCode;
    this.payload = payload;
  }

  /**
   * Creates an ExecutionException (can also be HttpException, if applicable).
   * @param grpcMetadata Optional gRPC metadata.
   * @param cause Exception triggered during execution.
   * @return ExecutionException
   */
  public static ExecutionException fromGrpcExecutionException(Metadata grpcMetadata, Throwable cause) {
    int httpStatusCode = parseHttpStatusCode(grpcMetadata);
    if (!isValidHttpStatusCode(httpStatusCode)) {
      return new ExecutionException(cause);
    }

    return new DaprHttpException(httpStatusCode, cause);
  }

  public static boolean isValidHttpStatusCode(int statusCode) {
    return statusCode >= 100 && statusCode <= 599; // Status codes range from 100 to 599
  }

  public static boolean isSuccessfulHttpStatusCode(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  private static int parseHttpStatusCode(Metadata grpcMetadata) {
    if (grpcMetadata == null) {
      return 0;
    }

    return parseHttpStatusCode(grpcMetadata.get(GRPC_METADATA_KEY_HTTP_STATUS_CODE));
  }

  /**
   * Parses a given string value into an HTTP status code, 0 if invalid.
   * @param value String value to be parsed.
   * @return HTTP status code, 0 if not valid.
   */
  public static int parseHttpStatusCode(String value) {
    if ((value == null) || value.isEmpty()) {
      return 0;
    }

    try {
      int httpStatusCode = Integer.parseInt(value);
      if (!isValidHttpStatusCode(httpStatusCode)) {
        return 0;
      }

      return httpStatusCode;
    } catch (NumberFormatException nfe) {
      return 0;
    }
  }

  /**
   * Returns the HTTP Status code for the exception.
   * @return HTTP Status code for the exception, 0 if not applicable.
   */
  public int getStatusCode() {
    return this.statusCode;
  }

  /**
   * Returns the HTTP payload for the exception.
   * @return HTTP payload, null if not present.
   */
  public byte[] getPayload() {
    return this.payload;
  }

}
