/*
 * Copyright 2026 The Dapr Authors
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

import io.dapr.durabletask.implementation.protobuf.WorkflowScheduleOuterClass.ListWorkflowSchedulesResponse;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a page of workflow schedules returned by a list operation.
 */
public final class WorkflowSchedulePage {

  private final List<WorkflowSchedule> schedules;
  @Nullable
  private final String continuationToken;

  WorkflowSchedulePage(ListWorkflowSchedulesResponse response) {
    List<WorkflowSchedule> schedules = new ArrayList<>(response.getSchedulesCount());
    response.getSchedulesList().forEach(schedule -> schedules.add(new WorkflowSchedule(schedule)));
    this.schedules = Collections.unmodifiableList(schedules);
    this.continuationToken = response.hasContinuationToken() ? response.getContinuationToken() : null;
  }

  /**
   * Gets the workflow schedules in this page.
   *
   * @return an unmodifiable list of schedules
   */
  public List<WorkflowSchedule> getSchedules() {
    return this.schedules;
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
