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

package io.dapr.testcontainers.wait.strategy;

import io.dapr.testcontainers.wait.strategy.metadata.Actor;
import io.dapr.testcontainers.wait.strategy.metadata.Metadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActorWaitStrategyTest {

  @Test
  @DisplayName("Should match any actor when no specific type is specified")
  void shouldMatchAnyActorWhenNoTypeSpecified() {
    ActorWaitStrategy strategy = new ActorWaitStrategy();
    Metadata metadata = createMetadataWithActor("SomeActor");

    assertTrue(strategy.isConditionMet(metadata));
  }

  @Test
  @DisplayName("Should not match when no actors exist and no type is specified")
  void shouldNotMatchWhenNoActorsAndNoTypeSpecified() {
    ActorWaitStrategy strategy = new ActorWaitStrategy();
    Metadata metadata = new Metadata();

    metadata.setActors(Collections.emptyList());

    assertFalse(strategy.isConditionMet(metadata));
  }

  @Test
  @DisplayName("Should match when specific actor type exists")
  void shouldMatchSpecificActorType() {
    ActorWaitStrategy strategy = new ActorWaitStrategy("MyActor");
    Metadata metadata = createMetadataWithActor("MyActor");

    assertTrue(strategy.isConditionMet(metadata));
  }

  @Test
  @DisplayName("Should not match when actor type differs from expected")
  void shouldNotMatchWhenActorTypeDiffers() {
    ActorWaitStrategy strategy = new ActorWaitStrategy("MyActor");
    Metadata metadata = createMetadataWithActor("OtherActor");

    assertFalse(strategy.isConditionMet(metadata));
  }

  @Test
  @DisplayName("Should not match when no actors exist but specific type is expected")
  void shouldNotMatchWhenNoActorsAndTypeSpecified() {
    ActorWaitStrategy strategy = new ActorWaitStrategy("MyActor");
    Metadata metadata = new Metadata();

    metadata.setActors(Collections.emptyList());

    assertFalse(strategy.isConditionMet(metadata));
  }

  @Test
  @DisplayName("Should find matching actor among multiple registered actors")
  void shouldFindMatchAmongMultipleActors() {
    ActorWaitStrategy strategy = new ActorWaitStrategy("TargetActor");

    Actor actor1 = createActor("FirstActor");
    Actor actor2 = createActor("TargetActor");
    Actor actor3 = createActor("ThirdActor");

    Metadata metadata = new Metadata();
    metadata.setActors(Arrays.asList(actor1, actor2, actor3));

    assertTrue(strategy.isConditionMet(metadata));
  }

  @Test
  @DisplayName("Should provide correct human-readable condition description")
  void shouldProvideCorrectDescription() {
    ActorWaitStrategy anyActors = new ActorWaitStrategy();
    assertEquals("any registered actors", anyActors.getConditionDescription());

    ActorWaitStrategy specificActor = new ActorWaitStrategy("MyActor");
    assertEquals("actor type 'MyActor'", specificActor.getConditionDescription());
  }

  private Metadata createMetadataWithActor(String actorType) {
    Metadata metadata = new Metadata();
    metadata.setActors(Collections.singletonList(createActor(actorType)));
    return metadata;
  }

  private Actor createActor(String type) {
    Actor actor = new Actor();
    actor.setType(type);
    actor.setCount(1);
    return actor;
  }
}
