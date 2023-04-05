package io.dapr.client;

import io.dapr.exceptions.DaprError;
import io.dapr.exceptions.DaprException;
import io.grpc.Status;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class DefaultDaprGrpcErrorResponseParser implements DaprErrorResponseParser {
    @Override
    public DaprException parse(int statusCode, byte[] errorDetails) {
        String errorMessage = new String(errorDetails, StandardCharsets.UTF_8);
        return new DaprException("UNKNOWN: ", errorMessage);
    }
}
