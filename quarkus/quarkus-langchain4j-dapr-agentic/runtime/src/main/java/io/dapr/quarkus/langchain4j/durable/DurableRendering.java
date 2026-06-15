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

import dev.langchain4j.model.input.PromptTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared prompt-template rendering for the durable agents, using LangChain4j's
 * {@link PromptTemplate} engine — the same one AiServices uses — so rendered prompts match
 * non-durable behavior ({@code {{var}}}, the single-variable {@code {{it}}}, property navigation
 * like {@code {{user.name}}}, and strict missing-variable checking).
 *
 * <p><b>Determinism:</b> rendering must be a pure function of the (captured) call state, because it
 * runs both at the proxy entry and inside composite orchestrators, which replay. LangChain4j's
 * wall-clock template variables ({@code current_date} / {@code current_time} /
 * {@code current_date_time}) are therefore unsupported here — their value would differ across a
 * replay and break determinism. Pass any time-dependent value as a method argument instead (it is
 * captured once, at the entry).
 */
final class DurableRendering {

  private DurableRendering() {
  }

  /**
   * Renders {@code template} against {@code state}.
   *
   * @param template the template (may be {@code null})
   * @param state    the variables to render from
   * @return the rendered string, or {@code null} if the template was {@code null}
   */
  static String render(String template, Map<String, String> state) {
    if (template == null) {
      return null;
    }
    Map<String, Object> variables = new HashMap<>(state);
    return PromptTemplate.from(template).apply(variables).text();
  }
}
