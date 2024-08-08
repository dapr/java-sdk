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

import io.dapr.actors.ActorId;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages state changes of a given Actor instance.
 * All changes are cached in-memory until save() is called.
 */
public class ActorStateManager {

  /**
   * Provides states using a state store.
   */
  private final DaprStateAsyncProvider stateProvider;

  /**
   * Name of the Actor's type.
   */
  private final String actorTypeName;

  /**
   * Actor's identifier.
   */
  private final ActorId actorId;

  /**
   * Cache of state changes in this Actor's instance.
   */
  private final Map<String, StateChangeMetadata> stateChangeTracker;

  /**
   * Instantiates a new state manager for the given Actor's instance.
   *
   * @param stateProvider State store provider.
   * @param actorTypeName Name of Actor's type.
   * @param actorId       Actor's identifier.
   */
  ActorStateManager(DaprStateAsyncProvider stateProvider, String actorTypeName, ActorId actorId) {
    this.stateProvider = stateProvider;
    this.actorTypeName = actorTypeName;
    this.actorId = actorId;
    this.stateChangeTracker = new ConcurrentHashMap<>();
  }

  /**
   * Adds a given key/value to the Actor's state store's cache.
   *
   * @param stateName  Name of the state being added.
   * @param value      Value to be added.
   * @param expiration State's expiration.
   * @param <T>        Type of the object being added.
   * @return Asynchronous void operation.
   */
  public <T> Mono<Void> add(String stateName, T value, Instant expiration) {
    return Mono.fromSupplier(() -> {
      if (stateName == null) {
        throw new IllegalArgumentException("State's name cannot be null.");
      }

      return null;
    }).then(this.stateProvider.contains(this.actorTypeName, this.actorId, stateName)
        .map(exists -> {
          if (this.stateChangeTracker.containsKey(stateName)) {
            StateChangeMetadata metadata = this.stateChangeTracker.get(stateName);

            if (metadata.kind == ActorStateChangeKind.REMOVE) {
              this.stateChangeTracker.put(
                  stateName, new StateChangeMetadata(ActorStateChangeKind.UPDATE, value, expiration));
              return true;
            }

            throw new IllegalStateException("Duplicate cached state: " + stateName);
          }

          if (exists) {
            throw new IllegalStateException("Duplicate state: " + stateName);
          }

          this.stateChangeTracker.put(
              stateName, new StateChangeMetadata(ActorStateChangeKind.ADD, value, expiration));
          return true;
        }))
        .then();
  }

  /**
   * Fetches the most recent value for the given state, including cached value.
   *
   * @param stateName Name of the state.
   * @param clazz     Class type for the value being fetched.
   * @param <T>       Type being fetched.
   * @return Asynchronous response with fetched object.
   */
  public <T> Mono<T> get(String stateName, Class<T> clazz) {
    return this.get(stateName, TypeRef.get(clazz));
  }

  /**
   * Fetches the most recent value for the given state, including cached value.
   *
   * @param stateName Name of the state.
   * @param type      Class type for the value being fetched.
   * @param <T>       Type being fetched.
   * @return Asynchronous response with fetched object.
   */
  public <T> Mono<T> get(String stateName, TypeRef<T> type) {
    return Mono.fromSupplier(() -> {
      if (stateName == null) {
        throw new IllegalArgumentException("State's name cannot be null.");
      }

      if (this.stateChangeTracker.containsKey(stateName)) {
        StateChangeMetadata metadata = this.stateChangeTracker.get(stateName);

        if (metadata.isExpired()) {
          throw new NoSuchElementException("State is expired: " + stateName);
        }

        if (metadata.kind == ActorStateChangeKind.REMOVE) {
          throw new NoSuchElementException("State is marked for removal: " + stateName);
        }

        return (T) metadata.value;
      }

      return (T) null;
    }).switchIfEmpty(
        this.stateProvider.load(this.actorTypeName, this.actorId, stateName, type)
            .switchIfEmpty(Mono.error(new NoSuchElementException("State not found: " + stateName)))
            .map(v -> {
              this.stateChangeTracker.put(
                  stateName, new StateChangeMetadata(ActorStateChangeKind.NONE, v.getValue(), v.getExpiration()));
              return (T) v.getValue();
            }));
  }

  /**
   * Updates a given key/value pair in the state store's cache.
   * Use the variation that takes in an TTL instead.
   *
   * @param stateName Name of the state being updated.
   * @param value     Value to be set for given state.
   * @param <T>       Type of the value being set.
   * @return Asynchronous void result.
   */
  @Deprecated
  public <T> Mono<Void> set(String stateName, T value) {
    return this.set(stateName, value, Duration.ZERO);
  }

  /**
   * Updates a given key/value pair in the state store's cache.
   * Using TTL is highly recommended to avoid state to be left in the state store forever.
   *
   * @param stateName Name of the state being updated.
   * @param value     Value to be set for given state.
   * @param ttl       Time to live.
   * @param <T>       Type of the value being set.
   * @return Asynchronous void result.
   */
  public <T> Mono<Void> set(String stateName, T value, Duration ttl) {
    return Mono.fromSupplier(() -> {
      if (stateName == null) {
        throw new IllegalArgumentException("State's name cannot be null.");
      }

      if (this.stateChangeTracker.containsKey(stateName)) {
        StateChangeMetadata metadata = this.stateChangeTracker.get(stateName);

        ActorStateChangeKind kind = metadata.kind;
        if (metadata.isExpired() || (kind == ActorStateChangeKind.NONE) || (kind == ActorStateChangeKind.REMOVE)) {
          kind = ActorStateChangeKind.UPDATE;
        }

        var expiration = buildExpiration(ttl);
        this.stateChangeTracker.put(stateName, new StateChangeMetadata(kind, value, expiration));
        return true;
      }

      return false;
    }).filter(x -> x)
        .switchIfEmpty(this.stateProvider.contains(this.actorTypeName, this.actorId, stateName)
            .map(exists -> {
              var expiration = buildExpiration(ttl);
              this.stateChangeTracker.put(stateName,
                  new StateChangeMetadata(
                      exists ? ActorStateChangeKind.UPDATE : ActorStateChangeKind.ADD, value, expiration));
              return exists;
            }))
        .then();
  }

  /**
   * Removes a given state from state store's cache.
   *
   * @param stateName State being stored.
   * @return Asynchronous void result.
   */
  public Mono<Void> remove(String stateName) {
    return Mono.fromSupplier(() -> {
      if (stateName == null) {
        throw new IllegalArgumentException("State's name cannot be null.");
      }

      if (this.stateChangeTracker.containsKey(stateName)) {
        StateChangeMetadata metadata = this.stateChangeTracker.get(stateName);

        if (metadata.kind == ActorStateChangeKind.REMOVE) {
          return true;
        }

        if (metadata.kind == ActorStateChangeKind.ADD) {
          this.stateChangeTracker.remove(stateName);
          return true;
        }

        this.stateChangeTracker.put(stateName, new StateChangeMetadata(ActorStateChangeKind.REMOVE, null, null));
        return true;
      }

      return false;
    })
        .filter(x -> x)
        .switchIfEmpty(this.stateProvider.contains(this.actorTypeName, this.actorId, stateName))
        .filter(exists -> exists)
        .map(exists -> {
          this.stateChangeTracker.put(stateName, new StateChangeMetadata(ActorStateChangeKind.REMOVE, null, null));
          return exists;
        })
        .then();
  }

  /**
   * Checks if a given state exists in state store or cache.
   *
   * @param stateName State being checked.
   * @return Asynchronous boolean result indicating whether state is present.
   */
  public Mono<Boolean> contains(String stateName) {
    return Mono.fromSupplier(() -> {
          if (stateName == null) {
            throw new IllegalArgumentException("State's name cannot be null.");
          }

          return this.stateChangeTracker.get(stateName);
        }
    ).map(metadata -> {
      if (metadata.isExpired() || (metadata.kind == ActorStateChangeKind.REMOVE)) {
        return Boolean.FALSE;
      }

      return Boolean.TRUE;
    }).switchIfEmpty(this.stateProvider.contains(this.actorTypeName, this.actorId, stateName));
  }

  /**
   * Saves all changes to state store.
   *
   * @return Asynchronous void result.
   */
  public Mono<Void> save() {
    return Mono.fromSupplier(() -> {
      if (this.stateChangeTracker.isEmpty()) {
        return null;
      }

      List<ActorStateChange> changes = new ArrayList<>();
      for (Map.Entry<String, StateChangeMetadata> tuple : this.stateChangeTracker.entrySet()) {
        if (tuple.getValue().kind == ActorStateChangeKind.NONE) {
          continue;
        }

        var actorState = new ActorState<>(tuple.getKey(), tuple.getValue().value, tuple.getValue().expiration);
        changes.add(new ActorStateChange(actorState, tuple.getValue().kind));
      }

      return changes.toArray(new ActorStateChange[0]);
    }).flatMap(changes -> this.stateProvider.apply(this.actorTypeName, this.actorId, changes))
        .then(Mono.fromRunnable(() -> this.flush()));
  }

  /**
   * Clears all changes not yet saved to state store.
   */
  public void clear() {
    this.stateChangeTracker.clear();
  }

  /**
   * Commits the current cached values after successful save.
   */
  private void flush() {
    for (Map.Entry<String, StateChangeMetadata> tuple : this.stateChangeTracker.entrySet()) {
      String stateName = tuple.getKey();
      if (tuple.getValue().kind == ActorStateChangeKind.REMOVE) {
        this.stateChangeTracker.remove(stateName);
      } else {
        StateChangeMetadata metadata =
            new StateChangeMetadata(ActorStateChangeKind.NONE, tuple.getValue().value, tuple.getValue().expiration);
        this.stateChangeTracker.put(stateName, metadata);
      }
    }
  }

  private static Instant buildExpiration(Duration ttl) {
    return (ttl != null) && !ttl.isNegative() && !ttl.isZero() ? Instant.now().plus(ttl) : null;
  }

  /**
   * Internal class to represent value and change kind.
   */
  private static final class StateChangeMetadata {

    /**
     * Kind of change cached.
     */
    private final ActorStateChangeKind kind;

    /**
     * Value cached.
     */
    private final Object value;

    /**
     * Expiration.
     */
    private final Instant expiration;

    /**
     * Creates a new instance of the metadata on state change.
     *
     * @param kind  Kind of change.
     * @param value Value to be set.
     * @param expiration When the value is set to expire (recommended but accepts null).
     */
    private StateChangeMetadata(ActorStateChangeKind kind, Object value, Instant expiration) {
      this.kind = kind;
      this.value = value;
      this.expiration = expiration;
    }

    private boolean isExpired() {
      return (this.expiration != null) && Instant.now().isAfter(this.expiration);
    }
  }
}
