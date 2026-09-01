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

package io.dapr.workflows.internal;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The module is compiled for Java 17, where {@code Thread.isVirtual()} does not exist, so
 * these tests cannot assert that the returned executor produces virtual threads. They assert
 * that a usable executor comes back on every supported runtime, and that the thread kind
 * matches the runtime the tests happen to be executing on.
 */
public class DefaultExecutorServiceTest {

  @Test
  public void createReturnsAUsableExecutor() throws Exception {
    ExecutorService executorService = DefaultExecutorService.create();

    assertNotNull(executorService);
    AtomicBoolean ran = new AtomicBoolean(false);
    executorService.submit(() -> ran.set(true)).get(5, TimeUnit.SECONDS);
    assertTrue(ran.get(), "the default executor must actually run submitted work");

    executorService.shutdown();
  }

  @Test
  public void threadKindMatchesTheRunningJavaVersion() throws Exception {
    ExecutorService executorService = DefaultExecutorService.create();
    try {
      boolean virtual = executorService.submit(DefaultExecutorServiceTest::currentThreadIsVirtual)
          .get(5, TimeUnit.SECONDS);

      assertEquals(Runtime.version().feature() >= 21, virtual,
          "Java 21+ should produce virtual threads; earlier runtimes should produce platform threads");
    } finally {
      executorService.shutdown();
    }
  }

  /**
   * Calls {@code Thread.isVirtual()} reflectively, since it is absent at this module's
   * Java 17 compile target.
   *
   * @return true when the calling thread is a virtual thread.
   */
  private static boolean currentThreadIsVirtual() {
    try {
      return (boolean) Thread.class.getMethod("isVirtual").invoke(Thread.currentThread());
    } catch (ReflectiveOperationException ex) {
      return false;
    }
  }
}
