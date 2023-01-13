package io.dapr.client.domain;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class BulkAppResponseEntryTest {
  @Test
  public void testSetStatus() {
    for (BulkAppResponseStatus status: Arrays.asList(
            BulkAppResponseStatus.RETRY,
            BulkAppResponseStatus.DROP,
            BulkAppResponseStatus.SUCCESS)) {
      BulkAppResponseEntry entry = new BulkAppResponseEntry();
      entry.setStatus(status);
      assertEquals(status, entry.getStatus());
    }
  }

  @Test
  public void testSetEntryId() {
    // Arrange
    BulkAppResponseEntry entry = new BulkAppResponseEntry();
    String entryId = "1";

    // Act
    entry.setEntryID(entryId);

    // Assert
    assertEquals(entryId, entry.getEntryID());
  }

  @Test
  public void testCtr() {
    // Arrange/Act
    BulkAppResponseEntry entry = new BulkAppResponseEntry("1", BulkAppResponseStatus.SUCCESS);

    // Assert
    assertEquals("1", entry.getEntryID());
    assertEquals(BulkAppResponseStatus.SUCCESS, entry.getStatus());
  }
}
