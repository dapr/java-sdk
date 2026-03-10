/*
 * Copyright 2025 The Dapr Authors
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

package io.quarkiverse.dapr.agents.registry.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentMetadataSchema {

  @JsonProperty("schema_version")
  private String schemaVersion;

  @JsonProperty("agent")
  private AgentMetadata agent;

  @JsonProperty("name")
  private String name;

  @JsonProperty("registered_at")
  private String registeredAt;

  @JsonProperty("pubsub")
  private PubSubMetadata pubsub;

  @JsonProperty("memory")
  private MemoryMetadata memory;

  @JsonProperty("llm")
  private LlmMetadata llm;

  @JsonProperty("registry")
  private RegistryMetadata registry;

  @JsonProperty("tools")
  private List<ToolMetadata> tools;

  @JsonProperty("max_iterations")
  private Integer maxIterations;

  @JsonProperty("tool_choice")
  private String toolChoice;

  @JsonProperty("agent_metadata")
  private Map<String, Object> agentMetadata;

  public AgentMetadataSchema() {
  }

  private AgentMetadataSchema(Builder builder) {
    this.schemaVersion = builder.schemaVersion;
    this.agent = builder.agent;
    this.name = builder.name;
    this.registeredAt = builder.registeredAt;
    this.pubsub = builder.pubsub;
    this.memory = builder.memory;
    this.llm = builder.llm;
    this.registry = builder.registry;
    this.tools = builder.tools;
    this.maxIterations = builder.maxIterations;
    this.toolChoice = builder.toolChoice;
    this.agentMetadata = builder.agentMetadata;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getSchemaVersion() {
    return schemaVersion;
  }

  public AgentMetadata getAgent() {
    return agent;
  }

  public String getName() {
    return name;
  }

  public String getRegisteredAt() {
    return registeredAt;
  }

  public PubSubMetadata getPubsub() {
    return pubsub;
  }

  public MemoryMetadata getMemory() {
    return memory;
  }

  public LlmMetadata getLlm() {
    return llm;
  }

  public RegistryMetadata getRegistry() {
    return registry;
  }

  public List<ToolMetadata> getTools() {
    return tools;
  }

  public Integer getMaxIterations() {
    return maxIterations;
  }

  public String getToolChoice() {
    return toolChoice;
  }

  public Map<String, Object> getAgentMetadata() {
    return agentMetadata;
  }

  public static class Builder {
    private String schemaVersion;
    private AgentMetadata agent;
    private String name;
    private String registeredAt;
    private PubSubMetadata pubsub;
    private MemoryMetadata memory;
    private LlmMetadata llm;
    private RegistryMetadata registry;
    private List<ToolMetadata> tools;
    private Integer maxIterations;
    private String toolChoice;
    private Map<String, Object> agentMetadata;

    public Builder schemaVersion(String schemaVersion) {
      this.schemaVersion = schemaVersion;
      return this;
    }

    public Builder agent(AgentMetadata agent) {
      this.agent = agent;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder registeredAt(String registeredAt) {
      this.registeredAt = registeredAt;
      return this;
    }

    public Builder pubsub(PubSubMetadata pubsub) {
      this.pubsub = pubsub;
      return this;
    }

    public Builder memory(MemoryMetadata memory) {
      this.memory = memory;
      return this;
    }

    public Builder llm(LlmMetadata llm) {
      this.llm = llm;
      return this;
    }

    public Builder registry(RegistryMetadata registry) {
      this.registry = registry;
      return this;
    }

    public Builder tools(List<ToolMetadata> tools) {
      this.tools = tools;
      return this;
    }

    /**
     * Adds a single tool to the tools list, initializing the list if needed.
     *
     * @param tool the tool metadata to add
     * @return this builder
     */
    public Builder addTool(ToolMetadata tool) {
      if (this.tools == null) {
        this.tools = new ArrayList<>();
      }
      this.tools.add(tool);
      return this;
    }

    public Builder maxIterations(Integer maxIterations) {
      this.maxIterations = maxIterations;
      return this;
    }

    public Builder toolChoice(String toolChoice) {
      this.toolChoice = toolChoice;
      return this;
    }

    public Builder agentMetadata(Map<String, Object> agentMetadata) {
      this.agentMetadata = agentMetadata;
      return this;
    }

    /**
     * Builds the AgentMetadataSchema, validating required fields.
     *
     * @return the constructed AgentMetadataSchema
     */
    public AgentMetadataSchema build() {
      if (schemaVersion == null || agent == null || name == null || registeredAt == null) {
        throw new IllegalStateException("schemaVersion, agent, name, and registeredAt are required");
      }
      return new AgentMetadataSchema(this);
    }
  }
}
