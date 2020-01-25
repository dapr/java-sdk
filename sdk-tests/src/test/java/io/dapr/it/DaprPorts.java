/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import java.io.IOException;
import java.net.ServerSocket;

public class DaprPorts {
  private final int grpcPort;

  private final int httpPort;

  private final int appPort;

  private DaprPorts(int grpcPort, int httpPort, int appPort) {
    this.grpcPort = grpcPort;
    this.httpPort = httpPort;
    this.appPort = appPort;
  }

  public static DaprPorts build() throws IOException {
    return new DaprPorts(
      findRandomOpenPortOnAllLocalInterfaces(),
      findRandomOpenPortOnAllLocalInterfaces(),
      findRandomOpenPortOnAllLocalInterfaces()
    );
  }

  public int getGrpcPort() {
    return grpcPort;
  }

  public int getHttpPort() {
    return httpPort;
  }

  public int getAppPort() {
    return appPort;
  }

  private static Integer findRandomOpenPortOnAllLocalInterfaces() throws IOException {
    try (
      ServerSocket socket = new ServerSocket(0)
    ) {
      return socket.getLocalPort();

    }
  }
}
