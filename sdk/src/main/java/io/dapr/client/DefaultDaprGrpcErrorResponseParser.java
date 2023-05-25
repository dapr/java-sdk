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


import static io.dapr.config.Properties.STRING_CHARSET;

/**
 * Default error parser for Dapr Grpc API.
 */
public class DefaultDaprGrpcErrorResponseParser implements DaprErrorResponseParser {

  /**
   * {@inheritDoc}
   */
  @Override
  public DaprException parse(int statusCode, byte[] errorDetails) {
    String errorMessage = new String(errorDetails, STRING_CHARSET.get());
    return new DaprException("UNKNOWN: ", errorMessage);
  }
}
