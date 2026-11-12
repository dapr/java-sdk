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
 * limitations under the License.
 */
package io.dapr.it.testcontainers.support;

import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.config.Property;

import java.net.URI;
import java.util.Map;

/** Addresses for either the Docker network or the test JVM. */
public final class Endpoints {
  private Endpoints() { }

  /** Returns a URI with IPv6 hosts correctly bracketed. */
  public static URI http(String host, int port) {
    try {
      return new URI("http", null, host, port, null, null, null);
    } catch (java.net.URISyntaxException e) {
      throw new IllegalArgumentException("Invalid endpoint host: " + host, e);
    }
  }

  /** Creates a client with explicit mapped endpoints. */
  public static DaprClientBuilder client(String host, int httpPort, int grpcPort) {
    return new DaprClientBuilder().withPropertyOverrides(overrides(host, httpPort, grpcPort));
  }

  static Map<Property<?>, String> overrides(String host, int httpPort, int grpcPort) {
    String grpc = http(host, grpcPort).getRawAuthority();
    return Map.of(Properties.HTTP_ENDPOINT, http(host, httpPort).toString(), Properties.GRPC_ENDPOINT, grpc,
        Properties.SIDECAR_IP, host, Properties.HTTP_PORT, Integer.toString(httpPort),
        Properties.GRPC_PORT, Integer.toString(grpcPort));
  }
}
