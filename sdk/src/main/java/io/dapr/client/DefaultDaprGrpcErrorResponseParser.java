package io.dapr.client;

import io.dapr.exceptions.DaprException;


import static io.dapr.config.Properties.STRING_CHARSET;

public class DefaultDaprGrpcErrorResponseParser implements DaprErrorResponseParser {
    @Override
    public DaprException parse(int statusCode, byte[] errorDetails) {
        String errorMessage = new String(errorDetails, STRING_CHARSET.get());
        return new DaprException("UNKNOWN: ", errorMessage);
    }
}
