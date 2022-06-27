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

package io.dapr.client.domain.response;

import java.io.IOException;
import java.util.Map;

/**
 * Response.
 */
public interface DaprResponse<T> {

  /**
   * get response code.
   * @return response code
   */
  int getCode();

  /**
   * get response data.
   * @return response data
   */
  T getData() throws IOException;

  /**
   * get response header.
   * @return response header
   */
  Map<String,String> getHeaders();
}
