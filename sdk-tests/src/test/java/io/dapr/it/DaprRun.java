/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import org.junit.Assert;

import java.io.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class DaprRun {

  private static final String DAPR_RUN = "dapr run --app-id %s ";

  // the arg in -Dexec.args is the app's port
  private static final String DAPR_COMMAND = " -- mvn exec:java -Dexec.mainClass=%s -Dexec.classpathScope=test -Dexec.args=\"%d\"";

  private final DaprPorts ports;

  private final String appName;

  private final String successMessage;

  private final Class serviceClass;

  private final Boolean useAppPort;

  private final int maxWaitMilliseconds;

  private volatile AtomicBoolean started;

  private Process proc;

  DaprRun(
    DaprPorts ports, String successMessage, Class serviceClass, Boolean useAppPort, int maxWaitMilliseconds) {
    this.ports = ports;
    this.appName = UUID.randomUUID().toString().replaceAll("-", "");
    this.successMessage = successMessage;
    this.serviceClass = serviceClass;
    this.useAppPort = useAppPort;
    this.maxWaitMilliseconds = maxWaitMilliseconds;
    this.started = new AtomicBoolean(false);
  }

  public void start() throws Exception {
    final Semaphore success = new Semaphore(0);

    String daprCommand = this.buildDaprCommand();
    System.out.println(daprCommand);
    proc = Runtime.getRuntime().exec(daprCommand);

    final Runnable stuffToDo = new Thread(() -> {
      try {
        try (InputStream stdin = proc.getInputStream()) {
          try (InputStreamReader isr = new InputStreamReader(stdin)) {
            try (BufferedReader br = new BufferedReader(isr)) {
              String line;
              while ((line = br.readLine()) != null) {
                System.out.println(line);
                if (line.contains(successMessage)) {
                  this.started.set(true);
                  success.release();
                  break;
                }
              }
            }
          }
        }
      } catch (IOException ex) {
        Assert.fail(ex.getMessage());
      }
    });

    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final Future future = executor.submit(stuffToDo);
    executor.shutdown(); // This does not cancel the already-scheduled task.
    future.get(1, TimeUnit.MINUTES);
    success.tryAcquire(this.maxWaitMilliseconds, TimeUnit.MILLISECONDS);
  }

  public boolean hasStarted() {
    return this.started.get();
  }

  public void stop() throws IOException {
    System.out.println("Stopping dapr application ...");
    Runtime.getRuntime().exec("dapr stop --app-id " + this.appName);
    System.out.println("Dapr application stopped.");

    System.out.println("Stopping dapr cli ...");
    Optional.ofNullable(this.proc).ifPresent(p -> p.destroy());
    System.out.println("Stopped dapr cli.");
  }

  public void use() {
    System.getProperties().setProperty("dapr.http.port", String.valueOf(this.ports.getHttpPort()));
    System.getProperties().setProperty("dapr.grpc.port", String.valueOf(this.ports.getGrpcPort()));
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

  private String buildDaprCommand() {
    StringBuilder stringBuilder = new StringBuilder(String.format(DAPR_RUN, this.appName))
      .append(this.useAppPort ? "--app-port " + this.ports.getAppPort() : "")
      .append(" --grpc-port ")
      .append(this.ports.getGrpcPort())
      .append(" --port ")
      .append(this.ports.getHttpPort())
      .append(String.format(DAPR_COMMAND, this.serviceClass.getCanonicalName(), this.ports.getAppPort()));
    return stringBuilder.toString();
  }

}
