/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.config;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Global properties for Dapr's SDK, using Supplier so they are dynamically resolved.
 */
public class Properties {

  /**
   * Dapr's default IP for HTTP and GRPC communication.
   */
  private static final String DEFAULT_SIDECAR_IP = "127.0.0.1";

  /**
   * Dapr's default HTTP port.
   */
  private static final Integer DEFAULT_HTTP_PORT = 3500;

  /**
   * Dapr's default GRPC port.
   */
  private static final Integer DEFAULT_GRPC_PORT = 50001;

  /**
   * Dapr's default GRPC port.
   */
  private static final Boolean DEFAULT_GRPC_ENABLED = true;

  /**
   * Dapr's default String encoding: UTF-8.
   */
  private static final Charset DEFAULT_STRING_CHARSET = StandardCharsets.UTF_8;

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
   * Determines if Dapr client will use GRPC to talk to Dapr's side car.
   */
  public static final Property<Boolean> USE_GRPC = new BooleanProperty(
      "dapr.grpc.enabled",
      "DAPR_GRPC_ENABLED",
      DEFAULT_GRPC_ENABLED);

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
}
