package io.dapr.client.domain;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DaprBulkMessageEntryTest {
  @Test
  public void testSetMetadata() {
    // Arrange
    DaprBulkMessageEntry<String> entry = new DaprBulkMessageEntry<>();
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
    DaprBulkMessageEntry<String> entry = new DaprBulkMessageEntry<>();
    String contentType = "application/json";

    // Act
    entry.setContentType(contentType);

    // Assert
    assertEquals(contentType, entry.getContentType());
  }

  @Test
  public void testSetEvent() {
    // Arrange
    DaprBulkMessageEntry<String> entry = new DaprBulkMessageEntry<>();
    String event = "This is an event!";

    // Act
    entry.setEvent(event);

    // Assert
    assertEquals(event, entry.getEvent());
  }
}
