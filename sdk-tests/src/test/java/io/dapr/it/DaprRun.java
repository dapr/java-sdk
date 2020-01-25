/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.dapr.it.Retry.callWithRetry;


public class DaprRun {

  private static final AtomicInteger NEXT_APP_ID = new AtomicInteger(0);

  private static final String DAPR_RUN = "dapr run --app-id %s ";

  // the arg in -Dexec.args is the app's port
  private static final String DAPR_COMMAND =
      " -- mvn exec:java -D exec.mainClass=%s -D exec.classpathScope=test -D exec.args=\"%s\"";

  private final DaprPorts ports;

  private final String appName;

  private final int maxWaitMilliseconds;

  private final AtomicBoolean started;

  private final Command command;

  DaprRun(
      DaprPorts ports, String successMessage, Class serviceClass, int maxWaitMilliseconds) {
    this.appName = String.format("%s%d", serviceClass.getSimpleName(), NEXT_APP_ID.getAndIncrement());
    this.command =
      new Command(successMessage, buildDaprCommand(this.appName, serviceClass, ports));
    this.ports = ports;
    this.maxWaitMilliseconds = maxWaitMilliseconds;
    this.started = new AtomicBoolean(false);
  }

  public void start() throws InterruptedException, IOException {
    long start = System.currentTimeMillis();
    this.command.run();
    this.started.set(true);

    long timeLeft = this.maxWaitMilliseconds - (System.currentTimeMillis() - start);
    if (this.ports.getAppPort() != null) {
      callWithRetry(() -> {
        System.out.println("Checking if app is listening on port ...");
        assertListeningOnPort(this.ports.getAppPort());
      }, timeLeft);
    }

    if (this.ports.getHttpPort() != null) {
      timeLeft = this.maxWaitMilliseconds - (System.currentTimeMillis() - start);
      callWithRetry(() -> {
        System.out.println("Checking if Dapr is listening on HTTP port ...");
        assertListeningOnPort(this.ports.getHttpPort());
      }, timeLeft);
    }

    if (this.ports.getGrpcPort() != null) {
      timeLeft = this.maxWaitMilliseconds - (System.currentTimeMillis() - start);
      callWithRetry(() -> {
        System.out.println("Checking if Dapr is listening on GRPC port ...");
        assertListeningOnPort(this.ports.getGrpcPort());
      }, timeLeft);
    }
  }

  public void stop() throws InterruptedException, IOException {
    System.out.println("Stopping dapr application ...");
    Command stopCommand = new Command(
      "app stopped successfully",
      "dapr stop --app-id " + this.appName);
    stopCommand.run();
    System.out.println("Dapr application stopped.");
  }

  public void use() {
    if (this.ports.getHttpPort() != null) {
      System.getProperties().setProperty("dapr.http.port", String.valueOf(this.ports.getHttpPort()));
    }
    if (this.ports.getGrpcPort() != null) {
      System.getProperties().setProperty("dapr.grpc.port", String.valueOf(this.ports.getGrpcPort()));
    }
    System.getProperties().setProperty("dapr.grpc.enabled", Boolean.FALSE.toString());
  }

  public void switchToGRPC() {
    System.getProperties().setProperty("dapr.grpc.enabled", Boolean.TRUE.toString());
  }

  public int getGrpcPort() {
    return ports.getGrpcPort();
  }

  public int getHttpPort() {
    return ports.getHttpPort();
  }

  public int getAppPort() {
    return ports.getAppPort();
  }

  public String getAppName() {
    return appName;
  }

  private static String buildDaprCommand(String appName, Class serviceClass, DaprPorts ports) {
    StringBuilder stringBuilder = new StringBuilder(String.format(DAPR_RUN, appName))
        .append(ports.getAppPort() != null ? " --app-port " + ports.getAppPort() : "")
        .append(ports.getHttpPort() != null ? " --port " + ports.getHttpPort() : "")
        .append(ports.getGrpcPort() != null ? " --grpc-port " + ports.getGrpcPort() : "")
        .append(String.format(DAPR_COMMAND, serviceClass.getCanonicalName(),
            ports.getAppPort() != null ? ports.getAppPort().toString() : ""));
    return stringBuilder.toString();
  }

  private static void assertListeningOnPort(int port) {
    System.out.printf("Checking port %d ...\n", port);

    java.net.SocketAddress socketAddress = new java.net.InetSocketAddress(io.dapr.utils.Constants.DEFAULT_HOSTNAME, port);
    try (java.net.Socket socket = new java.net.Socket()) {
      socket.connect(socketAddress, 1000);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    System.out.printf("Confirmed listening on port %d.\n", port);
  }
}
