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

package io.dapr.workflows.client;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a page of workflow instance IDs returned by a list operation.
 */
public final class WorkflowInstancePage {

  private final List<String> instanceIds;
  @Nullable
  private final String continuationToken;

  /**
   * Constructs a page of workflow instance IDs.
   *
   * @param instanceIds       the workflow instance IDs in this page; must not be null
   * @param continuationToken the token used to retrieve the next page, or null if there are no more pages
   */
  public WorkflowInstancePage(List<String> instanceIds, @Nullable String continuationToken) {
    this.instanceIds = Collections.unmodifiableList(new ArrayList<>(instanceIds));
    this.continuationToken = continuationToken;
  }

  /**
   * Gets the workflow instance IDs in this page.
   *
   * @return an unmodifiable list of instance IDs
   */
  public List<String> getInstanceIds() {
    return this.instanceIds;
  }

  /**
   * Gets the continuation token for the next page.
   *
   * @return the continuation token, or null if there are no more pages
   */
  @Nullable
  public String getContinuationToken() {
    return this.continuationToken;
  }
}
