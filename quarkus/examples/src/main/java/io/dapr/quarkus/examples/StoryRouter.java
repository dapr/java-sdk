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

package io.dapr.quarkus.examples;

import dev.langchain4j.agentic.declarative.ActivationCondition;
import dev.langchain4j.agentic.declarative.ConditionalAgent;
import dev.langchain4j.service.V;

/**
 * Conditional composite: routes a {@code topic} to {@link CreativeWriter} (short topics) or
 * {@link SummaryWriter} (longer topics) via {@code @ActivationCondition} predicates.
 *
 * <p>Both branches consume the same {@code topic} input (so the composite validates), and the
 * routing key is that same input. This runs as the {@code durable-conditional} workflow, which
 * evaluates the pure static conditions and runs the matching sub-agent as a {@code react-agent}
 * child.
 */
public interface StoryRouter {

  @ConditionalAgent(name = "story-router-agent", outputKey = "story",
      subAgents = { CreativeWriter.class, SummaryWriter.class })
  String route(@V("topic") String topic);

  /**
   * Routes short topics to the creative writer.
   *
   * @param topic the topic
   * @return true when the topic is short
   */
  @ActivationCondition(CreativeWriter.class)
  static boolean creativeForShortTopic(@V("topic") String topic) {
    return topic != null && topic.length() < 12;
  }

  /**
   * Routes longer topics to the summary writer.
   *
   * @param topic the topic
   * @return true when the topic is long
   */
  @ActivationCondition(SummaryWriter.class)
  static boolean summaryForLongTopic(@V("topic") String topic) {
    return topic == null || topic.length() >= 12;
  }
}
