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

import io.dapr.client.DaprHttp;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;

import java.io.IOException;
import java.util.Map;

/**
 * HttpDaprResponse.
 */
public class HttpDaprResponse<T> implements DaprResponse<T> {

  private final DaprHttp.Response response;

  private final DaprObjectSerializer serializer;

  private final TypeRef<T> type;

  /**
   * build http dapr response.
   * @param response http invoke response
   * @param serializer serializer
   * @param type type
   */
  public HttpDaprResponse(DaprHttp.Response response, DaprObjectSerializer serializer, TypeRef<T> type) {
    this.response = response;
    this.serializer = serializer;
    this.type = type;
  }

  @Override
  public int getCode() {
    return response.getStatusCode();
  }

  @Override
  public T getData() throws IOException {
    byte[] data = response.getBody();
    if (type.getType() == String.class) {
      return (T) new String(data);
    }
    if (type.getType() == byte[].class) {
      return (T) getBytes();
    }
    return serializer.deserialize(data, type);
  }

  @Override
  public Map<String, String> getHeaders() {
    return response.getHeaders();
  }

  @Override
  public byte[] getSourceBytesData() {
    return response.getBody();
  }
}
