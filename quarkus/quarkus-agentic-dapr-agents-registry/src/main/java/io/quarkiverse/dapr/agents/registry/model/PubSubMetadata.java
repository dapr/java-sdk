package io.quarkiverse.dapr.agents.registry.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PubSubMetadata {

    @JsonProperty("name")
    private String name;

    @JsonProperty("broadcast_topic")
    private String broadcastTopic;

    @JsonProperty("agent_topic")
    private String agentTopic;

    public PubSubMetadata() {
    }

    private PubSubMetadata(Builder builder) {
        this.name = builder.name;
        this.broadcastTopic = builder.broadcastTopic;
        this.agentTopic = builder.agentTopic;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public String getBroadcastTopic() {
        return broadcastTopic;
    }

    public String getAgentTopic() {
        return agentTopic;
    }

    public static class Builder {
        private String name;
        private String broadcastTopic;
        private String agentTopic;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder broadcastTopic(String broadcastTopic) {
            this.broadcastTopic = broadcastTopic;
            return this;
        }

        public Builder agentTopic(String agentTopic) {
            this.agentTopic = agentTopic;
            return this;
        }

        public PubSubMetadata build() {
            if (name == null) {
                throw new IllegalStateException("name is required");
            }
            return new PubSubMetadata(this);
        }
    }
}