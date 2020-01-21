/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import org.junit.Assert;

import java.io.*;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;


public class DaprIntegrationTestingRunner {

  public static   DaprIntegrationTestingRunner.DaprFreePorts DAPR_FREEPORTS;

  static {
    try {
      DAPR_FREEPORTS = new DaprIntegrationTestingRunner.DaprFreePorts().initPorts();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Runtime rt = Runtime.getRuntime();
  private Process proc;

  private String successMessage;
  private Class serviceClass;
  private Boolean useAppPort;
  private int sleepTime;
  private String appName;

  DaprIntegrationTestingRunner(String successMessage, Class serviceClass, Boolean useAppPort, int sleepTime) {
    this.successMessage = successMessage;
    this.serviceClass = serviceClass;
    this.useAppPort = useAppPort;
    this.sleepTime = sleepTime;
    this.generateAppName();
  }

  public DaprFreePorts initializeDapr() throws Exception {
    String daprCommand=this.buildDaprCommand();
    System.out.println(daprCommand);
    proc= rt.exec(daprCommand);

    final Runnable stuffToDo = new Thread(() -> {
      try {
        try (InputStream stdin = proc.getInputStream()) {
          try(InputStreamReader isr = new InputStreamReader(stdin)) {
            try (BufferedReader br = new BufferedReader(isr)){
              String line;
              while ((line = br.readLine()) != null) {
                System.out.println(line);
                if (line.contains(successMessage)) {
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
    Thread.sleep(sleepTime);
    return DAPR_FREEPORTS;
  }

  private static final String DAPR_RUN = "dapr run --app-id %s ";

  // the arg in -Dexec.args is the app's port
  private static final String DAPR_COMMAND = " -- mvn exec:java -Dexec.mainClass=%s -Dexec.classpathScope=test -Dexec.args=\"%d\"";

  private String buildDaprCommand(){
    StringBuilder stringBuilder= new StringBuilder(String.format(DAPR_RUN, this.appName))
      .append(this.useAppPort ? "--app-port " + this.DAPR_FREEPORTS.appPort : "")
      .append(String.format(DAPR_COMMAND, this.serviceClass.getCanonicalName(),this.DAPR_FREEPORTS.appPort));
    return stringBuilder.toString();
  }

  private void generateAppName(){

    this.appName=UUID.randomUUID().toString();
  }

  private static Integer findRandomOpenPortOnAllLocalInterfaces() throws Exception {
    try (
      ServerSocket socket = new ServerSocket(0)
    ) {
      return socket.getLocalPort();

    }
  }

  public  void destroyDapr() {
    Optional.ofNullable(rt).ifPresent( runtime -> {
      try {
        runtime.exec("dapr stop --app-id " + this.appName);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    Optional.ofNullable(proc).ifPresent(process -> process.destroy());
  }

  public static class DaprFreePorts
  {
    public  DaprFreePorts initPorts() throws Exception {
      this.appPort= findRandomOpenPortOnAllLocalInterfaces();
      this.grpcPort= findRandomOpenPortOnAllLocalInterfaces();
      this.httpPort= findRandomOpenPortOnAllLocalInterfaces();
      return  this;
    }

    private int grpcPort;

    public int getGrpcPort() {
      return grpcPort;
    }

    public int getHttpPort() {
      return httpPort;
    }

    public int getAppPort() {
      return appPort;
    }

    private int httpPort;

    private int appPort;
  }
}
