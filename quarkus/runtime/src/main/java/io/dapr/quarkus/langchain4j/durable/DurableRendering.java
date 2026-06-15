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

package io.dapr.quarkus.langchain4j.durable;

import java.util.Map;

/**
 * Shared {@code {{var}}} template rendering for the durable composites.
 */
final class DurableRendering {

  private DurableRendering() {
  }

  /**
   * Substitutes {@code {{key}}} placeholders in the template from the given state.
   *
   * @param template the template (may be {@code null})
   * @param state    the state to substitute from
   * @return the rendered string, or {@code null} if the template was {@code null}
   */
  static String render(String template, Map<String, String> state) {
    if (template == null) {
      return null;
    }
    String rendered = template;
    for (Map.Entry<String, String> entry : state.entrySet()) {
      if (entry.getValue() != null) {
        rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
      }
    }
    return rendered;
  }
}
