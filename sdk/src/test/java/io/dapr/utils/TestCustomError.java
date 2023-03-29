package io.dapr.utils;

import io.dapr.exceptions.DaprException;

public class TestCustomError extends DaprException {
    private String status;

    private String message;

    private String customField;

    public TestCustomError(String status, String message, String customField) {
        super(status, message);
        this.status = status;
        this.message = message;
        this.customField = customField;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getCustomField() {
        return customField;
    }
}
