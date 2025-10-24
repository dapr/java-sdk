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
  private final AppHttpPipeline appHttpPipeline;
  private final HttpPipeline httpPipeline;

  // @TODO: add secrets https://github.com/dapr/java-sdk/issues/1280
  // @TODO: add metrics https://github.com/dapr/java-sdk/issues/1281
  // @TODO: add logging https://github.com/dapr/java-sdk/issues/1282
  // @TODO: add nameResolution https://github.com/dapr/java-sdk/issues/1284
  // @TODO: add disallow components https://github.com/dapr/java-sdk/issues/1285 
  // @TODO: add mtls https://github.com/dapr/java-sdk/issues/1286


  /**
   * Creates a new configuration.
   *
   * @param name            Configuration name.
   * @param tracing         TracingConfigParameters tracing configuration
   *                        parameters.
   * @param appHttpPipeline AppHttpPipeline middleware configuration.
   */
  public Configuration(String name, TracingConfigurationSettings tracing,
                       AppHttpPipeline appHttpPipeline) {
    this(name, tracing, appHttpPipeline, null);
  }

  /**
   * Creates a new configuration.
   * 
   * @param name            Configuration name.
   * @param tracing         TracingConfigParameters tracing configuration
   *                        parameters.
   * @param appHttpPipeline AppHttpPipeline middleware configuration.
   * @param httpPipeline    HttpPipeline middleware configuration.
   */
  public Configuration(String name, TracingConfigurationSettings tracing,
                       AppHttpPipeline appHttpPipeline,
                       HttpPipeline httpPipeline) {
    this.name = name;
    this.tracing = tracing;
    this.appHttpPipeline = appHttpPipeline;
    this.httpPipeline = httpPipeline;
  }

  public String getName() {
    return name;
  }

  public TracingConfigurationSettings getTracing() {
    return tracing;
  }

  public AppHttpPipeline getAppHttpPipeline() {
    return appHttpPipeline;
  }

  public HttpPipeline getHttpPipeline() {
    return httpPipeline;
  }
}
