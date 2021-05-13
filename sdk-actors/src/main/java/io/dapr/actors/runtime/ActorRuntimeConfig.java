/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.config.ActorReentrancyConfig;

import javax.annotation.Nullable;
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

  private volatile ActorReentrancyConfig actorReentrancyConfig;

  /**
   * Instantiates a new config for the Actor Runtime.
   */
  ActorRuntimeConfig() {
  }

  /**
   * Adds a registered actor to the list of registered actors.
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
   * Gets the {@code ActorReentrancyConfig} which defines all reentrancy options.
   *
   * @return the full {@code ActorReentrancyConfig}
   */
  public ActorReentrancyConfig getActorReentrancyConfig() {
    return actorReentrancyConfig;
  }

  /**
   * Sets the {@code ActorReentrancyConfig}.
   *
   * @param enabled boolean stating if reentrancy is enabled
   * @param maxStackDepth optional integer that sets the max stack depth for reentrancy, can be null
   * @return This instance.
   */
  public ActorRuntimeConfig setActorReentrancyConfig(boolean enabled, @Nullable Integer maxStackDepth) {
    this.actorReentrancyConfig = new ActorReentrancyConfig(enabled, maxStackDepth);
    return this;
  }
}
