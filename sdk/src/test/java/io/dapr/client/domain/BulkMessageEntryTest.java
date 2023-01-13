/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.client.domain;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class BulkMessageEntryTest {
  @Test
  public void testSetMetadata() {
    // Arrange
    BulkMessageEntry<String> entry = new BulkMessageEntry<>();
    Map<String, String> metadata = new HashMap<>();
    metadata.put("FOO", "BAR");

    // Act
    entry.setMetadata(metadata);

    // Assert
    Map<String, String> m = entry.getMetadata();
    assertEquals(metadata, m);
  }

  @Test
  public void testSetContentType() {
    // Arrange
    BulkMessageEntry<String> entry = new BulkMessageEntry<>();
    String contentType = "application/json";

    // Act
    entry.setContentType(contentType);

    // Assert
    assertEquals(contentType, entry.getContentType());
  }

  @Test
  public void testSetEvent() {
    // Arrange
    BulkMessageEntry<String> entry = new BulkMessageEntry<>();
    String event = "This is an event!";

    // Act
    entry.setEvent(event);

    // Assert
    assertEquals(event, entry.getEvent());
  }
}
