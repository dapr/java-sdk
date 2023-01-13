package io.dapr.client.domain;

import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DaprBulkMessageTest {
  @Test
  public void testSetMetadata() {
    // Arrange
    BulkMessage<String> message = new BulkMessage<>();
    Map<String, String> metadata = new HashMap<>();
    metadata.put("FOO", "BAR");

    // Act
    message.setMetadata(metadata);

    // Assert
    Map<String, String> m = message.getMetadata();
    assertEquals(m, metadata);
  }

  @Test
  public void testSetTopic() {
    // Arrange
    BulkMessage<String> message = new BulkMessage<>();
    String topic = "myTopic";

    // Act
    message.setTopic(topic);

    // Assert
    assertEquals(topic, message.getTopic());
  }

  private BulkMessage<String> getBulkMessage(List<BulkMessageEntry<String>> entries, boolean useCtr) {
    if (useCtr) {
      return new BulkMessage<>(entries, "foo-topic", new HashMap<>());
    } else {
      BulkMessage<String> message = new BulkMessage<>();
      message.setEntries(entries);
      return message;
    }
  }

  @Test
  public void testSetEntries() {
    for (boolean b: new boolean[]{true, false}) {
      // Scenario 1: When entry is null
      BulkMessage<String> messageWithNull = getBulkMessage(null, b);
      assertNull(messageWithNull.getEntries());

      // Scenario 2: When entry has items
      BulkMessageEntry<String> entry1 = new BulkMessageEntry<>(
              "1", "Message 1", "text/plain", new HashMap<>()
      );
      BulkMessageEntry<String> entry2 = new BulkMessageEntry<>(
              "2", "Message 2", "text/plain", new HashMap<>()
      );
      List<BulkMessageEntry<String>> entries = new ArrayList<>(Arrays.asList(entry1, entry2));
      BulkMessage<String> message = getBulkMessage(entries, b);

      List<BulkMessageEntry<String>> entriesFromGet = message.getEntries();
      assertEquals(2, entriesFromGet.size());
      assertEquals("1", entriesFromGet.get(0).getEntryID());
      assertEquals("2", entriesFromGet.get(1).getEntryID());
    }
  }
}
