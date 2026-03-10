package io.quarkiverse.dapr.agents.registry.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ToolMetadata {

    @JsonProperty("tool_name")
    private String toolName;

    @JsonProperty("tool_description")
    private String toolDescription;

    @JsonProperty("tool_args")
    private String toolArgs;

    public ToolMetadata() {
    }

    private ToolMetadata(Builder builder) {
        this.toolName = builder.toolName;
        this.toolDescription = builder.toolDescription;
        this.toolArgs = builder.toolArgs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolDescription() {
        return toolDescription;
    }

    public String getToolArgs() {
        return toolArgs;
    }

    public static class Builder {
        private String toolName;
        private String toolDescription;
        private String toolArgs;

        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        public Builder toolDescription(String toolDescription) {
            this.toolDescription = toolDescription;
            return this;
        }

        public Builder toolArgs(String toolArgs) {
            this.toolArgs = toolArgs;
            return this;
        }

        public ToolMetadata build() {
            if (toolName == null || toolDescription == null || toolArgs == null) {
                throw new IllegalStateException("toolName, toolDescription, and toolArgs are required");
            }
            return new ToolMetadata(this);
        }
    }
}