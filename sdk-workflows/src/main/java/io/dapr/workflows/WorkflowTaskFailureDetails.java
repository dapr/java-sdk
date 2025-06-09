/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.workflows;

public class WorkflowTaskFailureDetails {
  private final String errorType;
  private final String errorMessage;
  private final String stackTrace;
  private final boolean isNonRetriable;

  /**
   * Constructor for WorkflowTaskFailureDetails.
   *
   * @param errorType The type of error
   * @param errorMessage The error message
   * @param stackTrace The stacktrace of the error
   * @param isNonRetriable Whether the failure is retriable or not
   */
  public WorkflowTaskFailureDetails(
          String errorType,
          String errorMessage,
          String stackTrace,
          boolean isNonRetriable) {
    this.errorType = errorType;
    this.errorMessage = errorMessage;
    this.stackTrace = stackTrace;
    this.isNonRetriable = isNonRetriable;
  }

  /**
   * Gets the exception class name if the failure was caused by an unhandled exception. Otherwise, gets a symbolic
   * name that describes the general type of error that was encountered.
   *
   * @return the error type as a {@code String} value
   */
  public String getErrorType() {
    return this.errorType;
  }

  /**
   * Gets a summary description of the error that caused this failure. If the failure was caused by an exception, the
   * exception message is returned.
   *
   * @return a summary description of the error
   */
  public String getErrorMessage() {
    return this.errorMessage;
  }

  /**
   * Gets the stack trace of the exception that caused this failure, or {@code null} if the failure was caused by
   * a non-exception error.
   *
   * @return the stack trace of the failure exception or {@code null} if the failure was not caused by an exception
   */
  public String getStackTrace() {
    return this.stackTrace;
  }

  /**
   * Returns {@code true} if the failure doesn't permit retries, otherwise {@code false}.
   * @return {@code true} if the failure doesn't permit retries, otherwise {@code false}.
   */
  public boolean isNonRetriable() {
    return this.isNonRetriable;
  }

  /**
   * Returns {@code true} if the task failure was provided by the specified exception type, otherwise {@code false}.
   *
   * <p>This method allows checking if a task failed due to a specific exception type by attempting to load the class
   * specified in {@link #getErrorType()}. If the exception class cannot be loaded for any reason, this method will
   * return {@code false}. Base types are supported by this method, as shown in the following example:
   * <pre>{@code
   * boolean isRuntimeException = failureDetails.isCausedBy(RuntimeException.class);
   * }</pre>
   *
   * @param exceptionClass the class representing the exception type to test
   * @return {@code true} if the task failure was provided by the specified exception type, otherwise {@code false}
   */
  public boolean isCausedBy(Class<? extends Exception> exceptionClass) {
    String actualClassName = this.getErrorType();
    try {
      // Try using reflection to load the failure's class type and see if it's a subtype of the specified
      // exception. For example, this should always succeed if exceptionClass is System.Exception.
      Class<?> actualExceptionClass = Class.forName(actualClassName);
      return exceptionClass.isAssignableFrom(actualExceptionClass);
    } catch (ClassNotFoundException ex) {
      // Can't load the class and thus can't tell if it's related
      return false;
    }
  }

}
