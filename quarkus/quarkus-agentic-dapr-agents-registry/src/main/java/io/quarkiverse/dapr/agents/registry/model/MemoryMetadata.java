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

    /**
     * Builds a new MemoryMetadata instance.
     *
     * @return the built MemoryMetadata
     */
    public MemoryMetadata build() {
      if (type == null) {
        throw new IllegalStateException("type is required");
      }
      return new MemoryMetadata(this);
    }
  }
}
