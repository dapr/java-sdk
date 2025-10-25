/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.durabletask;

import io.grpc.Channel;

/**
 * Builder class for constructing new {@link DurableTaskClient} objects that communicate with a sidecar process
 * over gRPC.
 */
public final class DurableTaskGrpcClientBuilder {
  DataConverter dataConverter;
  int port;
  Channel channel;
  String tlsCaPath;
  String tlsCertPath;
  String tlsKeyPath;
  boolean insecure;

  /**
   * Sets the {@link DataConverter} to use for converting serializable data payloads.
   *
   * @param dataConverter the {@link DataConverter} to use for converting serializable data payloads
   * @return this builder object
   */
  public DurableTaskGrpcClientBuilder dataConverter(DataConverter dataConverter) {
    this.dataConverter = dataConverter;
    return this;
  }

  /**
   * Sets the gRPC channel to use for communicating with the sidecar process.
   *
   * <p>This builder method allows you to provide your own gRPC channel for communicating with the Durable Task sidecar
   * endpoint. Channels provided using this method won't be closed when the client is closed.
   * Rather, the caller remains responsible for shutting down the channel after disposing the client.</p>
   *
   * <p>If not specified, a gRPC channel will be created automatically for each constructed
   * {@link DurableTaskClient}.</p>
   *
   * @param channel the gRPC channel to use
   * @return this builder object
   */
  public DurableTaskGrpcClientBuilder grpcChannel(Channel channel) {
    this.channel = channel;
    return this;
  }

  /**
   * Sets the gRPC endpoint port to connect to. If not specified, the default Durable Task port number will be used.
   *
   * @param port the gRPC endpoint port to connect to
   * @return this builder object
   */
  public DurableTaskGrpcClientBuilder port(int port) {
    this.port = port;
    return this;
  }

  /**
   * Sets the path to the TLS CA certificate file for server authentication.
   * If not set, the system's default CA certificates will be used.
   *
   * @param tlsCaPath path to the TLS CA certificate file
   * @return this builder object
   */
  public DurableTaskGrpcClientBuilder tlsCaPath(String tlsCaPath) {
    this.tlsCaPath = tlsCaPath;
    return this;
  }

  /**
   * Sets the path to the TLS client certificate file for client authentication.
   * This is used for mTLS (mutual TLS) connections.
   *
   * @param tlsCertPath path to the TLS client certificate file
   * @return this builder object
   */
  public DurableTaskGrpcClientBuilder tlsCertPath(String tlsCertPath) {
    this.tlsCertPath = tlsCertPath;
    return this;
  }

  /**
   * Sets the path to the TLS client key file for client authentication.
   * This is used for mTLS (mutual TLS) connections.
   *
   * @param tlsKeyPath path to the TLS client key file
   * @return this builder object
   */
  public DurableTaskGrpcClientBuilder tlsKeyPath(String tlsKeyPath) {
    this.tlsKeyPath = tlsKeyPath;
    return this;
  }

  /**
   * Sets whether to use insecure (plaintext) mode for gRPC communication.
   * When set to true, TLS will be disabled and communication will be unencrypted.
   * This should only be used for development/testing.
   *
   * @param insecure whether to use insecure mode
   * @return this builder object
   */
  public DurableTaskGrpcClientBuilder insecure(boolean insecure) {
    this.insecure = insecure;
    return this;
  }

  /**
   * Initializes a new {@link DurableTaskClient} object with the settings specified in the current builder object.
   *
   * @return a new {@link DurableTaskClient} object
   */
  public DurableTaskClient build() {
    return new DurableTaskGrpcClient(this);
  }
}
