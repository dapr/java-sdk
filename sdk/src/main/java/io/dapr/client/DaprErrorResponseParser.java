package io.dapr.client;

import io.dapr.exceptions.DaprException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface DaprErrorResponseParser {
    DaprException parse(@NotNull okhttp3.Response response) throws IOException;
}
