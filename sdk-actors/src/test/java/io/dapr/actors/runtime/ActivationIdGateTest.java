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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActivationIdGateTest {

  @ParameterizedTest
  @ValueSource(strings = {"1.19.1", "1.19.1-rc.1", "1.19.2", "1.20.0", "2.0.0", "1.19.1+build"})
  public void supportedVersions(String version) {
    assertTrue(ActivationIdGate.supported(version));
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.19.0", "1.19.0-rc.2", "1.18.9", "0.99.99", "edge", "dev", "", "1.19", "v1.19.1",
      "1.19.1.1", "99999999999.0.0"})
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
      return Mono.just("1.19.0");
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
}
