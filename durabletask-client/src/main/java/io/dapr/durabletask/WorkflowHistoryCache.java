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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    /**
     * Already wrapped unmodifiable, so {@link #get(String)} hands it out without allocating.
     */
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

  /**
   * Maps an instance to the completion token of its most recently received dispatch. Cache writes
   * are gated on it: a runner whose dispatch has been superseded (a redelivery, or a dispatch on a
   * reconnected stream while the old runner still executes) must not commit its prefix over the
   * newer dispatch's. The sidecar already discards the stale runner's response by completion
   * token; without this gate the cache write would still land, and after a continue-as-new the
   * stale prefix can have the same length as the good one, poisoning the next delta
   * reconstruction. Deliberately NOT cleared by {@link #reset()}: it must survive reconnects to
   * fence exactly those still-running handlers. Mirrors the Go reference
   * (durabletask-go client/worker_history.go).
   */
  private final Map<String, String> latestTokens = new ConcurrentHashMap<>();
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
   * <p>The returned list is the entry's unmodifiable view, wrapped once when it was cached, so a
   * caller cannot corrupt the cache by mutating what it was handed and this path allocates nothing.
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
    // Snapshot so a later mutation by the caller is not observed, and wrap once here rather than on
    // every get(), which is the hot path.
    List<HistoryEvents.HistoryEvent> snapshot = Collections.unmodifiableList(new ArrayList<>(events));

    // Only measure the serialized size when a byte budget is configured. On the default
    // (unbounded) path this avoids re-serializing the whole history every turn, which would
    // reintroduce the full-history serialization cost this cache exists to avoid.
    long bytes = 0;
    if (this.maxBytes > 0) {
      for (HistoryEvents.HistoryEvent event : snapshot) {
        bytes += event.getSerializedSize();
      }
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

  /**
   * Records that {@code completionToken} is the newest dispatch received for the instance.
   *
   * @param instanceId      the workflow instance ID
   * @param completionToken the dispatch's completion token
   */
  public void noteDispatch(String instanceId, String completionToken) {
    this.latestTokens.put(instanceId, completionToken);
  }

  /**
   * Reports whether {@code completionToken} still belongs to the newest dispatch received for the
   * instance. An instance with no recorded dispatch accepts any token, so paths that never call
   * {@link #noteDispatch(String, String)} keep their previous behavior.
   *
   * @param instanceId      the workflow instance ID
   * @param completionToken the dispatch's completion token
   * @return whether the dispatch carrying this token is still the newest one
   */
  public boolean isLatestDispatch(String instanceId, String completionToken) {
    String latest = this.latestTokens.get(instanceId);
    return latest == null || latest.equals(completionToken);
  }

  /**
   * Drops the newest-dispatch marker, for instances whose history reached a terminal state.
   *
   * @param instanceId the workflow instance ID
   */
  public void forgetDispatch(String instanceId) {
    this.latestTokens.remove(instanceId);
  }

  /**
   * Clears the cached histories; used when the stream reconnects (and starts cold). The
   * newest-dispatch markers survive on purpose: their whole job is to fence runners from the
   * previous stream that are still executing across the reconnect.
   */
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
