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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AgentMetadata {

  @JsonProperty("appid")
  private String appId;

  @JsonProperty("type")
  private String type;

  @JsonProperty("orchestrator")
  private boolean orchestrator;

  @JsonProperty("role")
  private String role = "";

  @JsonProperty("goal")
  private String goal = "";

  @JsonProperty("instructions")
  private List<String> instructions;

  @JsonProperty("statestore")
  private String statestore;

  @JsonProperty("system_prompt")
  private String systemPrompt;

  @JsonProperty("framework")
  private String framework;

  public AgentMetadata() {
  }

  private AgentMetadata(Builder builder) {
    this.appId = builder.appId;
    this.type = builder.type;
    this.orchestrator = builder.orchestrator;
    this.role = builder.role;
    this.goal = builder.goal;
    this.instructions = builder.instructions;
    this.statestore = builder.statestore;
    this.systemPrompt = builder.systemPrompt;
    this.framework = builder.framework;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getAppId() {
    return appId;
  }

  public String getType() {
    return type;
  }

  public boolean isOrchestrator() {
    return orchestrator;
  }

  public String getRole() {
    return role;
  }

  public String getGoal() {
    return goal;
  }

  public List<String> getInstructions() {
    return instructions;
  }

  public String getStatestore() {
    return statestore;
  }

  public String getSystemPrompt() {
    return systemPrompt;
  }

  public String getFramework() {
    return framework;
  }

  public static class Builder {
    private String appId;
    private String type;
    private boolean orchestrator = false;
    private String role = "";
    private String goal = "";
    private List<String> instructions;
    private String statestore;
    private String systemPrompt;
    private String framework;

    public Builder appId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder orchestrator(boolean orchestrator) {
      this.orchestrator = orchestrator;
      return this;
    }

    public Builder role(String role) {
      this.role = role;
      return this;
    }

    public Builder goal(String goal) {
      this.goal = goal;
      return this;
    }

    public Builder instructions(List<String> instructions) {
      this.instructions = instructions;
      return this;
    }

    public Builder statestore(String statestore) {
      this.statestore = statestore;
      return this;
    }

    public Builder systemPrompt(String systemPrompt) {
      this.systemPrompt = systemPrompt;
      return this;
    }

    public Builder framework(String framework) {
      this.framework = framework;
      return this;
    }

    /**
     * Builds the AgentMetadata, validating required fields.
     *
     * @return the constructed AgentMetadata
     */
    public AgentMetadata build() {
      if (appId == null || type == null) {
        throw new IllegalStateException("appId and type are required");
      }
      return new AgentMetadata(this);
    }
  }
}
