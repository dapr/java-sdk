/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
    this.stateChangeTracker = new HashMap<>();
  }

  /**
   * Adds a given key/value to the Actor's state store's cache.
   *
   * @param stateName Name of the state being added.
   * @param value     Value to be added.
   * @param <T>       Type of the object being added.
   * @return Asynchronous void operation.
   */
  public <T> Mono<Void> add(String stateName, T value) {
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
              this.stateChangeTracker.put(stateName, new StateChangeMetadata(ActorStateChangeKind.UPDATE, value));
              return true;
            }

            throw new IllegalStateException("Duplicate cached state: " + stateName);
          }

          if (exists) {
            throw new IllegalStateException("Duplicate state: " + stateName);
          }

          this.stateChangeTracker.put(stateName, new StateChangeMetadata(ActorStateChangeKind.ADD, value));
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
              this.stateChangeTracker.put(stateName, new StateChangeMetadata(ActorStateChangeKind.NONE, v));
              return (T) v;
            }));
  }

  /**
   * Updates a given key/value pair in the state store's cache.
   *
   * @param stateName Name of the state being updated.
   * @param value     Value to be set for given state.
   * @param <T>       Type of the value being set.
   * @return Asynchronous void result.
   */
  public <T> Mono<Void> set(String stateName, T value) {
    return Mono.fromSupplier(() -> {
      if (stateName == null) {
        throw new IllegalArgumentException("State's name cannot be null.");
      }

      if (this.stateChangeTracker.containsKey(stateName)) {
        StateChangeMetadata metadata = this.stateChangeTracker.get(stateName);

        ActorStateChangeKind kind = metadata.kind;
        if ((kind == ActorStateChangeKind.NONE) || (kind == ActorStateChangeKind.REMOVE)) {
          kind = ActorStateChangeKind.UPDATE;
        }

        this.stateChangeTracker.put(stateName, new StateChangeMetadata(kind, value));
        return true;
      }

      return false;
    }).filter(x -> x)
        .switchIfEmpty(this.stateProvider.contains(this.actorTypeName, this.actorId, stateName)
            .map(exists -> {
              this.stateChangeTracker.put(stateName,
                  new StateChangeMetadata(exists ? ActorStateChangeKind.UPDATE : ActorStateChangeKind.ADD, value));
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

        this.stateChangeTracker.put(stateName, new StateChangeMetadata(ActorStateChangeKind.REMOVE, null));
        return true;
      }

      return false;
    })
        .filter(x -> x)
        .switchIfEmpty(this.stateProvider.contains(this.actorTypeName, this.actorId, stateName))
        .filter(exists -> exists)
        .map(exists -> {
          this.stateChangeTracker.put(stateName, new StateChangeMetadata(ActorStateChangeKind.REMOVE, null));
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
      if (metadata.kind == ActorStateChangeKind.REMOVE) {
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

        changes.add(new ActorStateChange(tuple.getKey(), tuple.getValue().value, tuple.getValue().kind));
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
        StateChangeMetadata metadata = new StateChangeMetadata(ActorStateChangeKind.NONE, tuple.getValue().value);
        this.stateChangeTracker.put(stateName, metadata);
      }
    }
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
     * Creates a new instance of the metadata on state change.
     *
     * @param kind  Kind of change.
     * @param value Value to be set.
     */
    private StateChangeMetadata(ActorStateChangeKind kind, Object value) {
      this.kind = kind;
      this.value = value;
    }
  }
}
