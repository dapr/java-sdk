package io.dapr.client;

import io.dapr.exceptions.DaprException;
import java.io.IOException;

public interface DaprErrorResponseParser {
    DaprException parse(int statusCode, byte[] response) throws IOException;
}
