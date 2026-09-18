/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.workflows.task.serialization;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JacksonDataConverterTest {

  private final JacksonDataConverter converter = new JacksonDataConverter();

  public static final class Payload {
    private String name;

    public Payload() {
    }

    public Payload(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  /** Jackson cannot serialize a type with no accessible properties. */
  public static final class NotSerializable {
  }

  @Test
  public void shouldRoundTripAnObject() {
    String json = converter.serialize(new Payload("value"));

    assertEquals("{\"name\":\"value\"}", json);
    assertEquals("value", converter.deserialize(json, Payload.class).getName());
  }

  @Test
  public void shouldSerializeNullAsNullRatherThanTheStringNull() {
    assertNull(converter.serialize(null));
  }

  @Test
  public void shouldDeserializeNothingToNull() {
    assertNull(converter.deserialize(null, String.class));
    assertNull(converter.deserialize("", String.class));
    assertNull(converter.deserialize("\"value\"", Void.class),
        "a Void target means the caller wants no value back");
  }

  @Test
  public void shouldHandleJavaTimeThroughTheDiscoveredModules() {
    Instant instant = Instant.parse("2025-01-01T00:00:00Z");

    String json = converter.serialize(instant);

    assertEquals(instant, converter.deserialize(json, Instant.class));
  }

  @Test
  public void shouldWrapASerializationFailureWithTheOffendingType() {
    DataConverter.DataConverterException thrown = assertThrows(DataConverter.DataConverterException.class,
        () -> converter.serialize(new NotSerializable()));

    assertTrue(thrown.getMessage().contains(NotSerializable.class.getName()), thrown.getMessage());
    assertTrue(thrown.getMessage().startsWith("Failed to serialize"), thrown.getMessage());
  }

  @Test
  public void shouldWrapADeserializationFailureWithTheTargetType() {
    DataConverter.DataConverterException thrown = assertThrows(DataConverter.DataConverterException.class,
        () -> converter.deserialize("this is not json", Payload.class));

    assertTrue(thrown.getMessage().contains(Payload.class.getName()), thrown.getMessage());
    assertTrue(thrown.getMessage().startsWith("Failed to deserialize"), thrown.getMessage());
  }
}
