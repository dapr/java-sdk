package io.dapr.utils;

import io.dapr.client.DaprErrorResponseParser;
import io.dapr.exceptions.DaprException;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

public class TestCustomErrorResponseParser implements DaprErrorResponseParser {

    @Override
    public DaprException parse(@NotNull Response response) {
        return new TestCustomError("customStatus", "customMessage", String.valueOf(response.code()));
    }
}
