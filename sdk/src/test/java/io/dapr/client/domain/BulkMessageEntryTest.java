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
