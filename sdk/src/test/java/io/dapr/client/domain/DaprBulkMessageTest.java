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
    DaprBulkMessage<String> message = new DaprBulkMessage<>();
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
    DaprBulkMessage<String> message = new DaprBulkMessage<>();
    String topic = "myTopic";

    // Act
    message.setTopic(topic);

    // Assert
    assertEquals(topic, message.getTopic());
  }

  private DaprBulkMessage<String> getDaprBulkMessage(List<DaprBulkMessageEntry<String>> entries, boolean useCtr) {
    if (useCtr) {
      return new DaprBulkMessage<>(entries, "foo-topic", new HashMap<>());
    } else {
      DaprBulkMessage<String> message = new DaprBulkMessage<>();
      message.setEntries(entries);
      return message;
    }
  }

  @Test
  public void testSetEntries() {
    for (boolean b: new boolean[]{true, false}) {
      // Scenario 1: When entry is null
      DaprBulkMessage<String> messageWithNull = getDaprBulkMessage(null, b);
      assertNull(messageWithNull.getEntries());

      // Scenario 2: When entry has items
      DaprBulkMessageEntry<String> entry1 = new DaprBulkMessageEntry<>(
              "1", "Message 1", "text/plain", new HashMap<>()
      );
      DaprBulkMessageEntry<String> entry2 = new DaprBulkMessageEntry<>(
              "2", "Message 2", "text/plain", new HashMap<>()
      );
      List<DaprBulkMessageEntry<String>> entries = new ArrayList<>(Arrays.asList(entry1, entry2));
      DaprBulkMessage<String> message = getDaprBulkMessage(entries, b);

      List<DaprBulkMessageEntry<String>> entriesFromGet = message.getEntries();
      assertEquals(2, entriesFromGet.size());
      assertEquals("1", entriesFromGet.get(0).getEntryID());
      assertEquals("2", entriesFromGet.get(1).getEntryID());
    }
  }
}
