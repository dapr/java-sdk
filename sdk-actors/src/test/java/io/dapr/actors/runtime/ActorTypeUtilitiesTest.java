/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.utils.TypeRef;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.time.Duration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActorTypeUtilitiesTest {

    @Test
    public void nullIsNotRemindable() {
        assertFalse(ActorTypeUtilities.isRemindableActor(null));
    }

    @Test
    public void nonActorIsNotRemindable() {
        assertFalse(ActorTypeUtilities.isRemindableActor(String.class));
    }

    @Test
    public void actorButNotRemindable() {
        assertFalse(ActorTypeUtilities.isRemindableActor(NonRemindable.class));
    }

    @Test
    public void actorWithInterfacesButNotRemindable() {
        assertFalse(ActorTypeUtilities.isRemindableActor(NonRemindableWithInterfaces.class));
    }

    @Test
    public void actorIsRemindable() {
        assertTrue(ActorTypeUtilities.isRemindableActor(Remindable.class));
    }

    public static class NonRemindable extends AbstractActor {

        protected NonRemindable(ActorRuntimeContext runtimeContext, ActorId id) {
            super(runtimeContext, id);
        }
    }

    public static class NonRemindableWithInterfaces extends AbstractActor implements Closeable {

        protected NonRemindableWithInterfaces(ActorRuntimeContext runtimeContext, ActorId id) {
            super(runtimeContext, id);
        }

        @Override
        public void close() {
        }
    }

    public static class Remindable extends AbstractActor implements io.dapr.actors.runtime.Remindable {

        protected Remindable(ActorRuntimeContext runtimeContext, ActorId id) {
            super(runtimeContext, id);
        }

        @Override
        public TypeRef getStateType() {
            return null;
        }

        @Override
        public Mono<Void> receiveReminder(String reminderName, Object state, Duration dueTime, Duration period) {
            return null;
        }
    }
}
