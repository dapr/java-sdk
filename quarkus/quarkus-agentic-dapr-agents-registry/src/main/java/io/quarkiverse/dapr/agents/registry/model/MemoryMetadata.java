package io.quarkiverse.dapr.agents.registry.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MemoryMetadata {

    @JsonProperty("type")
    private String type;

    @JsonProperty("statestore")
    private String statestore;

    public MemoryMetadata() {
    }

    private MemoryMetadata(Builder builder) {
        this.type = builder.type;
        this.statestore = builder.statestore;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getType() {
        return type;
    }

    public String getStatestore() {
        return statestore;
    }

    public static class Builder {
        private String type;
        private String statestore;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder statestore(String statestore) {
            this.statestore = statestore;
            return this;
        }

        public MemoryMetadata build() {
            if (type == null) {
                throw new IllegalStateException("type is required");
            }
            return new MemoryMetadata(this);
        }
    }
}