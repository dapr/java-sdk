/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.it;

import io.dapr.config.Properties;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DaprPorts {

  private final Integer grpcPort;

  private final Integer httpPort;

  private final Integer appPort;

  private DaprPorts(Integer appPort, Integer httpPort, Integer grpcPort) {
    this.grpcPort = grpcPort;
    this.httpPort = httpPort;
    this.appPort = appPort;
  }

  public static DaprPorts build(boolean appPort, boolean httpPort, boolean grpcPort) {
    try {
      List<Integer> freePorts = new ArrayList<>(findFreePorts(3));
      return new DaprPorts(
          appPort ? freePorts.get(0) : null,
          httpPort ? freePorts.get(1) : null,
          grpcPort ? freePorts.get(2) : null);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void use() {
    if (this.httpPort != null) {
      System.getProperties().setProperty(Properties.HTTP_PORT.getName(), String.valueOf(this.httpPort));
      System.getProperties().setProperty(
              Properties.HTTP_ENDPOINT.getName(), "http://127.0.0.1:" + this.httpPort);
    }

    if (this.grpcPort != null) {
      System.getProperties().setProperty(Properties.GRPC_PORT.getName(), String.valueOf(this.grpcPort));
      System.getProperties().setProperty(
              Properties.GRPC_ENDPOINT.getName(), "http://127.0.0.1:" + this.grpcPort);
    }
  }

  public Integer getGrpcPort() {
    return grpcPort;
  }

  public Integer getHttpPort() {
    return httpPort;
  }

  public Integer getAppPort() {
    return appPort;
  }

  private static Set<Integer> findFreePorts(int n) throws IOException {
    Set<Integer> output = new HashSet<>();
    for (int i = 0; i < n;) {
      try (ServerSocket socket = new ServerSocket(0)) {
        socket.setReuseAddress(true);
        int port = socket.getLocalPort();
        if (!output.contains(port)) {
          output.add(port);
          i++;
        }
      }
    }
    return output;
  }
}
