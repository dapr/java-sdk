/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.spring.boot.properties.actors;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for hosting Dapr actors from a Spring Boot application.
 */
@ConfigurationProperties(prefix = "dapr.actors")
public class DaprActorProperties {

  private Transport transport = Transport.HTTP;

  /**
   * Returns the transport used by the application to host actors.
   *
   * @return The actor hosting transport. Defaults to HTTP.
   */
  public Transport getTransport() {
    return transport;
  }

  /**
   * Sets the transport used by the application to host actors.
   *
   * @param transport The actor hosting transport.
   */
  public void setTransport(Transport transport) {
    this.transport = transport;
  }

  /**
   * Supported transports for hosting actors.
   */
  public enum Transport {
    /** Host callbacks through the existing HTTP app channel. */
    HTTP,
    /** Host callbacks through the app-initiated gRPC actor event stream. */
    GRPC
  }
}
