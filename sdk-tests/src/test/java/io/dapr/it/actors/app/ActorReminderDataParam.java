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

package io.dapr.it.actors.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.utils.TypeRef;

/**
 * This class is for passing string or binary data to the Actor for registering reminder later on during test.
 */
public class ActorReminderDataParam {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private String data;

  private byte[] binaryData;

  private String typeHint;

  public ActorReminderDataParam() {
  }

  public ActorReminderDataParam(String data, String typeHint) {
    this.data = data;
    this.typeHint = typeHint;
  }

  public ActorReminderDataParam(byte[] data, String typeHint) {
    this.binaryData = data;
    this.typeHint = typeHint;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public byte[] getBinaryData() {
    return binaryData;
  }

  public void setBinaryData(byte[] binaryData) {
    this.binaryData = binaryData;
  }

  public String getTypeHint() {
    return typeHint;
  }

  public void setTypeHint(String typeHint) {
    this.typeHint = typeHint;
  }

  public <T> T asObject(TypeRef<T> type) throws Exception {
    if (this.data != null) {
      return OBJECT_MAPPER.readValue(this.data, OBJECT_MAPPER.constructType(type.getType()));
    } else if (this.binaryData != null) {
      return OBJECT_MAPPER.readValue(this.binaryData, OBJECT_MAPPER.constructType(type.getType()));
    }
    return null;
  }
}
