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

package io.dapr.actors.runtime;

import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class used to test different serializer implementations.
 */
public class JavaSerializer implements DaprObjectSerializer {

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] serialize(Object o) throws IOException {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
        oos.writeObject(o);
        oos.flush();
        return bos.toByteArray();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T deserialize(byte[] data, TypeRef<T> type) throws IOException {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
      try (ObjectInputStream ois = new ObjectInputStream(bis)) {
        try {
          return (T) ois.readObject();
        } catch (Exception e) {
          throw new IOException("Could not deserialize Java object.", e);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getContentType() {
    return "application/json";
  }
}
