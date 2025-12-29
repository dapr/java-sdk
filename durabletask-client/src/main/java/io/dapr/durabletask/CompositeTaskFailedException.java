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

package io.dapr.durabletask;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception that gets thrown when multiple {@link Task}s for an activity or sub-orchestration fails with an
 * unhandled exception.
 *
 * <p>Detailed information associated with each task failure can be retrieved using the {@link #getExceptions()}
 * method.</p>
 */
public class CompositeTaskFailedException extends RuntimeException {
  private final List<Exception> exceptions;

  CompositeTaskFailedException() {
    this.exceptions = new ArrayList<>();
  }

  CompositeTaskFailedException(List<Exception> exceptions) {
    this.exceptions = exceptions;
  }

  CompositeTaskFailedException(String message, List<Exception> exceptions) {
    super(message);
    this.exceptions = exceptions;
  }

  CompositeTaskFailedException(String message, Throwable cause, List<Exception> exceptions) {
    super(message, cause);
    this.exceptions = exceptions;
  }

  CompositeTaskFailedException(Throwable cause, List<Exception> exceptions) {
    super(cause);
    this.exceptions = exceptions;
  }

  CompositeTaskFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace,
                               List<Exception> exceptions) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.exceptions = exceptions;
  }

  /**
   * Gets a list of exceptions that occurred during execution of a group of {@link Task}.
   * These exceptions include details of the task failure and exception information
   *
   * @return a list of exceptions
   */
  public List<Exception> getExceptions() {
    return new ArrayList<>(this.exceptions);
  }

}
