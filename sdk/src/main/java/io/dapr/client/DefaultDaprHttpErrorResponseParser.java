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

import io.dapr.exceptions.DaprError;
import io.dapr.exceptions.DaprException;

import java.io.IOException;

import static io.dapr.client.ObjectSerializer.OBJECT_MAPPER;
import static io.dapr.config.Properties.STRING_CHARSET;

/**
 * Default implementation for DaprHTTPErrorResponse.
 */
public class DefaultDaprHttpErrorResponseParser implements DaprErrorResponseParser {

  /**
   * {@inheritDoc}
   */
  @Override
  public DaprException parse(int statusCode, byte[] response) {
    String errorMessage =
        (response == null || (response.length == 0))
            ? "HTTP status code: " + statusCode
            : new String(response, STRING_CHARSET.get());
    DaprError error = null;
    String errorCode = "UNKNOWN";
    DaprException unknownException = new DaprException(errorCode, errorMessage);

    if ((response != null) && (response.length != 0)) {
      try {
        error = OBJECT_MAPPER.readValue(response, DaprError.class);
      } catch (IOException e) {
        return new DaprException("UNKNOWN", new String(response, STRING_CHARSET.get()));
      }
    }

    if (error != null) {
      errorMessage = error.getMessage() == null ? errorMessage : error.getMessage();
      errorCode = error.getErrorCode() == null ? errorCode : error.getErrorCode();
      return new DaprException(errorCode, errorMessage);
    }
    return unknownException;
  }
}
