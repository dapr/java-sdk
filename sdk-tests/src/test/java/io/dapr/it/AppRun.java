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
import java.util.HashMap;

import static io.dapr.it.Retry.callWithRetry;


/**
 * This class runs an app outside Dapr but adds Dapr env variables.
 */
public class AppRun implements Stoppable {

  private static final String APP_COMMAND =
      "mvn exec:java -B -D exec.mainClass=%s -D exec.classpathScope=test -D exec.args=\"%s\"";

  private final DaprPorts ports;

  private final int maxWaitMilliseconds;

  private final Command command;

  AppRun(DaprPorts ports,
                 String successMessage,
                 Class serviceClass,
                 int maxWaitMilliseconds) {
    this.command = new Command(
            successMessage,
            buildCommand(serviceClass, ports),
            new HashMap<>() {{
              put("DAPR_HTTP_PORT", ports.getHttpPort().toString());
              put("DAPR_GRPC_PORT", ports.getGrpcPort().toString());
            }});
    this.ports = ports;
    this.maxWaitMilliseconds = maxWaitMilliseconds;
  }

  public void start() throws InterruptedException, IOException {
    long start = System.currentTimeMillis();
    // First, try to stop previous run (if left running).
    this.stop();
    // Wait for the previous run to kill the prior process.
    System.out.println("Starting application ...");
    this.command.run();
    if (this.ports.getAppPort() != null) {
      long timeLeft = this.maxWaitMilliseconds - (System.currentTimeMillis() - start);
      callWithRetry(() -> {
        System.out.println("Checking if app is listening on port ...");
        assertListeningOnPort(this.ports.getAppPort());
      }, timeLeft);
    }
    System.out.println("Application started.");
  }

  @Override
  public void stop() throws InterruptedException {
    System.out.println("Stopping application ...");
    try {
      this.command.stop();

      System.out.println("Application stopped.");
    } catch (RuntimeException e) {
      System.out.println("Could not stop command: " + this.command.toString());
    }
  }

  private static String buildCommand(Class serviceClass, DaprPorts ports) {
    return String.format(APP_COMMAND, serviceClass.getCanonicalName(),
                ports.getAppPort() != null ? ports.getAppPort().toString() : "");
  }

  private static void assertListeningOnPort(int port) {
    System.out.printf("Checking port %d ...\n", port);

    java.net.SocketAddress socketAddress = new java.net.InetSocketAddress(Properties.SIDECAR_IP.get(), port);
    try (java.net.Socket socket = new java.net.Socket()) {
      socket.connect(socketAddress, 1000);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    System.out.printf("Confirmed listening on port %d.\n", port);
  }

  public enum AppProtocol {
    HTTP, GRPC;
  }
}
