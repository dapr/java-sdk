/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.utils;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtobufUtils {
  private static final Logger log = LoggerFactory.getLogger(ProtobufUtils.class);

  /**
   * Converts a JSON string to a protobuf Struct.
   *
   * @param json JSON string.
   * @return Protobuf Struct.
   */
  public static Struct jsonToStruct(String json) {
    Struct.Builder builder = Struct.newBuilder();
    try {
      com.google.protobuf.util.JsonFormat.parser()
          .ignoringUnknownFields() // optional
          .merge(json, builder);
    } catch (InvalidProtocolBufferException e) {
      log.error("Failed to parse json to protobuf struct", e);
      return builder.build();
    }
    return builder.build();
  }
}
