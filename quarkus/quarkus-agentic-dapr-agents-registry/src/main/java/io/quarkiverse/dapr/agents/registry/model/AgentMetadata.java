package io.quarkiverse.dapr.agents.registry.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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

        public AgentMetadata build() {
            if (appId == null || type == null) {
                throw new IllegalStateException("appId and type are required");
            }
            return new AgentMetadata(this);
        }
    }
}