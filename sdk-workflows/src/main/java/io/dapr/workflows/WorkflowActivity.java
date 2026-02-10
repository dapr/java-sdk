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

package io.dapr.workflows;

/**
 * Common interface for task activity implementations.
 *
 * <p>Activities are the basic unit of work in a durable task orchestration. Activities are the tasks that are
 * orchestrated in the business process. For example, you might create an orchestrator to process an order. The tasks
 * ay involve checking the inventory, charging the customer, and creating a shipment. Each task would be a separate
 * activity. These activities may be executed serially, in parallel, or some combination of both.
 *
 * <p>Unlike task orchestrators, activities aren't restricted in the type of work you can do in them. Activity functions
 * are frequently used to make network calls or run CPU intensive operations. An activity can also return data back to
 * the orchestrator function. The Durable Task runtime guarantees that each called activity function will be executed
 * <strong>at least once</strong> during an orchestration's execution.
 *
 * <p>Because activities only guarantee at least once execution, it's recommended that activity logic be implemented as
 * idempotent whenever possible.
 *
 * <p>Activities are scheduled by orchestrators using one of the {@link io.dapr.workflows.WorkflowContext#callActivity}
 * method overloads.
 */
public interface WorkflowActivity {
  /**
   * Executes the activity logic and returns a value which will be serialized and
   * returned to the calling orchestrator.
   *
   * @param ctx provides information about the current activity execution, like the activity's name and the input
   *            data provided to it by the orchestrator.
   * @return any serializable value to be returned to the calling orchestrator.
   */
  Object run(WorkflowActivityContext ctx);

  default String getName() {
    return null;
  }
}
