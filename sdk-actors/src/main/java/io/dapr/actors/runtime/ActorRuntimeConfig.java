/*
 * Copyright 2021 The Dapr Authors
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

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents the configuration for the Actor Runtime.
 */
public class ActorRuntimeConfig {

  private List<String> registeredActorTypes = new CopyOnWriteArrayList<>();

  private volatile Duration actorIdleTimeout;

  private volatile Duration actorScanInterval;

  private volatile Duration drainOngoingCallTimeout;

  private volatile Boolean drainBalancedActors;

  private volatile Integer remindersStoragePartitions;

  /**
   * Instantiates a new config for the Actor Runtime.
   */
  ActorRuntimeConfig() {
  }

  /**
   * Adds a registered actor to the list of registered actors.
   * 
   * @param actorTypeName Actor type that was registered.
   * @return This instance.
   */
  ActorRuntimeConfig addRegisteredActorType(String actorTypeName) {
    if (actorTypeName == null) {
      throw new IllegalArgumentException("Registered actor must have a type name.");
    }

    this.registeredActorTypes.add(actorTypeName);
    return this;
  }

  /**
   * Gets the list of registered actor types.
   *
   * @return List of registered actor types.
   */
  Collection<String> getRegisteredActorTypes() {
    return Collections.unmodifiableCollection(registeredActorTypes);
  }

  /**
   * Gets the duration for Actors' timeout.
   *
   * @return Duration for Actors' timeout.
   */
  public Duration getActorIdleTimeout() {
    return actorIdleTimeout;
  }

  /**
   * Sets the duration for Actors' timeout.
   *
   * @param actorIdleTimeout Duration for Actors' timeout.
   * @return This instance.
   */
  public ActorRuntimeConfig setActorIdleTimeout(Duration actorIdleTimeout) {
    this.actorIdleTimeout = actorIdleTimeout;
    return this;
  }

  /**
   * Gets the duration to scan for Actors.
   *
   * @return The duration to scan for Actors.
   */
  public Duration getActorScanInterval() {
    return actorScanInterval;
  }

  /**
   * Sets the duration to scan for Actors.
   *
   * @param actorScanInterval The duration to scan for Actors.
   * @return This instance.
   */
  public ActorRuntimeConfig setActorScanInterval(Duration actorScanInterval) {
    this.actorScanInterval = actorScanInterval;
    return this;
  }

  /**
   * Gets the timeout to drain ongoing calls.
   *
   * @return The timeout to drain ongoing calls.
   */
  public Duration getDrainOngoingCallTimeout() {
    return drainOngoingCallTimeout;
  }

  /**
   * Sets the timeout to drain ongoing calls.
   *
   * @param drainOngoingCallTimeout The timeout to drain ongoing calls.
   * @return This instance.
   */
  public ActorRuntimeConfig setDrainOngoingCallTimeout(Duration drainOngoingCallTimeout) {
    this.drainOngoingCallTimeout = drainOngoingCallTimeout;
    return this;
  }

  /**
   * Gets whether balanced actors should be drained.
   *
   * @return Whether balanced actors should be drained.
   */
  public Boolean getDrainBalancedActors() {
    return drainBalancedActors;
  }

  /**
   * Sets whether balanced actors should be drained.
   *
   * @param drainBalancedActors Whether balanced actors should be drained.
   * @return This instance.
   */
  public ActorRuntimeConfig setDrainBalancedActors(Boolean drainBalancedActors) {
    this.drainBalancedActors = drainBalancedActors;
    return this;
  }

  /**
   * Gets the number of storage partitions for Actor reminders.
   *
   * @return The number of Actor reminder storage partitions.
   */
  public Integer getRemindersStoragePartitions() {
    return remindersStoragePartitions;
  }

  /**
   * Sets the number of storage partitions for Actor reminders.
   *
   * @param remindersStoragePartitions The number of storage partitions for Actor reminders.
   * @return This instance.
   */
  public ActorRuntimeConfig setRemindersStoragePartitions(Integer remindersStoragePartitions) {
    this.remindersStoragePartitions = remindersStoragePartitions;
    return this;
  }

}
