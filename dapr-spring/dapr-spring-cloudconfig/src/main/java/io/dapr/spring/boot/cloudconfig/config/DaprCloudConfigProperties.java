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

package io.dapr.spring.boot.cloudconfig.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The properties for creating dapr client.
 */
@ConfigurationProperties(DaprCloudConfigProperties.PROPERTY_PREFIX)
public class DaprCloudConfigProperties {

  public static final String PROPERTY_PREFIX = "dapr.cloudconfig";

  /**
   * whether enable cloud config.
   */
  private Boolean enabled = true;

  /**
   * whether enable dapr client wait for sidecar, if no response, will throw IOException.
   */
  private Boolean waitSidecarEnabled = false;

  /**
   * retries of dapr client wait for sidecar.
   */
  private Integer waitSidecarRetries = 3;

  /**
   * get config timeout (include wait for dapr sidecar).
   */
  private Integer timeout = 2000;

  public Integer getTimeout() {
    return timeout;
  }

  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Boolean getWaitSidecarEnabled() {
    return waitSidecarEnabled;
  }

  public void setWaitSidecarEnabled(Boolean waitSidecarEnabled) {
    this.waitSidecarEnabled = waitSidecarEnabled;
  }

  public Integer getWaitSidecarRetries() {
    return waitSidecarRetries;
  }

  public void setWaitSidecarRetries(Integer waitSidecarRetries) {
    this.waitSidecarRetries = waitSidecarRetries;
  }
}
