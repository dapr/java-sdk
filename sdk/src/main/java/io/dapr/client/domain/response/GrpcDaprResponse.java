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

import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;

import java.io.IOException;
import java.util.Map;

/**
 * GrpcDaprResponse.
 */
public class GrpcDaprResponse<T> implements DaprResponse<T> {

  private final DaprObjectSerializer serializer;

  private final TypeRef<T> type;

  private final byte[] data;

  private final Map<String,String> headers;

  /**
   * build grpc dapr response.
   * @param data grpc invoke response data
   * @param headers grpc invoke headers
   * @param serializer objectSerializer
   * @param type type
   */
  public GrpcDaprResponse(byte[] data, Map<String,String> headers, DaprObjectSerializer serializer, TypeRef<T> type) {
    this.data = data;
    this.headers = headers;
    this.serializer = serializer;
    this.type = type;
  }

  @Override
  public int getCode() {
    // InvokeResponse didn't have it.
    return 200;
  }

  @Override
  public T getData() throws IOException {
    return serializer.deserialize(data, type);
  }

  @Override
  public Map<String, String> getHeaders() {
    return this.headers;
  }
}
