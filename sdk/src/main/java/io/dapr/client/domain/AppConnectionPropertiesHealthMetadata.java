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

package io.dapr.client.domain;

/**
 * AppConnectionPropertiesHealthMetadata describes the application health properties.
 */
public final class AppConnectionPropertiesHealthMetadata {

  private final String healthCheckPath;
  private final String healthProbeInterval;
  private final String healthProbeTimeout;
  private final int healthThreshold;

  /**
   * Constructor for a AppConnectionPropertiesHealthMetadata.
   *
   * @param healthCheckPath of the application
   * @param healthProbeInterval time interval between health probes
   * @param healthProbeTimeout timeout for each health probe
   * @param healthThreshold max number of failed health probes
   */
  public AppConnectionPropertiesHealthMetadata(String healthCheckPath, String healthProbeInterval,
      String healthProbeTimeout, int healthThreshold) {
    this.healthCheckPath = healthCheckPath;
    this.healthProbeInterval = healthProbeInterval;
    this.healthProbeTimeout = healthProbeTimeout;
    this.healthThreshold = healthThreshold;
  }

  public String getHealthCheckPath() {
    return healthCheckPath;
  }

  public String getHealthProbeInterval() {
    return healthProbeInterval;
  }

  public String getHealthProbeTimeout() {
    return healthProbeTimeout;
  }

  public int getHealthThreshold() {
    return healthThreshold;
  }

}
