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

package io.dapr.actors.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActivationIdGateTest {

  @ParameterizedTest
  @ValueSource(strings = {"1.19.0", "1.19.0+build", "1.19.1", "1.19.1-rc.1", "1.19.2", "1.20.0", "2.0.0"})
  public void supportedVersions(String version) {
    assertTrue(ActivationIdGate.supported(version));
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.19.0-rc.2", "1.19.0-rc.7", "1.18.9", "1.18.10", "0.99.99", "edge", "dev", "", "1.19",
      "v1.19.0", "1.19.1.1", "99999999999.0.0"})
  public void unsupportedVersions(String version) {
    assertFalse(ActivationIdGate.supported(version));
  }

  @Test
  public void nullVersionUnsupported() {
    assertFalse(ActivationIdGate.supported(null));
  }

  @Test
  public void emptyIdSkipsLookup() {
    AtomicInteger lookups = new AtomicInteger();
    ActivationIdGate gate = new ActivationIdGate(() -> {
      lookups.incrementAndGet();
      return Mono.just("1.19.1");
    });

    assertEquals(Optional.empty(), gate.filter(null).block());
    assertEquals(Optional.empty(), gate.filter("").block());
    assertEquals(0, lookups.get());
  }

  @Test
  public void oldRuntimeIgnoresIdAndCachesResult() {
    AtomicInteger lookups = new AtomicInteger();
    ActivationIdGate gate = new ActivationIdGate(() -> {
      lookups.incrementAndGet();
      return Mono.just("1.18.9");
    });

    assertEquals(Optional.empty(), gate.filter("a1").block());
    assertEquals(Optional.empty(), gate.filter("a2").block());
    assertEquals(1, lookups.get());
  }

  @Test
  public void failedLookupIgnoresIdThenRetries() {
    AtomicInteger lookups = new AtomicInteger();
    ActivationIdGate gate = new ActivationIdGate(() -> lookups.incrementAndGet() == 1
        ? Mono.error(new RuntimeException("unavailable"))
        : Mono.just("1.19.1"));

    assertEquals(Optional.empty(), gate.filter("a1").block());
    assertEquals(Optional.of("a2"), gate.filter("a2").block());
    assertEquals(Optional.of("a3"), gate.filter("a3").block());
    assertEquals(2, lookups.get());
  }

  @Test
  public void concurrentCallersShareOneLookup() {
    AtomicInteger lookups = new AtomicInteger();
    Sinks.One<String> version = Sinks.one();
    ActivationIdGate gate = new ActivationIdGate(() -> {
      lookups.incrementAndGet();
      return version.asMono();
    });

    List<Optional<String>> results = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      gate.filter("a" + i).subscribe(results::add);
    }
    assertEquals(0, results.size());
    version.tryEmitValue("1.19.1");

    assertEquals(10, results.size());
    for (int i = 0; i < 10; i++) {
      assertEquals(Optional.of("a" + i), results.get(i));
    }
    assertEquals(1, lookups.get());
  }

  @Test
  public void warnsOnceForOldRuntime() {
    List<String> warnings = captureWarnings(() -> {
      ActivationIdGate gate = new ActivationIdGate(() -> Mono.just("1.18.9"));
      gate.filter("a1").block();
      gate.filter("a2").block();
    });

    assertEquals(1, warnings.size());
    assertTrue(warnings.get(0).contains("1.18.9"), warnings.get(0));
  }

  @Test
  public void warnsOncePerRunOfFailedLookups() {
    AtomicInteger lookups = new AtomicInteger();
    List<String> warnings = captureWarnings(() -> {
      ActivationIdGate gate = new ActivationIdGate(() -> lookups.incrementAndGet() == 3
          ? Mono.just("1.19.0")
          : Mono.error(new RuntimeException("unavailable")));
      gate.filter("a1").block();
      gate.filter("a2").block();
      assertEquals(Optional.of("a3"), gate.filter("a3").block());
    });

    assertEquals(1, warnings.size());
    assertTrue(warnings.get(0).contains("unavailable"), warnings.get(0));
  }

  private static List<String> captureWarnings(Runnable body) {
    Logger logger = Logger.getLogger("io.dapr.actors.ActorTrace");
    List<String> warnings = Collections.synchronizedList(new ArrayList<>());
    Handler handler = new Handler() {
      @Override
      public void publish(LogRecord record) {
        if (record.getLevel() == Level.WARNING && record.getMessage().startsWith("ActivationIdGate:")) {
          warnings.add(record.getMessage());
        }
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() {
      }
    };
    logger.addHandler(handler);
    try {
      body.run();
    } finally {
      logger.removeHandler(handler);
    }
    return warnings;
  }
}
