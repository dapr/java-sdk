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
