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

package io.quarkiverse.dapr.langchain4j.workflow;

import io.dapr.workflows.Workflow;
import io.quarkiverse.dapr.workflows.WorkflowMetadata;

/**
 * Resolves the registration name for a {@link Workflow} class.
 * If the class is annotated with {@link WorkflowMetadata} and provides a non-empty
 * {@code name}, that name is returned; otherwise, the fully-qualified class name is used.
 */
public final class WorkflowNameResolver {

  private WorkflowNameResolver() {
  }

  /**
   * Returns the Dapr registration name for the given workflow class.
   *
   * @param workflowClass the workflow class to resolve the name for
   * @return the Dapr registration name
   */
  public static String resolve(Class<? extends Workflow> workflowClass) {
    WorkflowMetadata meta = workflowClass.getAnnotation(WorkflowMetadata.class);
    if (meta != null && !meta.name().isEmpty()) {
      return meta.name();
    }
    return workflowClass.getCanonicalName();
  }
}
