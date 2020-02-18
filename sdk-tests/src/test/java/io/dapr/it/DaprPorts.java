/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

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
      List<Integer> freePorts = findFreePorts(3);
      return new DaprPorts(
          appPort ? freePorts.get(0) : null,
          httpPort ? freePorts.get(1) : null,
          grpcPort ? freePorts.get(2) : null);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static DaprPorts build() throws IOException {
    return build(true, true, true);
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

  private static List<Integer> findFreePorts(int n) throws IOException {
    if (n <= 0) {
      return new ArrayList<>();
    }
    try (
      ServerSocket socket = new ServerSocket(0)
    ) {
      socket.setReuseAddress(true);
      int port = socket.getLocalPort();
      List<Integer> output = findFreePorts(n - 1);
      output.add(port);
      return output;
    }
  }
}
