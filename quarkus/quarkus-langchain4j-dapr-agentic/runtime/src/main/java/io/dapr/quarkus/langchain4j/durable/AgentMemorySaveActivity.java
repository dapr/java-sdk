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

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.quarkiverse.dapr.workflows.ActivityMetadata;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Persists the conversation for a {@code @MemoryId} agent at the end of a {@link ReActAgentWorkflow}
 * run. The save is an idempotent <em>replace</em> (see {@link DurableChatMemory}), so it is safe
 * under Dapr's at-least-once activity delivery.
 */
@ApplicationScoped
@ActivityMetadata(name = "memory-save")
public class AgentMemorySaveActivity implements WorkflowActivity {

  @Override
  public Object run(WorkflowActivityContext ctx) {
    MemorySaveInput input = ctx.getInput(MemorySaveInput.class);
    List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(input.messagesJson());
    DurableChatMemory.save(input.memoryId(), messages);
    return input.memoryId();
  }
}
