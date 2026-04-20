/*
 * Copyright 2025 The Dapr Authors
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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for DurableTaskGrpcWorker shutdown behavior.
 */
public class DurableTaskGrpcWorkerShutdownTest {

  /**
   * Verifies that calling close() on a worker that was started via start()
   * causes the worker thread to terminate promptly (within a bounded time),
   * rather than hanging in the retry loop.
   */
  @Test
  void workerThreadTerminatesPromptlyOnClose() throws Exception {
    // Use an arbitrary port where no sidecar is running — the worker will
    // enter the retry loop (UNAVAILABLE → sleep 5s → retry).
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder()
        .port(19876)
        .build();

    worker.start();

    // Give the worker thread time to enter the retry loop
    Thread.sleep(500);

    Instant before = Instant.now();
    worker.close();

    // Wait for the worker thread to finish — the join is bounded so the
    // test doesn't hang if the fix regresses.
    Thread workerThread = getWorkerThread(worker);
    assertNotNull(workerThread, "Worker thread should be accessible via reflection");
    workerThread.join(Duration.ofSeconds(3).toMillis());
    assertFalse(workerThread.isAlive(),
        "Worker thread should have terminated after close()");

    Duration elapsed = Duration.between(before, Instant.now());
    assertTrue(elapsed.toMillis() < 3000,
        "close() should return promptly, but took " + elapsed.toMillis() + "ms");
  }

  /**
   * Verifies that calling close() on a worker that was started via
   * startAndBlock() on a separate thread terminates that thread promptly.
   */
  @Test
  void startAndBlockExitsOnClose() throws Exception {
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder()
        .port(19877)
        .build();

    Thread blockingThread = new Thread(worker::startAndBlock);
    blockingThread.start();

    // Give the blocking thread time to enter the retry loop
    Thread.sleep(500);

    Instant before = Instant.now();
    worker.close();

    blockingThread.join(Duration.ofSeconds(3).toMillis());
    assertFalse(blockingThread.isAlive(),
        "startAndBlock() thread should have terminated after close()");

    Duration elapsed = Duration.between(before, Instant.now());
    assertTrue(elapsed.toMillis() < 3000,
        "close() should terminate startAndBlock() promptly, but took " + elapsed.toMillis() + "ms");
  }

  /**
   * Verifies that interrupting the thread running startAndBlock() causes it
   * to exit and preserves the interrupt status.
   */
  @Test
  void startAndBlockExitsOnInterrupt() throws Exception {
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder()
        .port(19878)
        .build();

    Thread blockingThread = new Thread(worker::startAndBlock);
    blockingThread.start();

    // Give the blocking thread time to enter the retry loop
    Thread.sleep(500);

    blockingThread.interrupt();
    blockingThread.join(Duration.ofSeconds(3).toMillis());

    assertFalse(blockingThread.isAlive(),
        "startAndBlock() thread should have exited after interrupt");
    assertTrue(blockingThread.isInterrupted(),
        "Interrupt status should be preserved after startAndBlock() exits");

    worker.close();
  }

  private Thread getWorkerThread(DurableTaskGrpcWorker worker) {
    try {
      java.lang.reflect.Field f = DurableTaskGrpcWorker.class.getDeclaredField("workerThread");
      f.setAccessible(true);
      return (Thread) f.get(worker);
    } catch (Exception e) {
      fail("Failed to access workerThread field via reflection: " + e.getMessage());
      return null; // unreachable
    }
  }
}
