/*
 * Copyright 2026 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dapr.durabletask;

import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the worker's stateful-history cache bounds. These mirror the Go reference
 * (durabletask-go client/worker_history_test.go) and the Python/.NET SDKs: a sliding TTL, an
 * instance-count cap, and a byte budget, all with least-recently-used eviction.
 */
class WorkflowHistoryCacheTest {

  /** Events with non-zero serialized size (eventId 0 is the proto default, which is 0 bytes). */
  private static List<HistoryEvents.HistoryEvent> events(int count) {
    List<HistoryEvents.HistoryEvent> list = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      list.add(HistoryEvents.HistoryEvent.newBuilder().setEventId(i + 1).build());
    }
    return list;
  }

  private static long bytesOf(int count) {
    long total = 0;
    for (HistoryEvents.HistoryEvent event : events(count)) {
      total += event.getSerializedSize();
    }
    return total;
  }

  @Test
  void getPutRemoveReset() {
    WorkflowHistoryCache cache = new WorkflowHistoryCache(null, 0, 0);

    assertNull(cache.get("a"));

    cache.put("a", events(3));
    assertNotNull(cache.get("a"));
    assertEquals(3, cache.get("a").size());

    cache.remove("a");
    assertNull(cache.get("a"));

    cache.put("b", events(1));
    cache.reset();
    assertNull(cache.get("b"));
  }

  @Test
  void countCapEvictsLeastRecentlyUsed() {
    AtomicLong clock = new AtomicLong(0);
    WorkflowHistoryCache cache = new WorkflowHistoryCache(null, 2, 0, clock::get);

    cache.put("a", events(1));
    clock.incrementAndGet();
    cache.put("b", events(1));
    clock.incrementAndGet();
    cache.put("c", events(1)); // over the cap, evicts the LRU entry ("a")

    assertNull(cache.get("a"));
    assertNotNull(cache.get("b"));
    assertNotNull(cache.get("c"));
  }

  @Test
  void byteCapEvictsLeastRecentlyUsed() {
    long entryBytes = bytesOf(4);
    assertTrue(entryBytes > 0);
    AtomicLong clock = new AtomicLong(0);
    WorkflowHistoryCache cache = new WorkflowHistoryCache(null, 0, entryBytes + 1, clock::get);

    cache.put("a", events(4));
    clock.incrementAndGet();
    cache.put("b", events(4)); // two entries exceed the byte budget, evicts the LRU entry ("a")

    assertNull(cache.get("a"));
    assertNotNull(cache.get("b"));
    assertTrue(cache.totalBytes() <= entryBytes + 1);
  }

  @Test
  void singleOversizedEntryIsKept() {
    WorkflowHistoryCache cache = new WorkflowHistoryCache(null, 0, 1);
    cache.put("big", events(5));
    assertNotNull(cache.get("big"));
  }

  @Test
  void byteAccountingTracksReplaceAndRemove() {
    WorkflowHistoryCache cache = new WorkflowHistoryCache(null, 0, 0);

    cache.put("a", events(3));
    cache.put("b", events(2));
    assertEquals(bytesOf(3) + bytesOf(2), cache.totalBytes());

    cache.put("a", events(6)); // replace adjusts the running total to the new size
    assertEquals(bytesOf(6) + bytesOf(2), cache.totalBytes());

    cache.remove("a");
    assertEquals(bytesOf(2), cache.totalBytes());

    cache.reset();
    assertEquals(0, cache.totalBytes());
  }

  @Test
  void ttlSweepIsSliding() {
    AtomicLong clock = new AtomicLong(0);
    WorkflowHistoryCache cache = new WorkflowHistoryCache(Duration.ofSeconds(60), 0, 0, clock::get);

    cache.put("idle", events(2));
    cache.put("active", events(2));

    clock.set(Duration.ofSeconds(120).toNanos()); // past the TTL...
    assertNotNull(cache.get("active")); // ...but a turn refreshes "active"

    cache.sweepExpired();
    assertNull(cache.get("idle"));
    assertNotNull(cache.get("active"));
  }

  @Test
  void nonPositiveConfigUsesDefaults() {
    // ttl/maxInstances fall back to their (large) defaults; maxBytes becomes unlimited. None of
    // these should evict the three modest entries below.
    WorkflowHistoryCache cache = new WorkflowHistoryCache(Duration.ZERO, -1, -5);

    cache.put("a", events(1));
    cache.put("b", events(1));
    cache.put("c", events(1));

    assertEquals(3, cache.size());
  }
}
