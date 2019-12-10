/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors;

import java.io.IOException;

/**
 * A Dapr's specific exception.
 */
class DaprException extends IOException {

    /**
     * Dapr's error code for this exception.
     */
    private String errorCode;

    /**
     * New exception from a server-side generated error code and message.
     * @param daprError Server-side error.
     */
    DaprException(DaprError daprError) {
        this(daprError.getErrorCode(), daprError.getMessage());
    }

    /**
     * New Exception from a client-side generated error code and message.
     * @param errorCode Client-side error code.
     * @param message Client-side error message.
     */
    DaprException(String errorCode, String message) {
        super(String.format("%s: %s", errorCode, message));
        this.errorCode = errorCode;
    }

    /**
     * Returns the exception's error code.
     * @return Error code.
     */
    String getErrorCode() {
        return this.errorCode;
    }
}
