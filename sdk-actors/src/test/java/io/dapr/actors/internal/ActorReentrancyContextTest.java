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

package io.dapr.actors.internal;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActorReentrancyContextTest {

  @Test
  void synchronousScopesAreVisibleToBlockingCallsAndRestored() {
    String result = ActorReentrancyContext.withReentrancyId("outer", () -> {
      assertEquals("outer", Mono.deferContextual(context ->
          Mono.justOrEmpty(ActorReentrancyContext.getReentrancyId(context))).block());

      String nestedResult = ActorReentrancyContext.withReentrancyId("inner", () ->
          Mono.deferContextual(context -> Mono.justOrEmpty(
              ActorReentrancyContext.getReentrancyId(context))).block());
      assertEquals("inner", nestedResult);

      return Mono.deferContextual(context -> Mono.justOrEmpty(
          ActorReentrancyContext.getReentrancyId(context))).block();
    });

    assertEquals("outer", result);
    assertNull(Mono.deferContextual(context -> Mono.justOrEmpty(
        ActorReentrancyContext.getReentrancyId(context))).block());
  }

  @Test
  void nestedNonReentrantCallDoesNotInheritTheOuterId() {
    String result = ActorReentrancyContext.withReentrancyId("outer", () -> {
      String nestedResult = ActorReentrancyContext.withReentrancyId(null, () ->
          Mono.deferContextual(context -> Mono.justOrEmpty(
              ActorReentrancyContext.getReentrancyId(context))).block());
      assertNull(nestedResult);
      return Mono.deferContextual(context -> Mono.justOrEmpty(
          ActorReentrancyContext.getReentrancyId(context))).block();
    });

    assertEquals("outer", result);
  }

}
