package io.dapr.client.domain;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class DaprBulkAppResponseEntryTest {
  @Test
  public void testSetStatus() {
    for (DaprBulkAppResponseStatus status: Arrays.asList(
            DaprBulkAppResponseStatus.RETRY,
            DaprBulkAppResponseStatus.DROP,
            DaprBulkAppResponseStatus.SUCCESS)) {
      DaprBulkAppResponseEntry entry = new DaprBulkAppResponseEntry();
      entry.setStatus(status);
      assertEquals(status, entry.getStatus());
    }
  }

  @Test
  public void testSetEntryId() {
    // Arrange
    DaprBulkAppResponseEntry entry = new DaprBulkAppResponseEntry();
    String entryId = "1";

    // Act
    entry.setEntryID(entryId);

    // Assert
    assertEquals(entryId, entry.getEntryID());
  }

  @Test
  public void testCtr() {
    // Arrange/Act
    DaprBulkAppResponseEntry entry = new DaprBulkAppResponseEntry("1", DaprBulkAppResponseStatus.SUCCESS);

    // Assert
    assertEquals("1", entry.getEntryID());
    assertEquals(DaprBulkAppResponseStatus.SUCCESS, entry.getStatus());
  }
}
