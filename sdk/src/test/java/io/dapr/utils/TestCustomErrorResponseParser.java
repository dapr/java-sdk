package io.dapr.utils;

import io.dapr.client.DaprErrorResponseParser;
import io.dapr.exceptions.DaprException;

public class TestCustomErrorResponseParser implements DaprErrorResponseParser {

    @Override
    public DaprException parse(int statusCode, byte[] response) {
        return new TestCustomError("customStatus", "customMessage", String.valueOf(statusCode));
    }
}
