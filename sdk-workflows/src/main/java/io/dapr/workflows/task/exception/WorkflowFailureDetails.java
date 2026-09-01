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

package io.dapr.workflows.task.exception;

import com.google.protobuf.StringValue;
import io.dapr.durabletask.implementation.protobuf.Orchestration.TaskFailureDetails;
import io.dapr.workflows.task.history.PropagatedHistoryException;
import io.dapr.workflows.task.interruption.ContinueAsNewInterruption;
import io.dapr.workflows.task.interruption.OrchestratorBlockedException;
import io.dapr.workflows.task.serialization.DataConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that represents the details of a task failure.
 *
 * <p>In most cases, failures are caused by unhandled exceptions in activity or orchestrator code, in which case
 * instances of this class will expose the details of the exception. However, it's also possible that other types
 * of errors could result in task failures, in which case there may not be any exception-specific information.</p>
 */
public final class WorkflowFailureDetails {

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowFailureDetails.class);

  /**
   * Error types written into workflow history by SDK versions that shipped these exceptions under
   * {@code io.dapr.durabletask}, mapped to where they live now.
   *
   * <p>Includes the nested {@code DataConverter$DataConverterException}; enumerating the legacy
   * types by source file name misses it, because a nested class has no file of its own.
   *
   * <p>The error type is the exception's fully qualified name and it is persisted, so a workflow
   * started before the durable task client was folded into this module carries the old names in its
   * history. Without this mapping {@link #isCausedBy(Class)} would silently answer {@code false} for
   * those instances after an upgrade, sending compensation logic down the wrong branch.
   */
  private static final Map<String, String> LEGACY_ERROR_TYPES = legacyErrorTypes();

  private static Map<String, String> legacyErrorTypes() {
    Map<String, String> legacy = new HashMap<>();
    legacy.put("io.dapr.durabletask.TaskFailedException", TaskFailedException.class.getName());
    legacy.put("io.dapr.durabletask.TaskCanceledException", TaskCanceledException.class.getName());
    legacy.put("io.dapr.durabletask.CompositeTaskFailedException",
        CompositeTaskFailedException.class.getName());
    legacy.put("io.dapr.durabletask.NonDeterministicOrchestratorException",
        NonDeterministicOrchestratorException.class.getName());
    legacy.put("io.dapr.durabletask.PropagatedHistoryException",
        PropagatedHistoryException.class.getName());
    legacy.put("io.dapr.durabletask.orchestration.exception.VersionNotRegisteredException",
        VersionNotRegisteredException.class.getName());
    legacy.put("io.dapr.durabletask.interruption.OrchestratorBlockedException",
        OrchestratorBlockedException.class.getName());
    legacy.put("io.dapr.durabletask.interruption.ContinueAsNewInterruption",
        ContinueAsNewInterruption.class.getName());
    // Nested inside the DataConverter interface, so its binary name uses '$'. Easy to miss when
    // enumerating exception types by file name - it has no file of its own.
    legacy.put("io.dapr.durabletask.DataConverter$DataConverterException",
        DataConverter.DataConverterException.class.getName());
    return Collections.unmodifiableMap(legacy);
  }

  private final String errorType;
  private final String errorMessage;
  private final String stackTrace;
  private final boolean isNonRetriable;

  /**
   * Creates failure details from their individual parts.
   *
   * <p>Public so the workflow executor in a sibling package can reach it; not intended for
   * application code.
   *
   * @param errorType the namespace-qualified exception type name.
   * @param errorMessage the error message, if any.
   * @param errorDetails the stack trace, if any.
   * @param isNonRetriable whether the failure must not be retried.
   */
  public WorkflowFailureDetails(
      String errorType,
      @Nullable String errorMessage,
      @Nullable String errorDetails,
      boolean isNonRetriable) {
    this.errorType = errorType;
    this.stackTrace = errorDetails;

    // Error message can be null for things like NullPointerException but the gRPC contract doesn't allow null
    this.errorMessage = errorMessage != null ? errorMessage : "";
    this.isNonRetriable = isNonRetriable;
  }

  public WorkflowFailureDetails(Exception exception) {
    this(exception.getClass().getName(), exception.getMessage(), getFullStackTrace(exception), false);
  }

  /**
   * Creates failure details from their protobuf form.
   *
   * <p>Public so the workflow executor in a sibling package can reach it; not intended for
   * application code.
   *
   * @param proto the protobuf failure details.
   */
  public WorkflowFailureDetails(TaskFailureDetails proto) {
    this(proto.getErrorType(),
        proto.getErrorMessage(),
        proto.getStackTrace().getValue(),
        proto.getIsNonRetriable());
  }

  /**
   * Gets the exception class name if the failure was caused by an unhandled exception. Otherwise, gets a symbolic
   * name that describes the general type of error that was encountered.
   *
   * @return the error type as a {@code String} value
   */
  @Nonnull
  public String getErrorType() {
    return this.errorType;
  }

  /**
   * Gets a summary description of the error that caused this failure. If the failure was caused by an exception, the
   * exception message is returned.
   *
   * @return a summary description of the error
   */
  @Nonnull
  public String getErrorMessage() {
    return this.errorMessage;
  }

  /**
   * Gets the stack trace of the exception that caused this failure, or {@code null} if the failure was caused by
   * a non-exception error.
   *
   * @return the stack trace of the failure exception or {@code null} if the failure was not caused by an exception
   */
  @Nullable
  public String getStackTrace() {
    return this.stackTrace;
  }

  /**
   * Returns {@code true} if the failure doesn't permit retries, otherwise {@code false}.
   *
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
   * return {@code false}. Base types are supported by this method, as shown in the following example:</p>
   * <pre>{@code
   * boolean isRuntimeException = failureDetails.isCausedBy(RuntimeException.class);
   * }</pre>
   *
   * @param exceptionClass the class representing the exception type to test
   * @return {@code true} if the task failure was provided by the specified exception type, otherwise {@code false}
   */
  public boolean isCausedBy(Class<? extends Exception> exceptionClass) {
    String actualClassName = this.getErrorType();
    String resolvedClassName = LEGACY_ERROR_TYPES.getOrDefault(actualClassName, actualClassName);

    try {
      // Try using reflection to load the failure's class type and see if it's a subtype of the specified
      // exception. For example, this should always succeed if exceptionClass is System.Exception.
      Class<?> actualExceptionClass = Class.forName(resolvedClassName);
      return exceptionClass.isAssignableFrom(actualExceptionClass);
    } catch (ClassNotFoundException ex) {
      // Can't load the class and thus can't tell if it's related. Say so rather than failing silently:
      // an unloadable error type is usually history written by a different application or SDK version,
      // and a quiet false here is indistinguishable from a genuine "not caused by".
      LOGGER.warn("Cannot determine whether failure type '{}' is a {}: the class is not on the "
          + "classpath, so isCausedBy is answering false.", actualClassName, exceptionClass.getName());
      return false;
    }
  }

  /**
   * Gets the full stack trace of the specified exception.
   *
   * @param e the exception
   * @return the full stack trace of the exception
   */
  public static String getFullStackTrace(Throwable e) {
    StackTraceElement[] elements = e.getStackTrace();

    // Plan for 256 characters per stack frame (which is likely on the high-end)
    StringBuilder sb = new StringBuilder(elements.length * 256);
    for (StackTraceElement element : elements) {
      sb.append("\tat ").append(element.toString()).append(System.lineSeparator());
    }
    return sb.toString();
  }

  /**
   * Converts these failure details to their protobuf form.
   *
   * <p>Public so the workflow executor in a sibling package can reach it; not intended for
   * application code.
   *
   * @return the protobuf failure details.
   */
  public TaskFailureDetails toProto() {
    return TaskFailureDetails.newBuilder()
        .setErrorType(this.getErrorType())
        .setErrorMessage(this.getErrorMessage())
        .setStackTrace(StringValue.of(this.getStackTrace() != null ? this.getStackTrace() : ""))
        .build();
  }
}
