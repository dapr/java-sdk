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
import io.dapr.exceptions.DaprError;
import io.dapr.exceptions.DaprException;
import io.grpc.ChannelCredentials;
import io.grpc.ClientInterceptor;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static io.dapr.config.Properties.GRPC_ENABLE_KEEP_ALIVE;
import static io.dapr.config.Properties.GRPC_ENDPOINT;
import static io.dapr.config.Properties.GRPC_KEEP_ALIVE_TIMEOUT_SECONDS;
import static io.dapr.config.Properties.GRPC_KEEP_ALIVE_TIME_SECONDS;
import static io.dapr.config.Properties.GRPC_KEEP_ALIVE_WITHOUT_CALLS;
import static io.dapr.config.Properties.GRPC_MAX_INBOUND_MESSAGE_SIZE_BYTES;
import static io.dapr.config.Properties.GRPC_MAX_INBOUND_METADATA_SIZE_BYTES;
import static io.dapr.config.Properties.GRPC_PORT;
import static io.dapr.config.Properties.GRPC_TLS_CA_PATH;
import static io.dapr.config.Properties.GRPC_TLS_CERT_PATH;
import static io.dapr.config.Properties.GRPC_TLS_INSECURE;
import static io.dapr.config.Properties.GRPC_TLS_KEY_PATH;
import static io.dapr.config.Properties.SIDECAR_IP;

/**
 * Utility methods for network, internal to Dapr SDK.
 */
public final class NetworkUtils {

  private static final long RETRY_WAIT_MILLISECONDS = 1000;

  // Thanks to https://ihateregex.io/expr/ipv6/
  private static final String IPV6_REGEX = "(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|"
      + "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|"
      + "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|"
      + "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|"
      + "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|"
      + ":((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|"
      + "::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|"
      + "1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|"
      + "(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|"
      + "(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))";

  private static final Pattern IPV6_PATTERN = Pattern.compile(IPV6_REGEX, Pattern.CASE_INSENSITIVE);

  // Don't accept "?" to avoid ambiguity with ?tls=true
  private static final String GRPC_ENDPOINT_FILENAME_REGEX_PART = "[^\0\\?]+";

  private static final String GRPC_ENDPOINT_HOSTNAME_REGEX_PART = "(([A-Za-z0-9_\\-\\.]+)|(\\[" + IPV6_REGEX + "\\]))";

  private static final String GRPC_ENDPOINT_DNS_AUTHORITY_REGEX_PART = "(?<dnsWithAuthority>dns://)"
      + "(?<authorityEndpoint>"
      + GRPC_ENDPOINT_HOSTNAME_REGEX_PART + ":[0-9]+)?/";

  private static final String GRPC_ENDPOINT_PARAM_REGEX_PART = "(\\?(?<param>tls\\=((true)|(false))))?";

  private static final String GRPC_ENDPOINT_SOCKET_REGEX_PART = "(?<socket>((unix:)|(unix://)|(unix-abstract:))"
      + GRPC_ENDPOINT_FILENAME_REGEX_PART + ")";

  private static final String GRPC_ENDPOINT_VSOCKET_REGEX_PART = "(?<vsocket>vsock:" + GRPC_ENDPOINT_HOSTNAME_REGEX_PART
      + ":[0-9]+)";
  private static final String GRPC_ENDPOINT_HOST_REGEX_PART = "((?<http>http://)|(?<https>https://)|(?<dns>dns:)|("
      + GRPC_ENDPOINT_DNS_AUTHORITY_REGEX_PART + "))?"
      + "(?<hostname>" + GRPC_ENDPOINT_HOSTNAME_REGEX_PART + ")?+"
      + "(:(?<port>[0-9]+))?";

  private static final String GRPC_ENDPOINT_REGEX = "^("
      + "(" + GRPC_ENDPOINT_HOST_REGEX_PART + ")|"
      + "(" + GRPC_ENDPOINT_SOCKET_REGEX_PART + ")|"
      + "(" + GRPC_ENDPOINT_VSOCKET_REGEX_PART + ")"
      + ")" + GRPC_ENDPOINT_PARAM_REGEX_PART + "$";

  private static final Pattern GRPC_ENDPOINT_PATTERN = Pattern.compile(GRPC_ENDPOINT_REGEX, Pattern.CASE_INSENSITIVE);

  private NetworkUtils() {
  }

  /**
   * Tries to connect to a socket, retrying every 1 second.
   *
   * @param host                  Host to connect to.
   * @param port                  Port to connect to.
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
   * 
   * @param properties   instance to set up the GrpcEndpoint
   * @param interceptors Optional interceptors to add to the channel.
   * @return GRPC managed channel to communicate with the sidecar.
   */
  public static ManagedChannel buildGrpcManagedChannel(Properties properties, ClientInterceptor... interceptors) {
    var settings = GrpcEndpointSettings.parse(properties);

    boolean insecureTls = properties.getValue(GRPC_TLS_INSECURE);
    if (insecureTls) {
      try {
        ManagedChannelBuilder<?> builder = NettyChannelBuilder.forTarget(settings.endpoint)
            .sslContext(GrpcSslContexts.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build());
        builder.userAgent(Version.getSdkVersion());
        if (interceptors != null && interceptors.length > 0) {
          builder = builder.intercept(interceptors);
        }

        if (settings.enableKeepAlive) {
          builder.keepAliveTime(settings.keepAliveTimeSeconds.toSeconds(), TimeUnit.SECONDS)
              .keepAliveTimeout(settings.keepAliveTimeoutSeconds.toSeconds(), TimeUnit.SECONDS)
              .keepAliveWithoutCalls(settings.keepAliveWithoutCalls);
        }
        
        return builder.maxInboundMessageSize(settings.maxInboundMessageSize)
        .maxInboundMetadataSize(settings.maxInboundMetadataSize)
        .build();

      } catch (Exception e) {
        throw new DaprException(
            new DaprError().setErrorCode("TLS_CREDENTIALS_ERROR")
                .setMessage("Failed to create insecure TLS credentials"),
            e);
      }
    }

    String clientKeyPath = settings.tlsPrivateKeyPath;
    String clientCertPath = settings.tlsCertPath;
    String caCertPath = settings.tlsCaPath;

    ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forTarget(settings.endpoint);

    if (clientCertPath != null && clientKeyPath != null) {
      // mTLS case - using client cert and key, with optional CA cert for server
      // authentication
      try (
          InputStream clientCertInputStream = new FileInputStream(clientCertPath);
          InputStream clientKeyInputStream = new FileInputStream(clientKeyPath);
          InputStream caCertInputStream = caCertPath != null ? new FileInputStream(caCertPath) : null) {
        TlsChannelCredentials.Builder builderCreds = TlsChannelCredentials.newBuilder()
            .keyManager(clientCertInputStream, clientKeyInputStream); // For client authentication
        if (caCertInputStream != null) {
          builderCreds.trustManager(caCertInputStream); // For server authentication
        }
        ChannelCredentials credentials = builderCreds.build();
        builder = Grpc.newChannelBuilder(settings.endpoint, credentials);
      } catch (IOException e) {
        throw new DaprException(
            new DaprError().setErrorCode("TLS_CREDENTIALS_ERROR")
                .setMessage("Failed to create mTLS credentials" + (caCertPath != null ? " with CA cert" : "")),
            e);
      }
    } else if (caCertPath != null) {
      // Simple TLS case - using CA cert only for server authentication
      try (InputStream caCertInputStream = new FileInputStream(caCertPath)) {
        ChannelCredentials credentials = TlsChannelCredentials.newBuilder()
            .trustManager(caCertInputStream)
            .build();
        builder = Grpc.newChannelBuilder(settings.endpoint, credentials);
      } catch (IOException e) {
        throw new DaprException(
            new DaprError().setErrorCode("TLS_CREDENTIALS_ERROR")
                .setMessage("Failed to create TLS credentials with CA cert"),
            e);
      }
    } else if (!settings.secure) {
      builder = builder.usePlaintext();
    }

    builder.userAgent(Version.getSdkVersion());

    if (interceptors != null && interceptors.length > 0) {
      builder = builder.intercept(interceptors);
    }

    if (settings.enableKeepAlive) {
      builder.keepAliveTime(settings.keepAliveTimeSeconds.toSeconds(), TimeUnit.SECONDS)
          .keepAliveTimeout(settings.keepAliveTimeoutSeconds.toSeconds(), TimeUnit.SECONDS)
          .keepAliveWithoutCalls(settings.keepAliveWithoutCalls);
    }

    return builder.maxInboundMessageSize(settings.maxInboundMessageSize)
        .maxInboundMetadataSize(settings.maxInboundMetadataSize).build();
  }

  // Not private to allow unit testing
  static final class GrpcEndpointSettings {
    final String endpoint;
    final boolean secure;
    final String tlsPrivateKeyPath;
    final String tlsCertPath;
    final String tlsCaPath;

    final boolean enableKeepAlive;
    final Duration keepAliveTimeSeconds;
    final Duration keepAliveTimeoutSeconds;
    final boolean keepAliveWithoutCalls;

    final int maxInboundMessageSize;
    final int maxInboundMetadataSize;

    private GrpcEndpointSettings(
        String endpoint, boolean secure, String tlsPrivateKeyPath, String tlsCertPath, String tlsCaPath,
        boolean enableKeepAlive, Duration keepAliveTimeSeconds, Duration keepAliveTimeoutSeconds,
        boolean keepAliveWithoutCalls, int maxInboundMessageSize, int maxInboundMetadataSize) {
      this.endpoint = endpoint;
      this.secure = secure;
      this.tlsPrivateKeyPath = tlsPrivateKeyPath;
      this.tlsCertPath = tlsCertPath;
      this.tlsCaPath = tlsCaPath;
      this.enableKeepAlive = enableKeepAlive;
      this.keepAliveTimeSeconds = keepAliveTimeSeconds;
      this.keepAliveTimeoutSeconds = keepAliveTimeoutSeconds;
      this.keepAliveWithoutCalls = keepAliveWithoutCalls;
      this.maxInboundMessageSize = maxInboundMessageSize;
      this.maxInboundMetadataSize = maxInboundMetadataSize;
    }

    static GrpcEndpointSettings parse(Properties properties) {
      String address = properties.getValue(SIDECAR_IP);
      int port = properties.getValue(GRPC_PORT);
      String clientKeyPath = properties.getValue(GRPC_TLS_KEY_PATH);
      String clientCertPath = properties.getValue(GRPC_TLS_CERT_PATH);
      String caCertPath = properties.getValue(GRPC_TLS_CA_PATH);
      boolean enablekeepAlive = properties.getValue(GRPC_ENABLE_KEEP_ALIVE);
      Duration keepAliveTimeSeconds = properties.getValue(GRPC_KEEP_ALIVE_TIME_SECONDS);
      Duration keepAliveTimeoutSeconds = properties.getValue(GRPC_KEEP_ALIVE_TIMEOUT_SECONDS);
      boolean keepAliveWithoutCalls = properties.getValue(GRPC_KEEP_ALIVE_WITHOUT_CALLS);
      int maxInboundMessageSizeBytes = properties.getValue(GRPC_MAX_INBOUND_MESSAGE_SIZE_BYTES);
      int maxInboundMetadataSizeBytes = properties.getValue(GRPC_MAX_INBOUND_METADATA_SIZE_BYTES);

      boolean secure = false;
      String grpcEndpoint = properties.getValue(GRPC_ENDPOINT);
      if ((grpcEndpoint != null) && !grpcEndpoint.isEmpty()) {
        var matcher = GRPC_ENDPOINT_PATTERN.matcher(grpcEndpoint);
        if (!matcher.matches()) {
          throw new IllegalArgumentException("Illegal gRPC endpoint: " + grpcEndpoint);
        }
        var parsedHost = matcher.group("hostname");
        if (parsedHost != null) {
          address = parsedHost;
        }

        var https = matcher.group("https") != null;
        var http = matcher.group("http") != null;
        secure = https;

        String parsedPort = matcher.group("port");
        if (parsedPort != null) {
          port = Integer.parseInt(parsedPort);
        } else {
          // This implements default port as 80 for http for backwards compatibility.
          port = http ? 80 : 443;
        }

        String parsedParam = matcher.group("param");
        if ((http || https) && (parsedParam != null)) {
          throw new IllegalArgumentException("Query params is not supported in HTTP URI for gRPC endpoint.");
        }

        if (parsedParam != null) {
          secure = parsedParam.equalsIgnoreCase("tls=true");
        }

        var authorityEndpoint = matcher.group("authorityEndpoint");
        if (authorityEndpoint != null) {
          return new GrpcEndpointSettings(
              String.format(
                  "dns://%s/%s:%d",
                  authorityEndpoint,
                  address,
                  port),
              secure, clientKeyPath, clientCertPath, caCertPath, enablekeepAlive, keepAliveTimeSeconds,
              keepAliveTimeoutSeconds, keepAliveWithoutCalls, maxInboundMessageSizeBytes, maxInboundMetadataSizeBytes);
        }

        var socket = matcher.group("socket");
        if (socket != null) {
          return new GrpcEndpointSettings(socket, secure, clientKeyPath, clientCertPath, caCertPath, enablekeepAlive,
              keepAliveTimeSeconds, keepAliveTimeoutSeconds, keepAliveWithoutCalls,
              maxInboundMessageSizeBytes, maxInboundMetadataSizeBytes);
        }

        var vsocket = matcher.group("vsocket");
        if (vsocket != null) {
          return new GrpcEndpointSettings(vsocket, secure, clientKeyPath, clientCertPath, caCertPath, enablekeepAlive,
              keepAliveTimeSeconds, keepAliveTimeoutSeconds, keepAliveWithoutCalls, 
              maxInboundMessageSizeBytes, maxInboundMetadataSizeBytes);
        }
      }

      return new GrpcEndpointSettings(String.format(
          "dns:///%s:%d",
          address,
          port), secure, clientKeyPath, clientCertPath, caCertPath, enablekeepAlive, keepAliveTimeSeconds,
          keepAliveTimeoutSeconds, keepAliveWithoutCalls,
          maxInboundMessageSizeBytes, maxInboundMetadataSizeBytes);
    }

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
          throw (RuntimeException) exception;
        }

        throw new RuntimeException(exception);
      }

      long remaining = retryTimeoutMilliseconds - elapsed;
      Thread.sleep(Math.min(remaining, RETRY_WAIT_MILLISECONDS));
    }
  }

  /**
   * Retrieve loopback address for the host.
   *
   * @return The loopback address String
   */
  public static String getHostLoopbackAddress() {
    return InetAddress.getLoopbackAddress().getHostAddress();
  }

  static boolean isIPv6(String ip) {
    return IPV6_PATTERN.matcher(ip).matches();
  }
}
