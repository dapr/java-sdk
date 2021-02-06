/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;

/**
 * Common headers for GRPC and HTTP communication.
 */
class Headers {

  /**
   * OpenCensus's metadata for GRPC.
   */
  static final String GRPC_TRACE_BIN = "grpc-trace-bin";

  /**
   * Token for authentication from Application to Dapr runtime.
   */
  static final String DAPR_API_TOKEN = "dapr-api-token";
}
