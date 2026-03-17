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

public class LlmMetadata {

  @JsonProperty("client")
  private String client;

  @JsonProperty("provider")
  private String provider;

  @JsonProperty("api")
  private String api = "unknown";

  @JsonProperty("model")
  private String model = "unknown";

  @JsonProperty("component_name")
  private String componentName;

  @JsonProperty("base_url")
  private String baseUrl;

  @JsonProperty("azure_endpoint")
  private String azureEndpoint;

  @JsonProperty("azure_deployment")
  private String azureDeployment;

  @JsonProperty("prompt_template")
  private String promptTemplate;

  public LlmMetadata() {
  }

  private LlmMetadata(Builder builder) {
    this.client = builder.client;
    this.provider = builder.provider;
    this.api = builder.api;
    this.model = builder.model;
    this.componentName = builder.componentName;
    this.baseUrl = builder.baseUrl;
    this.azureEndpoint = builder.azureEndpoint;
    this.azureDeployment = builder.azureDeployment;
    this.promptTemplate = builder.promptTemplate;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getClient() {
    return client;
  }

  public String getProvider() {
    return provider;
  }

  public String getApi() {
    return api;
  }

  public String getModel() {
    return model;
  }

  public String getComponentName() {
    return componentName;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getAzureEndpoint() {
    return azureEndpoint;
  }

  public String getAzureDeployment() {
    return azureDeployment;
  }

  public String getPromptTemplate() {
    return promptTemplate;
  }

  public static class Builder {
    private String client;
    private String provider;
    private String api = "unknown";
    private String model = "unknown";
    private String componentName;
    private String baseUrl;
    private String azureEndpoint;
    private String azureDeployment;
    private String promptTemplate;

    public Builder client(String client) {
      this.client = client;
      return this;
    }

    public Builder provider(String provider) {
      this.provider = provider;
      return this;
    }

    public Builder api(String api) {
      this.api = api;
      return this;
    }

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder componentName(String componentName) {
      this.componentName = componentName;
      return this;
    }

    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public Builder azureEndpoint(String azureEndpoint) {
      this.azureEndpoint = azureEndpoint;
      return this;
    }

    public Builder azureDeployment(String azureDeployment) {
      this.azureDeployment = azureDeployment;
      return this;
    }

    public Builder promptTemplate(String promptTemplate) {
      this.promptTemplate = promptTemplate;
      return this;
    }

    /**
     * Builds the LlmMetadata, validating required fields.
     *
     * @return the constructed LlmMetadata
     */
    public LlmMetadata build() {
      if (client == null || provider == null) {
        throw new IllegalStateException("client and provider are required");
      }
      return new LlmMetadata(this);
    }
  }
}
