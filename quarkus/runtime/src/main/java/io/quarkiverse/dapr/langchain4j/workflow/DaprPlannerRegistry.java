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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry mapping planner IDs to {@link DaprWorkflowPlanner} instances.
 * Allows Dapr WorkflowActivities (which are instantiated by the Dapr SDK) to
 * look up the in-process planner.
 */
public class DaprPlannerRegistry {

  private static final ConcurrentHashMap<String, DaprWorkflowPlanner> registry = new ConcurrentHashMap<>();

  /**
   * Registers a planner with the given ID.
   *
   * @param id      the planner ID
   * @param planner the planner instance to register
   */
  public static void register(String id, DaprWorkflowPlanner planner) {
    registry.put(id, planner);
  }

  /**
   * Returns the planner for the given ID.
   *
   * @param id the planner ID
   * @return the planner instance, or {@code null} if not registered
   */
  public static DaprWorkflowPlanner get(String id) {
    return registry.get(id);
  }

  /**
   * Unregisters the planner for the given ID.
   *
   * @param id the planner ID to unregister
   */
  public static void unregister(String id) {
    registry.remove(id);
  }

  /**
   * Returns the set of all registered planner IDs as a string.
   *
   * @return a string representation of the registered planner IDs
   */
  public static String getRegisteredIds() {
    return registry.keySet().toString();
  }
}
