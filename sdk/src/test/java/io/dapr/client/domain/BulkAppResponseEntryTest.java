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
