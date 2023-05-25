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

package io.dapr.exceptions;

import okhttp3.Response;

/**
 * HTTP Exception from Dapr API.
 */
public class DaprHttpException extends RuntimeException {

  /**
   * Creates a new instance of DaprHTTPException.
   *
   * @param response HTTP response from OkHTTP
   */
  public DaprHttpException(Response response) {
    super("Dapr HTTP exception: " + response.code() + " " + response.message());
  }
}
