/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import java.lang.annotation.*;

/**
 * Annotation to define Actor class.
 */
@Documented
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActorType {

    /**
     * Overrides Actor's name.
     *
     * @return Actor's name.
     */
    String name();

}
