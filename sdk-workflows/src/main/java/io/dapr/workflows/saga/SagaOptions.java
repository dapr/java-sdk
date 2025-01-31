/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.workflows.saga;

/**
 * Saga option.
 */
public final class SagaOptions {
  private final boolean parallelCompensation;
  private final int maxParallelThread;
  private final boolean continueWithError;

  private SagaOptions(boolean parallelCompensation, int maxParallelThread, boolean continueWithError) {
    this.parallelCompensation = parallelCompensation;
    this.maxParallelThread = maxParallelThread;
    this.continueWithError = continueWithError;
  }

  public boolean isParallelCompensation() {
    return parallelCompensation;
  }

  public boolean isContinueWithError() {
    return continueWithError;
  }

  public int getMaxParallelThread() {
    return maxParallelThread;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {
    // by default compensation is sequential
    private boolean parallelCompensation = false;

    // by default max parallel thread is 16, it's enough for most cases
    private int maxParallelThread = 16;

    // by default set continueWithError to be true
    // So if a compensation fails, we should continue with the next compensations
    private boolean continueWithError = true;

    /**
     * Set parallel compensation.
     * @param parallelCompensation parallel compensation or not
     * @return this builder itself
     */
    public Builder setParallelCompensation(boolean parallelCompensation) {
      this.parallelCompensation = parallelCompensation;
      return this;
    }

    /**
     * set max parallel thread.
     * 
     * <p>Only valid when parallelCompensation is true.
     * @param maxParallelThread max parallel thread
     * @return this builder itself
     */
    public Builder setMaxParallelThread(int maxParallelThread) {
      if (maxParallelThread <= 2) {
        throw new IllegalArgumentException("maxParallelThread should be greater than 1.");
      }
      this.maxParallelThread = maxParallelThread;
      return this;
    }

    /**
     * Set continue with error.
     * 
     * <p>Only valid when parallelCompensation is false.
     * @param continueWithError continue with error or not
     * @return this builder itself
     */
    public Builder setContinueWithError(boolean continueWithError) {
      this.continueWithError = continueWithError;
      return this;
    }

    /**
     * Build Saga option.
     * @return Saga option
     */
    public SagaOptions build() {
      return new SagaOptions(this.parallelCompensation, this.maxParallelThread, this.continueWithError);
    }
  }
}
