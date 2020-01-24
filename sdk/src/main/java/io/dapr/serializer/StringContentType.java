/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.serializer;

import java.lang.annotation.*;

/**
 * Flags a serializer indicating that byte[] contains String for both input and output.
 *
 * This information can be used to at the state store, for example, to save serialized data as plain text.
 */
@Documented
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StringContentType {
}
