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

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trusts the activation id only from a Dapr runtime that strips it from caller headers.
 */
class ActivationIdGate {

  private static final Pattern VERSION = Pattern.compile("^(\\d{1,9})\\.(\\d{1,9})\\.(\\d{1,9})([-+].*)?$");

  private static final int[] MIN_VERSION = {1, 19, 1};

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final Supplier<Mono<String>> runtimeVersion;

  private final AtomicReference<Mono<Boolean>> lookup = new AtomicReference<>();

  ActivationIdGate(Supplier<Mono<String>> runtimeVersion) {
    this.runtimeVersion = runtimeVersion;
  }

  /**
   * Returns the activation id if the Dapr runtime is trusted to set it, or empty otherwise.
   *
   * @param activationId Activation id sent by Dapr, or null if absent.
   * @return The trusted activation id, or empty.
   */
  Mono<Optional<String>> filter(String activationId) {
    if (activationId == null || activationId.isEmpty()) {
      return Mono.just(Optional.empty());
    }
    return Mono.defer(this::trusted)
        .map(t -> t ? Optional.of(activationId) : Optional.<String>empty())
        .onErrorReturn(Optional.empty());
  }

  private Mono<Boolean> trusted() {
    Mono<Boolean> cached = this.lookup.get();
    if (cached != null) {
      return cached;
    }
    AtomicReference<Mono<Boolean>> self = new AtomicReference<>();
    Mono<Boolean> next = Mono.defer(this.runtimeVersion)
        .timeout(TIMEOUT)
        .defaultIfEmpty("")
        .map(ActivationIdGate::supported)
        .doOnError(e -> this.lookup.compareAndSet(self.get(), null))
        .cache();
    self.set(next);
    while (true) {
      Mono<Boolean> current = this.lookup.get();
      if (current != null) {
        return current;
      }
      if (this.lookup.compareAndSet(null, next)) {
        return next;
      }
    }
  }

  static boolean supported(String version) {
    if (version == null) {
      return false;
    }
    Matcher m = VERSION.matcher(version);
    if (!m.matches()) {
      return false;
    }
    for (int i = 0; i < MIN_VERSION.length; i++) {
      int part = Integer.parseInt(m.group(i + 1));
      if (part != MIN_VERSION[i]) {
        return part > MIN_VERSION[i];
      }
    }
    return true;
  }
}
