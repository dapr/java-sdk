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

/**
 * Shared utility methods for Dapr agent services.
 */
public final class DaprAgentServiceUtil {

  private DaprAgentServiceUtil() {
  }

  /**
   * Sanitizes a name for use as a Dapr workflow identifier.
   * Replaces any non-alphanumeric characters (except hyphens and underscores)
   * with underscores.
   *
   * @param name the name to sanitize
   * @return the sanitized name
   */
  public static String safeName(String name) {
    if (name == null || name.isEmpty()) {
      return "unnamed";
    }
    return name.replaceAll("[^a-zA-Z0-9_-]", "_");
  }
}
