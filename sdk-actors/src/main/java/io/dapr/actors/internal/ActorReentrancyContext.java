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

package io.dapr.actors.internal;

import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Map;

/**
 * Internal helpers for carrying an incoming actor reentrancy id through a Reactor invocation.
 */
public final class ActorReentrancyContext {

  /** Metadata key used by Dapr to identify an actor reentrant call chain. */
  public static final String REENTRANCY_ID_HEADER = "Dapr-Reentrancy-Id";

  private static final String CONTEXT_KEY = ActorReentrancyContext.class.getName() + ".id";

  private ActorReentrancyContext() {
  }

  /**
   * Adds the reentrancy id to the subscriber context for the lifetime of this publisher.
   *
   * @param publisher Publisher for the actor callback.
   * @param reentrancyId Id received from Dapr, or {@code null} when this is not a reentrant call.
   * @param <T> Publisher result type.
   * @return The publisher with the invocation-scoped context value.
   */
  public static <T> Mono<T> withReentrancyId(Mono<T> publisher, String reentrancyId) {
    if (reentrancyId == null || reentrancyId.isEmpty()) {
      return publisher;
    }
    return publisher.contextWrite(context -> context.put(CONTEXT_KEY, reentrancyId));
  }

  /**
   * Reads the reentrancy id from a Reactor subscriber context.
   *
   * @param context Subscriber context.
   * @return The id, or {@code null} when no actor invocation is active.
   */
  public static String getReentrancyId(ContextView context) {
    return context.getOrDefault(CONTEXT_KEY, null);
  }

  /**
   * Reads the reentrancy id from callback metadata, ignoring header-name casing.
   *
   * @param metadata Callback metadata.
   * @return The id, or {@code null} when Dapr did not send one.
   */
  public static String getReentrancyId(Map<String, String> metadata) {
    if (metadata == null) {
      return null;
    }
    return metadata.entrySet().stream()
        .filter(entry -> REENTRANCY_ID_HEADER.equalsIgnoreCase(entry.getKey()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }
}
