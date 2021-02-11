/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
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
  private static final Integer DEFAULT_HTTP_CLIENT_READTIMEOUTSECONDS = 60;

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
      DEFAULT_HTTP_CLIENT_READTIMEOUTSECONDS);
}
