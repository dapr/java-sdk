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

package io.dapr.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Objects;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class CustomObjectSerializerTest {

  private static final DaprObjectSerializer SERIALIZER =
      new CustomizableObjectSerializer(new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

  public static class ObjectForTesting implements Serializable {
    private ZonedDateTime time;

    public ZonedDateTime getTime() {
      return time;
    }

    public void setTime(ZonedDateTime time) {
      this.time = time;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ObjectForTesting that = (ObjectForTesting) o;
      return Objects.equal(time, that.time);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(time);
    }
  }

  @Test
  public void serializeObjectForTesting() {
    ObjectForTesting obj = new ObjectForTesting();
    obj.setTime(ZonedDateTime.of(1900, 1, 1, 1, 1, 0, 0, ZoneId.of("UTC")));
    String expectedResult =
        "{\"time\":\"1900-01-01T01:01:00Z\"}";

    String serializedValue;
    try {
      serializedValue = new String(SERIALIZER.serialize(obj));
      assertEquals(expectedResult, serializedValue,
          "FOUND:[[" + serializedValue + "]] \n but was EXPECTING: [[" + expectedResult + "]]");
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }
}
