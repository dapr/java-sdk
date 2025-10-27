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

/**
 * Options that can be used to control the behavior of orchestrator and activity task execution.
 */
public final class TaskOptions {
  private final RetryPolicy retryPolicy;
  private final RetryHandler retryHandler;
  private final String appID;

  private TaskOptions(RetryPolicy retryPolicy, RetryHandler retryHandler, String appID) {
    this.retryPolicy = retryPolicy;
    this.retryHandler = retryHandler;
    this.appID = appID;
  }

  /**
   * Creates a new builder for {@code TaskOptions}.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new {@code TaskOptions} object with default values.
   *
   * @return a new TaskOptions instance with no configuration
   */
  public static TaskOptions create() {
    return new Builder().build();
  }

  /**
   * Creates a new {@code TaskOptions} object from a {@link RetryPolicy}.
   *
   * @param retryPolicy the retry policy to use in the new {@code TaskOptions} object.
   * @return a new TaskOptions instance with the specified retry policy
   */
  public static TaskOptions withRetryPolicy(RetryPolicy retryPolicy) {
    return new Builder().retryPolicy(retryPolicy).build();
  }

  /**
   * Creates a new {@code TaskOptions} object from a {@link RetryHandler}.
   *
   * @param retryHandler the retry handler to use in the new {@code TaskOptions} object.
   * @return a new TaskOptions instance with the specified retry handler
   */
  public static TaskOptions withRetryHandler(RetryHandler retryHandler) {
    return new Builder().retryHandler(retryHandler).build();
  }

  /**
   * Creates a new {@code TaskOptions} object with the specified app ID.
   *
   * @param appID the app ID to use for cross-app workflow routing
   * @return a new TaskOptions instance with the specified app ID
   */
  public static TaskOptions withAppID(String appID) {
    return new Builder().appID(appID).build();
  }

  boolean hasRetryPolicy() {
    return this.retryPolicy != null;
  }

  /**
   * Gets the configured {@link RetryPolicy} value or {@code null} if none was configured.
   *
   * @return the configured retry policy
   */
  public RetryPolicy getRetryPolicy() {
    return this.retryPolicy;
  }

  boolean hasRetryHandler() {
    return this.retryHandler != null;
  }

  /**
   * Gets the configured {@link RetryHandler} value or {@code null} if none was configured.
   *
   * @return the configured retry handler.
   */
  public RetryHandler getRetryHandler() {
    return this.retryHandler;
  }

  /**
   * Gets the configured app ID value or {@code null} if none was configured.
   *
   * @return the configured app ID
   */
  public String getAppID() {
    return this.appID;
  }

  boolean hasAppID() {
    return this.appID != null && !this.appID.isEmpty();
  }

  /**
   * Builder for creating {@code TaskOptions} instances.
   */
  public static final class Builder {
    private RetryPolicy retryPolicy;
    private RetryHandler retryHandler;
    private String appID;

    private Builder() {
      // Private constructor -enforces using TaskOptions.builder()
    }

    /**
     * Sets the retry policy for the task options.
     *
     * @param retryPolicy the retry policy to use
     * @return this builder instance for method chaining
     */
    public Builder retryPolicy(RetryPolicy retryPolicy) {
      this.retryPolicy = retryPolicy;
      return this;
    }

    /**
     * Sets the retry handler for the task options.
     *
     * @param retryHandler the retry handler to use
     * @return this builder instance for method chaining
     */
    public Builder retryHandler(RetryHandler retryHandler) {
      this.retryHandler = retryHandler;
      return this;
    }

    /**
     * Sets the app ID for cross-app workflow routing.
     *
     * @param appID the app ID to use
     * @return this builder instance for method chaining
     */
    public Builder appID(String appID) {
      this.appID = appID;
      return this;
    }

    /**
     * Builds a new {@code TaskOptions} instance with the configured values.
     *
     * @return a new TaskOptions instance
     */
    public TaskOptions build() {
      return new TaskOptions(this.retryPolicy, this.retryHandler, this.appID);
    }
  }
}
