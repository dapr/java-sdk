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

import io.dapr.utils.TypeRef;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

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

  /**
   * get sub type from type.
   * @param type response type
   * @param <T> type
   * @return sub type
   */
  static <T> TypeRef getSubType(TypeRef<T> type) {
    TypeRef resultType = type;
    if (type.getType() instanceof ParameterizedType) {
      Type[] actualTypeArguments = ((ParameterizedType) type.getType()).getActualTypeArguments();
      if (Objects.nonNull(actualTypeArguments) && actualTypeArguments.length > 0) {
        resultType = TypeRef.get(actualTypeArguments[0]);
      }
    } else {
      resultType = TypeRef.get(byte[].class);
    }
    return resultType;
  }

  /**
   * get response bytes.
   * @return bytes
   */
  byte[] getSourceBytesData();

  /**
   * get repsonse bytes without `34`.
   * string that serialized by objectmapper will surrounded by `"`.
   * it will be removed when return bytes.
   * @return bytes
   */
  default byte[] getBytes() {
    byte[] data = getSourceBytesData();
    if (data.length > 1 && data[0] == 34) {
      data = Arrays.copyOfRange(data, 1, data.length - 1);
    }
    return data;
  }
}
