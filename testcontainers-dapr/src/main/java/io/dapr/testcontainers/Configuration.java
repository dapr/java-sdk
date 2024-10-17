/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.testcontainers;

/**
 * Represents a Dapr component.
 */
public class Configuration {
  private final String name;
  private final TracingConfigurationSettings tracing;

  //@TODO: add httpPipeline
  //@TODO: add secrets
  //@TODO: add components
  //@TODO: add accessControl

  /**
   * Creates a new configuration.
   * @param name     Configuration name.
   * @param tracing     TracingConfigParameters tracing configuration parameters.
   */
  public Configuration(String name, TracingConfigurationSettings tracing) {
    this.name = name;
    this.tracing = tracing;
  }

  public String getName() {
    return name;
  }

  public TracingConfigurationSettings getTracing() {
    return tracing;
  }
}
