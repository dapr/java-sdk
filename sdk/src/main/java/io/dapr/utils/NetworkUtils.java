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

package io.dapr.utils;

import io.dapr.config.Properties;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Utility methods for network, internal to Dapr SDK.
 */
public final class NetworkUtils {

  private static final long RETRY_WAIT_MILLISECONDS = 1000;

  private NetworkUtils() {
  }

  /**
   * Tries to connect to a socket, retrying every 1 second.
   * @param host Host to connect to.
   * @param port Port to connect to.
   * @param timeoutInMilliseconds Timeout in milliseconds to give up trying.
   * @throws InterruptedException If retry is interrupted.
   */
  public static void waitForSocket(String host, int port, int timeoutInMilliseconds) throws InterruptedException {
    long started = System.currentTimeMillis();
    callWithRetry(() -> {
      try {
        try (Socket socket = new Socket()) {
          // timeout cannot be negative.
          // zero timeout means infinite, so 1 is the practical minimum.
          int remainingTimeout = (int) Math.max(1, timeoutInMilliseconds - (System.currentTimeMillis() - started));
          socket.connect(new InetSocketAddress(host, port), remainingTimeout);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, timeoutInMilliseconds);
  }

  /**
   * Creates a GRPC managed channel.
   * @param interceptors Optional interceptors to add to the channel.
   * @return GRPC managed channel to communicate with the sidecar.
   */
  public static ManagedChannel buildGrpcManagedChannel(ClientInterceptor... interceptors) {
    String address = Properties.SIDECAR_IP.get();
    int port = Properties.GRPC_PORT.get();
    boolean insecure = true;
    String grpcEndpoint = Properties.GRPC_ENDPOINT.get();
    if ((grpcEndpoint != null) && !grpcEndpoint.isEmpty()) {
      URI uri = URI.create(grpcEndpoint);
      insecure = uri.getScheme().equalsIgnoreCase("http");
      port = uri.getPort() > 0 ? uri.getPort() : (insecure ? 80 : 443);
      address = uri.getHost();
      if ((uri.getPath() != null) && !uri.getPath().isEmpty()) {
        address += uri.getPath();
      }
    }
    ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(address, port)
        .userAgent(Version.getSdkVersion());
    if (insecure) {
      builder = builder.usePlaintext();
    }
    if (interceptors != null && interceptors.length > 0) {
      builder = builder.intercept(interceptors);
    }
    return builder.build();
  }

  private static void callWithRetry(Runnable function, long retryTimeoutMilliseconds) throws InterruptedException {
    long started = System.currentTimeMillis();
    while (true) {
      Throwable exception;
      try {
        function.run();
        return;
      } catch (Exception e) {
        exception = e;
      } catch (AssertionError e) {
        exception = e;
      }

      long elapsed = System.currentTimeMillis() - started;
      if (elapsed >= retryTimeoutMilliseconds) {
        if (exception instanceof RuntimeException) {
          throw (RuntimeException)exception;
        }

        throw new RuntimeException(exception);
      }

      long remaining = retryTimeoutMilliseconds - elapsed;
      Thread.sleep(Math.min(remaining, RETRY_WAIT_MILLISECONDS));
    }
  }

    /**
   * Retrieve loopback address for the host.
   * @return The loopback address String
   */
  public static String getHostLoopbackAddress() {
    return InetAddress.getLoopbackAddress().getHostAddress();
  }
}