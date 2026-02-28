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

package io.dapr.client;

/**
 * Common headers for GRPC and HTTP communication.
 */
public final class Headers {

  /**
   * OpenCensus's metadata for GRPC.
   */
  public static final String GRPC_TRACE_BIN = "grpc-trace-bin";

  /**
   * Token for authentication from Application to Dapr runtime.
   */
  public static final String DAPR_API_TOKEN = "dapr-api-token";

  /**
   * Header for Api Logging User-Agent.
   */
  public static final String DAPR_USER_AGENT = "User-Agent";

  /**
   * W3C Baggage header for context propagation.
   */
  public static final String BAGGAGE = "baggage";
}
