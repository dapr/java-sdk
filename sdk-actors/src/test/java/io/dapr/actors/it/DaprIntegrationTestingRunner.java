package io.dapr.actors.it;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DaprIntegrationTestingRunner {

  private static AtomicInteger appGeneratorId = new AtomicInteger();

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
  public DaprIntegrationTestingRunner.DaprFreePorts DAPR_FREEPORTS;

  private Runtime rt = Runtime.getRuntime();
  private Process proc;

  private String successMessage;
  private Class serviceClass;
  private Boolean useAppPort;
  private Boolean useGrpcPort;
  private Boolean useHttpPort;
  private int sleepTime;
  private String appName;
  private boolean appRanOK = Boolean.FALSE;
  private boolean isClient = false;

  DaprIntegrationTestingRunner(String successMessage, Class serviceClass, Boolean useAppPort, Boolean useGrpcPort, Boolean useHttpPort, int sleepTime, boolean isClient) {
    this.successMessage = successMessage;
    this.serviceClass = serviceClass;
    this.useAppPort = useAppPort;
    this.useGrpcPort = useGrpcPort;
    this.useHttpPort = useHttpPort;
    this.sleepTime = sleepTime;
    this.isClient = isClient;
    this.generateAppName();
    try {
      DAPR_FREEPORTS = new DaprIntegrationTestingRunner.DaprFreePorts().initPorts();
      environmentVariables.set("DAPR_HTTP_PORT", String.valueOf(DAPR_FREEPORTS.getHttpPort()));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public DaprFreePorts initializeDapr() throws Exception {
    String daprCommand=this.buildDaprCommand();
    System.out.println(daprCommand);
    proc= rt.exec(daprCommand);

    final Thread stuffToDo = new Thread(() -> {
      try {
        try (InputStream stdin = proc.getInputStream()) {
          try(InputStreamReader isr = new InputStreamReader(stdin)) {
            try (BufferedReader br = new BufferedReader(isr)){
              String line;
              while ((line = br.readLine()) != null) {
                System.out.println(line);
                if (line.contains(successMessage)) {
                  this.appRanOK = true;
                }
              }
            }
          }

        }
      } catch (IOException ex) {
        Assert.fail(ex.getMessage());
      }
    });
    stuffToDo.start();
    Thread.sleep(sleepTime);
    return DAPR_FREEPORTS;
  }

  private static final String DAPR_RUN = "dapr run --app-id %s ";

  /**
   * The args in -Dexec.args are the App name, and if needed the app's port.
   * The args are passed as a CSV due to conflict of parsing a space separated list in different OS
   */
  private static final String DAPR_COMMAND = " -- mvn exec:java -Dexec.mainClass=%s -Dexec.classpathScope=test -Dexec.args=\"%s,%s\"";

  private String buildDaprCommand(){
    StringBuilder stringBuilder= new StringBuilder(String.format(DAPR_RUN, this.appName))
        .append(this.useAppPort ? "--app-port " + this.DAPR_FREEPORTS.appPort : "")
        .append(this.useGrpcPort ? " --grpc-port " + this.DAPR_FREEPORTS.grpcPort : "")
        .append(this.useHttpPort ? " --port " + this.DAPR_FREEPORTS.httpPort : "")
        .append(String.format(DAPR_COMMAND, this.serviceClass.getCanonicalName(), this.appName, buildPortsParamCommands()));
    return stringBuilder.toString();
  }

  private String buildPortsParamCommands() {
    StringBuilder ports = new StringBuilder();
    if (this.useAppPort) {
      ports.append(this.DAPR_FREEPORTS.appPort);
    }
    return ports.toString();
  }

  private void generateAppName(){
    this.appName="DAPRapp" + appGeneratorId.incrementAndGet();
  }

  public boolean isAppRanOK() {
    return appRanOK;
  }

  public String getAppName() {
    return appName;
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

  public static class DaprFreePorts {
    private int grpcPort;
    private int httpPort;
    private int appPort;

    public  DaprFreePorts initPorts() throws Exception {
      this.appPort = findRandomOpenPortOnAllLocalInterfaces();
      this.grpcPort = findRandomOpenPortOnAllLocalInterfaces();
      this.httpPort = findRandomOpenPortOnAllLocalInterfaces();
      return  this;
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
  }
}
