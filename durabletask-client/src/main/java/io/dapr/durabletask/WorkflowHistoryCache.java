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
limitations under the License.
*/

package io.dapr.durabletask;

import io.dapr.durabletask.implementation.protobuf.HistoryEvents;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Per-stream cache of each workflow instance's committed history, enabling "stateful history"
 * delta work items: a worker that advertises {@code WORKER_CAPABILITY_STATEFUL_HISTORY} retains
 * the history it has already replayed so the sidecar can send only the new events (the delta).
 * Entries are reclaimed by a sliding TTL, an instance-count cap, and an optional byte budget
 * (LRU eviction). Eviction is always safe: a miss is recovered via the GetInstanceHistory RPC.
 *
 * <p>Thread-safe; work items are processed concurrently on the worker pool.</p>
 */
public final class WorkflowHistoryCache {

  static final Duration DEFAULT_TTL = Duration.ofHours(1);
  static final int DEFAULT_MAX_INSTANCES = 100_000;

  private static final class Entry {
    final List<HistoryEvents.HistoryEvent> events;
    final long bytes;
    long lastAccess;

    Entry(List<HistoryEvents.HistoryEvent> events, long bytes, long lastAccess) {
      this.events = events;
      this.bytes = bytes;
      this.lastAccess = lastAccess;
    }
  }

  private final Object lock = new Object();
  private final Map<String, Entry> entries = new HashMap<>();
  private final long ttlNanos;
  private final int maxInstances;
  private final long maxBytes;
  private final LongSupplier clockNanos;
  private long totalBytes;

  /**
   * Constructs a cache with the default monotonic clock. Non-positive ttl/maxInstances use the
   * package defaults; a non-positive maxBytes means unlimited (bounded by ttl and maxInstances).
   *
   * @param ttl          sliding time-to-live for an idle instance's entry
   * @param maxInstances instance-count cap
   * @param maxBytes     byte budget, or {@code <= 0} for unlimited
   */
  public WorkflowHistoryCache(Duration ttl, int maxInstances, long maxBytes) {
    this(ttl, maxInstances, maxBytes, System::nanoTime);
  }

  /**
   * Constructs a cache with an injectable clock, for deterministic tests.
   *
   * @param clockNanos supplier of a monotonic nanosecond timestamp (e.g. {@code System::nanoTime})
   */
  WorkflowHistoryCache(Duration ttl, int maxInstances, long maxBytes, LongSupplier clockNanos) {
    Duration effectiveTtl = ttl != null && !ttl.isZero() && !ttl.isNegative() ? ttl : DEFAULT_TTL;
    this.ttlNanos = effectiveTtl.toNanos();
    this.maxInstances = maxInstances > 0 ? maxInstances : DEFAULT_MAX_INSTANCES;
    this.maxBytes = maxBytes > 0 ? maxBytes : 0;
    this.clockNanos = clockNanos;
  }

  /**
   * Returns the cached committed history for an instance, refreshing its TTL, or {@code null} on a
   * miss.
   *
   * @param instanceId the workflow instance ID
   * @return the cached committed history, or {@code null} if the instance is not cached
   */
  public List<HistoryEvents.HistoryEvent> get(String instanceId) {
    synchronized (this.lock) {
      Entry entry = this.entries.get(instanceId);
      if (entry == null) {
        return null;
      }
      entry.lastAccess = this.clockNanos.getAsLong();
      return entry.events;
    }
  }

  /**
   * Caches an instance's committed history, evicting least-recently-used entries to stay within
   * the configured bounds.
   *
   * @param instanceId the workflow instance ID
   * @param events     the committed history to cache for the instance
   */
  public void put(String instanceId, List<HistoryEvents.HistoryEvent> events) {
    List<HistoryEvents.HistoryEvent> snapshot = new ArrayList<>(events);
    long bytes = 0;
    for (HistoryEvents.HistoryEvent event : snapshot) {
      bytes += event.getSerializedSize();
    }

    synchronized (this.lock) {
      Entry existing = this.entries.get(instanceId);
      if (existing != null) {
        this.totalBytes -= existing.bytes;
      }
      this.entries.put(instanceId, new Entry(snapshot, bytes, this.clockNanos.getAsLong()));
      this.totalBytes += bytes;
      this.evictToFit(instanceId);
    }
  }

  /**
   * Drops an instance's cached history (e.g. once it completes).
   *
   * @param instanceId the workflow instance ID
   */
  public void remove(String instanceId) {
    synchronized (this.lock) {
      this.removeLocked(instanceId);
    }
  }

  /** Clears the cache; used when the stream reconnects (and starts cold). */
  public void reset() {
    synchronized (this.lock) {
      this.entries.clear();
      this.totalBytes = 0;
    }
  }

  /** Evicts entries whose last turn was longer ago than the TTL. */
  public void sweepExpired() {
    long now = this.clockNanos.getAsLong();
    synchronized (this.lock) {
      List<String> expired = new ArrayList<>();
      for (Map.Entry<String, Entry> entry : this.entries.entrySet()) {
        if (now - entry.getValue().lastAccess > this.ttlNanos) {
          expired.add(entry.getKey());
        }
      }
      for (String instanceId : expired) {
        this.removeLocked(instanceId);
      }
    }
  }

  int size() {
    synchronized (this.lock) {
      return this.entries.size();
    }
  }

  long totalBytes() {
    synchronized (this.lock) {
      return this.totalBytes;
    }
  }

  private void removeLocked(String instanceId) {
    Entry removed = this.entries.remove(instanceId);
    if (removed != null) {
      this.totalBytes -= removed.bytes;
    }
  }

  /**
   * Evicts least-recently-used entries until within the count and byte bounds, always keeping the
   * just-touched entry so the active working set is never evicted. A lone entry over the byte
   * budget is kept (a soft overage) rather than thrashing.
   */
  private void evictToFit(String keep) {
    while (this.entries.size() > 1) {
      boolean overCount = this.entries.size() > this.maxInstances;
      boolean overBytes = this.maxBytes > 0 && this.totalBytes > this.maxBytes;
      if (!overCount && !overBytes) {
        return;
      }
      String victim = this.leastRecentlyUsedExcept(keep);
      if (victim == null) {
        return;
      }
      this.removeLocked(victim);
    }
  }

  private String leastRecentlyUsedExcept(String keep) {
    String oldest = null;
    long oldestAccess = Long.MAX_VALUE;
    for (Map.Entry<String, Entry> entry : this.entries.entrySet()) {
      if (entry.getKey().equals(keep)) {
        continue;
      }
      if (oldest == null || entry.getValue().lastAccess < oldestAccess) {
        oldest = entry.getKey();
        oldestAccess = entry.getValue().lastAccess;
      }
    }
    return oldest;
  }
}
