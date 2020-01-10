/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.exceptions;

/**
 * A Dapr's specific exception.
 */
public class DaprException extends RuntimeException {

    /**
     * Dapr's error code for this exception.
     */
    private String errorCode;

    /**
     * New exception from a server-side generated error code and message.
     *
     * @param daprError Server-side error.
     */
    public DaprException(DaprError daprError) {
        this(daprError.getErrorCode(), daprError.getMessage());
    }

    /**
     *
     * @param daprError Client-side error.
     * @param cause the cause (which is saved for later retrieval by the
     *        {@link #getCause()} method).  (A {@code null} value is
     *        permitted, and indicates that the cause is nonexistent or
     *        unknown.)
     */
    public DaprException(DaprError daprError, Throwable cause) {
        this(daprError.getErrorCode(), daprError.getMessage(), cause);
    }

    /**
     * New Exception from a client-side generated error code and message.
     *
     * @param errorCode Client-side error code.
     * @param message   Client-side error message.
     */
    public DaprException(String errorCode, String message) {
        super(String.format("%s: %s", errorCode, message));
        this.errorCode = errorCode;
    }

    /**
     *
     * @param errorCode Client-side error code.
     * @param message   Client-side error message.
     * @param cause the cause (which is saved for later retrieval by the
     *        {@link #getCause()} method).  (A {@code null} value is
     *        permitted, and indicates that the cause is nonexistent or
     *        unknown.)
     */
    public DaprException(String errorCode, String message, Throwable cause) {
        super(String.format("%s: %s", errorCode, message), cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the exception's error code.
     *
     * @return Error code.
     */
    public String getErrorCode() {
        return this.errorCode;
    }
}
