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

import com.google.protobuf.Empty;
import io.dapr.config.Properties;
import io.dapr.v1.AppCallbackHealthCheckGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static io.dapr.it.Retry.callWithRetry;


public class DaprRun implements Stoppable {

  private static final String DAPR_SUCCESS_MESSAGE = "You're up and running!";

  private static final String DAPR_RUN = "dapr run --app-id %s --app-protocol %s " +
      "--config ./configurations/configuration.yaml " +
      "--resources-path ./components";

  // the arg in -Dexec.args is the app's port
  private static final String DAPR_COMMAND =
      " -- mvn exec:java -D exec.mainClass=%s -D exec.classpathScope=test -D exec.args=\"%s\"";

  private final DaprPorts ports;

  private final String appName;

  private final AppRun.AppProtocol appProtocol;

  private final int maxWaitMilliseconds;

  private final AtomicBoolean started;

  private final Command startCommand;

  private final Command listCommand;

  private final Command stopCommand;

  private final boolean hasAppHealthCheck;

  private DaprRun(String testName,
                  DaprPorts ports,
                  String successMessage,
                  Class serviceClass,
                  int maxWaitMilliseconds,
                  AppRun.AppProtocol appProtocol) {
    // The app name needs to be deterministic since we depend on it to kill previous runs.
    this.appName = serviceClass == null ?
        testName.toLowerCase() :
        String.format("%s-%s", testName, serviceClass.getSimpleName()).toLowerCase();
    this.appProtocol = appProtocol;
    this.startCommand =
        new Command(successMessage, buildDaprCommand(this.appName, serviceClass, ports, appProtocol));
    this.listCommand = new Command(
      this.appName,
      "dapr list");
    this.stopCommand = new Command(
        "app stopped successfully",
        "dapr stop --app-id " + this.appName);
    this.ports = ports;
    this.maxWaitMilliseconds = maxWaitMilliseconds;
    this.started = new AtomicBoolean(false);
    this.hasAppHealthCheck = isAppHealthCheckEnabled(serviceClass);
  }

  public void start() throws InterruptedException, IOException {
    long start = System.currentTimeMillis();
    // First, try to stop previous run (if left running).
    this.stop();
    // Wait for the previous run to kill the prior process.
    long timeLeft = this.maxWaitMilliseconds - (System.currentTimeMillis() - start);
    System.out.println("Checking if previous run for Dapr application has stopped ...");
    checkRunState(timeLeft, false);

    System.out.println("Starting dapr application ...");
    this.startCommand.run();
    this.started.set(true);

    timeLeft = this.maxWaitMilliseconds - (System.currentTimeMillis() - start);
    System.out.println("Checking if Dapr application has started ...");
    checkRunState(timeLeft, true);

    if (this.ports.getAppPort() != null) {
      timeLeft = this.maxWaitMilliseconds - (System.currentTimeMillis() - start);
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
    System.out.println("Dapr application started.");
  }

  @Override
  public void stop() throws InterruptedException, IOException {
    System.out.println("Stopping dapr application ...");
    try {
      this.stopCommand.run();

      System.out.println("Dapr application stopped.");
    } catch (RuntimeException e) {
      System.out.println("Could not stop app " + this.appName + ": " + e.getMessage());
    }
  }

  public void use() {
    this.ports.use();
  }

  public void waitForAppHealth(int maxWaitMilliseconds) throws InterruptedException {
    if (!this.hasAppHealthCheck) {
      return;
    }

    if (AppRun.AppProtocol.GRPC.equals(this.appProtocol)) {
      ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", this.getAppPort())
              .usePlaintext()
              .build();
      try {
        AppCallbackHealthCheckGrpc.AppCallbackHealthCheckBlockingStub stub =
                AppCallbackHealthCheckGrpc.newBlockingStub(channel);
        long maxWait = System.currentTimeMillis() + maxWaitMilliseconds;
        while (System.currentTimeMillis() <= maxWait) {
          try {
            stub.healthCheck(Empty.getDefaultInstance());
            // artursouza: workaround due to race condition with runtime's probe on app's health.
            Thread.sleep(5000);
            return;
          } catch (Exception e) {
            Thread.sleep(1000);
          }
        }

        throw new RuntimeException("timeout: gRPC service is not healthy.");
      } finally {
        channel.shutdown();
      }
    } else {
      // Create an OkHttpClient instance with a custom timeout
      OkHttpClient client = new OkHttpClient.Builder()
              .connectTimeout(maxWaitMilliseconds, TimeUnit.MILLISECONDS)
              .readTimeout(maxWaitMilliseconds, TimeUnit.MILLISECONDS)
              .build();

      // Define the URL to probe
      String url = "http://127.0.0.1:" + this.getAppPort() + "/health"; // Change to your specific URL

      // Create a request to the URL
      Request request = new Request.Builder()
              .url(url)
              .build();

      // Execute the request
      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          throw new RuntimeException("error: HTTP service is not healthy.");
        }
      } catch (IOException e) {
          throw new RuntimeException("exception: HTTP service is not healthy.");
      }

      // artursouza: workaround due to race condition with runtime's probe on app's health.
      Thread.sleep(5000);
    }
  }

  public Integer getGrpcPort() {
    return ports.getGrpcPort();
  }

  public Integer getHttpPort() {
    return ports.getHttpPort();
  }

  public Integer getAppPort() {
    return ports.getAppPort();
  }

  public String getAppName() {
    return appName;
  }

  public void checkRunState(long timeout, boolean shouldBeRunning) throws InterruptedException {
    callWithRetry(() -> {
      try {
        this.listCommand.run();

        if (!shouldBeRunning) {
          throw new RuntimeException("Previous run for app has not stopped yet!");
        }
      } catch (IllegalStateException e) {
        // Bad case if the app is supposed to be running.
        if (shouldBeRunning) {
          throw e;
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }, timeout);
  }

  private static String buildDaprCommand(
      String appName, Class serviceClass, DaprPorts ports, AppRun.AppProtocol appProtocol) {
    StringBuilder stringBuilder =
        new StringBuilder(String.format(DAPR_RUN, appName, appProtocol.toString().toLowerCase()))
            .append(ports.getAppPort() != null ? " --app-port " + ports.getAppPort() : "")
            .append(ports.getHttpPort() != null ? " --dapr-http-port " + ports.getHttpPort() : "")
            .append(ports.getGrpcPort() != null ? " --dapr-grpc-port " + ports.getGrpcPort() : "")
            .append(isAppHealthCheckEnabled(serviceClass) ?
                    " --enable-app-health-check --app-health-probe-interval=1" : "")
            .append(serviceClass == null ? "" :
                String.format(DAPR_COMMAND, serviceClass.getCanonicalName(),
                    ports.getAppPort() != null ? ports.getAppPort().toString() : ""));
    return stringBuilder.toString();
  }

  private static boolean isAppHealthCheckEnabled(Class serviceClass) {
    if (serviceClass != null) {
      DaprRunConfig daprRunConfig = (DaprRunConfig) serviceClass.getAnnotation(DaprRunConfig.class);
      if (daprRunConfig != null) {
        return daprRunConfig.enableAppHealthCheck();
      }
    }

    return false;
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

  static class Builder {

    private final String testName;

    private final Supplier<DaprPorts> portsSupplier;

    private final String successMessage;

    private final int maxWaitMilliseconds;

    private Class serviceClass;

    private AppRun.AppProtocol appProtocol;

    Builder(
        String testName,
        Supplier<DaprPorts> portsSupplier,
        String successMessage,
        int maxWaitMilliseconds,
        AppRun.AppProtocol appProtocol) {
      this.testName = testName;
      this.portsSupplier = portsSupplier;
      this.successMessage = successMessage;
      this.maxWaitMilliseconds = maxWaitMilliseconds;
      this.appProtocol = appProtocol;
    }

    public Builder withServiceClass(Class serviceClass) {
      this.serviceClass = serviceClass;
      return this;
    }

    DaprRun build() {
      return new DaprRun(
              this.testName,
              this.portsSupplier.get(),
              this.successMessage,
              this.serviceClass,
              this.maxWaitMilliseconds,
              this.appProtocol);
    }

    /**
     * Builds app and dapr run separately. It can be useful to force the restart of one of them.
     * @return Pair of AppRun and DaprRun.
     */
    ImmutablePair<AppRun, DaprRun> splitBuild() {
      DaprPorts ports = this.portsSupplier.get();
      AppRun appRun = new AppRun(
              ports,
              this.successMessage,
              this.serviceClass,
              this.maxWaitMilliseconds);

      DaprRun daprRun = new DaprRun(
              this.testName,
              ports,
              DAPR_SUCCESS_MESSAGE,
              null,
              this.maxWaitMilliseconds,
              this.appProtocol);

      return new ImmutablePair<>(appRun, daprRun);
    }
  }
}
