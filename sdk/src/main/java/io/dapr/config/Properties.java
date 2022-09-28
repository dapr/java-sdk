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

package io.dapr.config;

import io.dapr.client.DaprApiProtocol;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Global properties for Dapr's SDK, using Supplier so they are dynamically resolved.
 */
public class Properties {

  /**
   * Dapr's default IP for HTTP and gRPC communication.
   */
  private static final String DEFAULT_SIDECAR_IP = "127.0.0.1";

  /**
   * Dapr's default HTTP port.
   */
  private static final Integer DEFAULT_HTTP_PORT = 3500;

  /**
   * Dapr's default gRPC port.
   */
  private static final Integer DEFAULT_GRPC_PORT = 50001;

  /**
   * Dapr's default use of gRPC or HTTP.
   */
  private static final DaprApiProtocol DEFAULT_API_PROTOCOL = DaprApiProtocol.GRPC;

  /**
   * Dapr's default use of gRPC or HTTP for Dapr's method invocation APIs.
   */
  private static final DaprApiProtocol DEFAULT_API_METHOD_INVOCATION_PROTOCOL = DaprApiProtocol.HTTP;

  /**
   * Dapr's default String encoding: UTF-8.
   */
  private static final Charset DEFAULT_STRING_CHARSET = StandardCharsets.UTF_8;

  /**
   * Dapr's default timeout in seconds for HTTP client reads.
   */
  private static final Integer DEFAULT_HTTP_CLIENT_READ_TIMEOUT_SECONDS = 60;

  /**
   *   Dapr's default maximum number of requests for HTTP client to execute concurrently.
   *
   *   <p>Above this requests queue in memory, waiting for the running calls to complete.
   *   Default is 64 in okhttp which is OK for most case, but for some special case
   *   which is slow response and high concurrency, the value should set to a little big.
   */
  private static final Integer DEFAULT_HTTP_CLIENT_MAX_REQUESTS = 1024;

  /**
   *   Dapr's default maximum number of idle connections of HTTP connection pool.
   *
   *   <p>Attention! This is max IDLE connection, NOT max connection!
   *   It is also very important for high concurrency cases.
   */
  private static final Integer DEFAULT_HTTP_CLIENT_MAX_IDLE_CONNECTIONS = 128;

  /**
   * IP for Dapr's sidecar.
   */
  public static final Property<String> SIDECAR_IP = new StringProperty(
      "dapr.sidecar.ip",
      "DAPR_SIDECAR_IP",
      DEFAULT_SIDECAR_IP);

  /**
   * HTTP port for Dapr after checking system property and environment variable.
   */
  public static final Property<Integer> HTTP_PORT = new IntegerProperty(
      "dapr.http.port",
      "DAPR_HTTP_PORT",
      DEFAULT_HTTP_PORT);

  /**
   * GRPC port for Dapr after checking system property and environment variable.
   */
  public static final Property<Integer> GRPC_PORT = new IntegerProperty(
      "dapr.grpc.port",
      "DAPR_GRPC_PORT",
      DEFAULT_GRPC_PORT);

  /**
   * Determines if Dapr client will use gRPC or HTTP to talk to Dapr's side car.
   */
  public static final Property<DaprApiProtocol> API_PROTOCOL = new GenericProperty<>(
      "dapr.api.protocol",
      "DAPR_API_PROTOCOL",
      DEFAULT_API_PROTOCOL,
      (s) -> DaprApiProtocol.valueOf(s.toUpperCase()));

  /**
   * Determines if Dapr client should use gRPC or HTTP for Dapr's service method invocation APIs.
   */
  public static final Property<DaprApiProtocol> API_METHOD_INVOCATION_PROTOCOL = new GenericProperty<>(
      "dapr.api.methodInvocation.protocol",
      "DAPR_API_METHOD_INVOCATION_PROTOCOL",
      DEFAULT_API_METHOD_INVOCATION_PROTOCOL,
      (s) -> DaprApiProtocol.valueOf(s.toUpperCase()));

  /**
   * API token for authentication between App and Dapr's side car.
   */
  public static final Property<String> API_TOKEN = new StringProperty(
      "dapr.api.token",
      "DAPR_API_TOKEN",
      null);

  /**
   * Determines which string encoding is used in Dapr's Java SDK.
   */
  public static final Property<Charset> STRING_CHARSET = new GenericProperty<>(
      "dapr.string.charset",
      "DAPR_STRING_CHARSET",
      DEFAULT_STRING_CHARSET,
      (s) -> Charset.forName(s));

  /**
   * Dapr's timeout in seconds for HTTP client reads.
   */
  public static final Property<Integer> HTTP_CLIENT_READ_TIMEOUT_SECONDS = new IntegerProperty(
      "dapr.http.client.readTimeoutSeconds",
      "DAPR_HTTP_CLIENT_READ_TIMEOUT_SECONDS",
          DEFAULT_HTTP_CLIENT_READ_TIMEOUT_SECONDS);

  /**
   * Dapr's default maximum number of requests for HTTP client to execute concurrently.
   */
  public static final Property<Integer> HTTP_CLIENT_MAX_REQUESTS = new IntegerProperty(
          "dapr.http.client.maxRequests",
          "DAPR_HTTP_CLIENT_MAX_REQUESTS",
          DEFAULT_HTTP_CLIENT_MAX_REQUESTS);

  /**
   * Dapr's default maximum number of idle connections for HTTP connection pool.
   */
  public static final Property<Integer> HTTP_CLIENT_MAX_IDLE_CONNECTIONS = new IntegerProperty(
          "dapr.http.client.maxIdleConnections",
          "DAPR_HTTP_CLIENT_MAX_IDLE_CONNECTIONS",
          DEFAULT_HTTP_CLIENT_MAX_IDLE_CONNECTIONS);
}
