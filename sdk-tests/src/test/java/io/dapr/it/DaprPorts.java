/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it;

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
