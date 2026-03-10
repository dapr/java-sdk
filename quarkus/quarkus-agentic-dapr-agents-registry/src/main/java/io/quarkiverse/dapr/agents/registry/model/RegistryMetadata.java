package io.quarkiverse.dapr.agents.registry.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegistryMetadata {

    @JsonProperty("statestore")
    private String statestore;

    @JsonProperty("name")
    private String name;

    public RegistryMetadata() {
    }

    private RegistryMetadata(Builder builder) {
        this.statestore = builder.statestore;
        this.name = builder.name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getStatestore() {
        return statestore;
    }

    public String getName() {
        return name;
    }

    public static class Builder {
        private String statestore;
        private String name;

        public Builder statestore(String statestore) {
            this.statestore = statestore;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public RegistryMetadata build() {
            return new RegistryMetadata(this);
        }
    }
}