/*
 * Copyright 2024 The Dapr Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.protobuf.Descriptors;

public class DetailObjectMapper {
  public static final ObjectMapper OBJECT_MAPPER;

  static {
    SimpleModule module = new SimpleModule();
    module.addKeyDeserializer(Descriptors.FieldDescriptor.class, new DetailDeserializer());
    OBJECT_MAPPER = new ObjectMapper();
    OBJECT_MAPPER.registerModule(module);
  }
}
