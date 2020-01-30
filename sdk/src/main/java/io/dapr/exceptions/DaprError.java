/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.exceptions;

/**
 * Represents an error message from Dapr.
 */
public class DaprError {

  /**
   * Error code.
   */
  private String errorCode;

  /**
   * Error Message.
   */
  private String message;

  /**
   * Gets the error code.
   *
   * @return Error code.
   */
  public String getErrorCode() {
    return errorCode;
  }

  /**
   * Sets the error code.
   *
   * @param errorCode Error code.
   * @return This instance.
   */
  public DaprError setErrorCode(String errorCode) {
    this.errorCode = errorCode;
    return this;
  }

  /**
   * Gets the error message.
   *
   * @return Error message.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the error message.
   *
   * @param message Error message.
   * @return This instance.
   */
  public DaprError setMessage(String message) {
    this.message = message;
    return this;
  }

}
