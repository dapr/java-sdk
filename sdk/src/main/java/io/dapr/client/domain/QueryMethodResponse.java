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

package io.dapr.client.domain;

import java.util.Map;

/**
 * HttpDaprResponse.
 */
public class QueryMethodResponse<T> {

  private final int code;

  private final Map<String, String> headers;

  private final T result;

  /**
   * build query method response.
   * @param code http response code.
   * @param headers http headers.
   * @param result the type of the return.
   */
  public QueryMethodResponse(int code, Map<String, String> headers, T result) {
    this.code = code;
    this.headers = headers;
    this.result = result;
  }


  /**
   * get response code.
   * @return response code
   */
  public int getCode() {
    return code;
  }

  /**
   * get response result.
   * @return response result
   */
  public T getResult() {
    return result;
  }

  /**
   * get response header.
   * @return response header
   */
  public Map<String, String> getHeaders() {
    return headers;
  }

}
