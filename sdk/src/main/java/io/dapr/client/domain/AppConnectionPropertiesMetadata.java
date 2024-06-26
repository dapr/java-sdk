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
 * AppConnectionPropertiesMetadata describes the application connection properties.
 */
public final class AppConnectionPropertiesMetadata {

  private final int port;
  private final String protocol;
  private final String channelAddress;
  private final int maxConcurrency;
  private final AppConnectionPropertiesHealthMetadata health;

  /**
   * Constructor for a AppConnectionPropertiesMetadata.
   *
   * @param port of the application
   * @param protocol of the application
   * @param channelAddress host address of the application
   * @param maxConcurrency number of concurrent requests the app can handle
   * @param health health check details of the application
   */
  public AppConnectionPropertiesMetadata(int port, String protocol, String channelAddress, int maxConcurrency,
      AppConnectionPropertiesHealthMetadata health) {
    this.port = port;
    this.protocol = protocol;
    this.channelAddress = channelAddress;
    this.maxConcurrency = maxConcurrency;
    this.health = health;
  }

  public int getPort() {
    return port;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getChannelAddress() {
    return channelAddress;
  }

  public int getMaxConcurrency() {
    return maxConcurrency;
  }

  public AppConnectionPropertiesHealthMetadata getHealth() {
    return health;
  }

}
