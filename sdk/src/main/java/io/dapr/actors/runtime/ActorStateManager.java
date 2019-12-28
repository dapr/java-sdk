/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Manages state changes of a given Actor instance.
 * <p>
 * All changes are cached in-memory until save() is called.
 */
class ActorStateManager {

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
    <T> Mono<Void> add(String stateName, T value) {
        try {
            if (stateName == null) {
                throw new IllegalArgumentException("State's name cannot be null.");
            }

            if (this.stateChangeTracker.containsKey(stateName)) {
                StateChangeMetadata metadata = this.stateChangeTracker.get(stateName);

                if (metadata.kind == ActorStateChangeKind.REMOVE) {
                    this.stateChangeTracker.put(stateName, new StateChangeMetadata(ActorStateChangeKind.UPDATE, value));
                    return Mono.empty();
                }

                throw new IllegalStateException("Duplicate cached state: " + stateName);
            }

            return this.stateProvider.contains(this.actorTypeName, this.actorId, stateName)
                    .flatMap(exists -> {
                        if (exists) {
                            throw new IllegalStateException("Duplicate state: " + stateName);
                        }

                        this.stateChangeTracker.put(stateName, new StateChangeMetadata(ActorStateChangeKind.ADD, value));
                        return Mono.empty();
                    });
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    /**
     * Fetches the most recent value for the given state, including cached value.
     *
     * @param stateName Name of the state.
     * @param clazz     Class type for the value being fetched.
     * @param <T>       Type being fetched.
     * @return Asynchronous response with fetched object.
     */
    <T> Mono<T> get(String stateName, Class<T> clazz) {
        try {
            if (stateName == null) {
                throw new IllegalArgumentException("State's name cannot be null.");
            }

            if (this.stateChangeTracker.containsKey(stateName)) {
                StateChangeMetadata metadata = this.stateChangeTracker.get(stateName);

                if (metadata.kind == ActorStateChangeKind.REMOVE) {
                    throw new NoSuchElementException("State is marked for removal: " + stateName);
                }

                return Mono.just((T) metadata.value);
            }

            return this.stateProvider.load(this.actorTypeName, this.actorId, stateName, clazz)
                    .switchIfEmpty(Mono.error(new NoSuchElementException("State not found: " + stateName)))
                    .map(v -> {
                        this.stateChangeTracker.put(stateName, new StateChangeMetadata(ActorStateChangeKind.REMOVE, v));
                        return (T) v;
                    });
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    /**
     * Updates a given key/value pair in the state store's cache.
     *
     * @param stateName Name of the state being updated.
     * @param value     Value to be set for given state.
     * @param <T>       Type of the value being set.
     * @return Asynchronous void result.
     */
    <T> Mono<Void> set(String stateName, T value) {
        try {
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
                return Mono.empty();
            }

            return this.stateProvider.contains(this.actorTypeName, this.actorId, stateName)
                    .map(exists -> {
                        this.stateChangeTracker.put(stateName,
                                new StateChangeMetadata(exists ? ActorStateChangeKind.UPDATE : ActorStateChangeKind.ADD, value));
                        return exists;
                    })
                    .then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    /**
     * Removes a given state from state store's cache.
     *
     * @param stateName State being stored.
     * @return Asynchronous void result.
     */
    Mono<Void> remove(String stateName) {
        try {
            if (stateName == null) {
                throw new IllegalArgumentException("State's name cannot be null.");
            }

            if (this.stateChangeTracker.containsKey(stateName)) {
                StateChangeMetadata metadata = this.stateChangeTracker.get(stateName);

                if (metadata.kind == ActorStateChangeKind.REMOVE) {
                    return Mono.empty();
                }

                if (metadata.kind == ActorStateChangeKind.ADD) {
                    this.stateChangeTracker.remove(stateName);
                    return Mono.empty();
                }

                this.stateChangeTracker.put(stateName, new StateChangeMetadata(ActorStateChangeKind.REMOVE, null));
                return Mono.empty();
            }

            return this.stateProvider.contains(this.actorTypeName, this.actorId, stateName)
                    .filter(exists -> exists)
                    .map(exists -> {
                        this.stateChangeTracker.put(stateName, new StateChangeMetadata(ActorStateChangeKind.REMOVE, null));
                        return exists;
                    })
                    .then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    /**
     * Checks if a given state exists in state store or cache.
     *
     * @param stateName State being checked.
     * @return Asynchronous boolean result indicating whether state is present.
     */
    Mono<Boolean> contains(String stateName) {
        try {
            if (stateName == null) {
                throw new IllegalArgumentException("State's name cannot be null.");
            }

            if (this.stateChangeTracker.containsKey(stateName)) {
                StateChangeMetadata metadata = this.stateChangeTracker.get(stateName);

                if (metadata.kind == ActorStateChangeKind.REMOVE) {
                    return Mono.just(false);
                }

                return Mono.just(true);
            }

            return this.stateProvider.contains(this.actorTypeName, this.actorId, stateName);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    /**
     * Saves all changes to state store.
     *
     * @return Asynchronous void result.
     */
    Mono<Void> save() {
        if (this.stateChangeTracker.isEmpty()) {
            return Mono.empty();
        }

        List<ActorStateChange> changes = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        for (Map.Entry<String, StateChangeMetadata> tuple : this.stateChangeTracker.entrySet()) {
            if (tuple.getValue().kind == ActorStateChangeKind.NONE) {
                continue;
            }

            if (tuple.getValue().kind == ActorStateChangeKind.REMOVE) {
                removed.add(tuple.getKey());
            }

            changes.add(new ActorStateChange(tuple.getKey(), tuple.getValue().value, tuple.getValue().kind));
        }

        return this.stateProvider.apply(this.actorTypeName, this.actorId, changes.toArray(new ActorStateChange[0]))
                .then(this.flush());
    }

    /**
     * Clears all changes not yet saved to state store.
     *
     * @return
     */
    Mono<Void> clear() {
        this.stateChangeTracker.clear();
        return Mono.empty();
    }

    /**
     * Commits the current cached values after successful save.
     *
     * @return
     */
    private Mono<Void> flush() {
        for (Map.Entry<String, StateChangeMetadata> tuple : this.stateChangeTracker.entrySet()) {
            String stateName = tuple.getKey();
            if (tuple.getValue().kind == ActorStateChangeKind.REMOVE) {
                this.stateChangeTracker.remove(stateName);
            } else {
                StateChangeMetadata metadata = new StateChangeMetadata(ActorStateChangeKind.NONE, tuple.getValue().value);
                this.stateChangeTracker.put(stateName, metadata);
            }
        }

        return Mono.empty();
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
