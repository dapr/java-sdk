/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.DaprGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * A builder for the DaprClient,
 * Only 2 type of clients are supported at the moment, HTTP and GRPC.
 *
 */
public class DaprClientBuilder {

  /**
   * The type of client supported.
   */
  public static enum DaprClientTypeEnum {
    GRPC,
    HTTP;
  }

  /**
   * An indicator of the client to be build by the instance of the builder.
   */
  private DaprClientTypeEnum clientType;
  /**
   * The host to be used by the client to communicate.
   */
  private String host;
  /**
   * The port to be used by the client to communicate
   */
  private Integer port;

  /**
   * Creates an instance of the builder setting the type of client to be creted
   * @param clientType
   */
  public DaprClientBuilder(DaprClientTypeEnum clientType) {
    this.clientType = clientType;
  }

  /**
   * Sets the host to be used by the client
   * @param host
   * @return itself
   */
  public DaprClientBuilder host(String host) {
    this.host = host;
    return this;
  }

  /**
   * Sets the port to be used by the client
   * @param port
   * @return itself
   */
  public DaprClientBuilder port(Integer port) {
    this.port = port;
    return this;
  }

  /**
   * Build an instance of the Client based on the provided setup.
   * @return an instance of the setup Client
   * @throws java.lang.IllegalStateException if any required field is missing
   */
  public DaprClient build() {
    if (DaprClientTypeEnum.GRPC.equals(this.clientType)) {
      return buildDaprClientGrpc();
    } else if (DaprClientTypeEnum.HTTP.equals(this.clientType)) {
      return buildDaprClientHttp();
    }
    throw new IllegalStateException("Unsupported client type.");
  }

  /**
   * Creates an instance of the GPRC Client.
   * @return the GRPC Client.
   * @throws java.lang.IllegalStateException if either host is missing or if port is missing or a negative number.
   */
  private DaprClient buildDaprClientGrpc() {
    if (null == this.host || "".equals(this.host.trim())) {
      throw new IllegalStateException("Host must is required.");
    }
    if (null == port || port <= 0) {
      throw new IllegalStateException("Invalid port.");
    }
    ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    return new DaprClientGrpcAdapter(DaprGrpc.newFutureStub(channel));
  }

  /**
   * Creates and instance of the HTTP CLient.
   * @return
   */
  private DaprClient buildDaprClientHttp() {
    throw new UnsupportedOperationException("Not implemented yet.");
  }
}
