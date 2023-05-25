/*
 * Copyright 2023 The Dapr Authors
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

import io.dapr.exceptions.DaprException;

import java.io.IOException;

/**
 * Parses an error response from Dapr APIs.
 */
public interface DaprErrorResponseParser {

  /**
   * Parses an error code and throws an Exception.
   *
   * @param statusCode HTTP or gRPC status code.
   * @param response   response payload from Dapr API.
   * @return Exception parsed from payload
   * @throws IOException if cannot parse error.
   */
  DaprException parse(int statusCode, byte[] response) throws IOException;
}
